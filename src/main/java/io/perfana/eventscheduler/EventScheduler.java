/*
 * Copyright (C) 2025 Peter Paul Bakker, Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.eventscheduler;

import io.perfana.eventscheduler.api.*;
import io.perfana.eventscheduler.api.config.EventContext;
import io.perfana.eventscheduler.api.config.EventSchedulerContext;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessage;
import io.perfana.eventscheduler.api.message.EventMessageBus;
import io.perfana.eventscheduler.exception.EventCheckFailureException;
import io.perfana.eventscheduler.util.TestRunConfigUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class EventScheduler {

    private final EventLogger logger;

    private final String name;

    private final boolean checkResultsEnabled;

    private final EventBroadcaster broadcaster;

    private final EventMessageBus messageBus;

    private final Collection<CustomEvent> scheduleEvents;

    private final EventSchedulerContext eventSchedulerContext;

    private final AtomicReference<SchedulerExceptionHandler> schedulerExceptionHandler = new AtomicReference<>();

    private final EventSchedulerEngine eventSchedulerEngine;

    private final AtomicBoolean isSessionActive = new AtomicBoolean(false);

    private final AtomicInteger goMessageCount = new AtomicInteger(0);

    private final StartTestFunction startTestFunction;

    private final int waitForGoMessagesCount;

    EventScheduler(EventBroadcaster broadcaster,
                   Collection<CustomEvent> scheduleEvents,
                   EventSchedulerContext eventSchedulerContext,
                   EventMessageBus messageBus,
                   EventLogger logger,
                   EventSchedulerEngine eventSchedulerEngine,
                   SchedulerExceptionHandler schedulerExceptionHandler) {
        this.name = eventSchedulerContext.getTestContext().getTestRunId();
        this.broadcaster = broadcaster;
        this.eventSchedulerContext = eventSchedulerContext;
        this.checkResultsEnabled = eventSchedulerContext.isSchedulerEnabled();
        this.scheduleEvents = scheduleEvents;
        this.logger = logger;
        this.eventSchedulerEngine = eventSchedulerEngine;
        this.schedulerExceptionHandler.set(schedulerExceptionHandler);
        this.messageBus = messageBus;

        this.waitForGoMessagesCount = (int) eventSchedulerContext.getEventContexts().stream()
            .filter(EventContext::isReadyForStartParticipant)
            .peek(e -> logger.info("Found 'ReadyForStart' participant: " + e.getName()))
            .count();

        eventSchedulerContext.getEventContexts().stream()
                .filter(EventContext::isContinueOnKeepAliveParticipant)
                .forEach(e -> logger.info("Found 'ContinueOnKeepAlive' participant: " + e.getName()));

        this.startTestFunction = createStartTestFunction();

        // add startTest to this receiver... if needed...
        if (waitForGoMessagesCount != 0) {
            logger.info("Wait for Go! messages is active, need " + waitForGoMessagesCount + " Go! messages to start!");
            this.messageBus.addReceiver(m -> checkMessageForGo(m, startTestFunction, waitForGoMessagesCount));
        }
    }

    private StartTestFunction createStartTestFunction() {
        return () -> {
            broadcaster.broadcastStartTest();
            // Note that schedulerExceptionHandler field can be set later, so it's value can change over time!
            // The schedulerExceptionHandler can be null in constructor.
            // Can result in: "SchedulerHandlerException KILL was thrown, but no SchedulerExceptionHandler is present."
            eventSchedulerEngine.startKeepAliveThread(name, eventSchedulerContext.getKeepAliveInterval(), broadcaster, schedulerExceptionHandler.get());
            eventSchedulerEngine.startCustomEventScheduler(scheduleEvents, broadcaster);
        };
    }

    private void checkMessageForGo(EventMessage m, StartTestFunction startTestFunction, int totalGoMessages) {
        if ("go!".equalsIgnoreCase(m.getMessage())) {
            int count = goMessageCount.incrementAndGet();
            logger.info("Got 'Go! message' from " + m.getPluginName() + " now counted " + count + " 'Go! messages' of " + totalGoMessages + " needed.");
            if (count == totalGoMessages) {
                // Go!
                startTestFunction.start();
            }
        }
    }

    public void addKillSwitch(SchedulerExceptionHandler schedulerExceptionHandler) {
        this.schedulerExceptionHandler.set(schedulerExceptionHandler);
    }

    /**
     * Start a test session.
     */
    public void startSession() {
        boolean wasInActive = isSessionActive.compareAndSet(false, true);
        if (!wasInActive) {
            logger.warn("unexpected call to start session, session was active already, ignore call!");
        }
        else {

            broadcaster.broadcastBeforeTest();

            sendTestConfig();

            if (waitForGoMessagesCount == 0) {
                logger.info("start test session");
                startTestFunction.start();
            }
            // otherwise, wait for the Go! messages callbacks
        }
    }

    /**
     * Stop a test session.
     */
    public void stopSession() {
        boolean wasActive = isSessionActive.compareAndSet(true, false);

        if (!wasActive) {
            logger.warn("unexpected call to stop session, session was inactive already, ignoring call: please debug");
        }
        else {
            logger.info("stop test session.");

            eventSchedulerEngine.shutdownThreadsNow();

            broadcaster.broadcastAfterTest();

            logger.info("all broadcasts for stop test session are done");
        }
    }

    /**
     * @return true when stop or abort has been called.
     */
    public boolean isSessionStopped() {
        return !isSessionActive.get();
    }

    /**
     * Call to abort this test run.
     */
    public void abortSession() {
        boolean wasActive = isSessionActive.compareAndSet(true, false);

        if (!wasActive) {
            logger.warn("unexpected call to abort session, session was inactive already, ignoring call: please debug");
        }
        else {
            logger.info("test session abort called");

            eventSchedulerEngine.shutdownThreadsNow();

            broadcaster.broadcastAbortTest();
        }
    }

    /**
     * Call to check results of this test run. Catch the exception to do something useful.
     * @throws EventCheckFailureException when there are events that report failures
     */
    public void checkResults() throws EventCheckFailureException {
        logger.info("check results called");

        List<EventCheck> eventChecks = broadcaster.broadcastCheck();

        logger.debug("event checks: " + eventChecks);

        boolean success = eventChecks.stream().allMatch(e -> e.getEventStatus() != EventStatus.FAILURE);

        logger.debug("checked " + eventChecks.size() + " event checks, all success: " + success);

        if (!success) {
            String failureMessage = eventChecks.stream()
                    .filter(e -> e.getEventStatus() == EventStatus.FAILURE)
                    .map(e -> String.format("class: '%s' eventId: '%s' message: '%s'", e.getEventClassName(), e.getEventId(), e.getMessage()))
                    .collect(Collectors.joining(", "));
            String message = String.format("event checks with failures found: [%s]", failureMessage);
            if (checkResultsEnabled) {
                logger.info("one or more event checks reported a failure: " + message);
                throw new EventCheckFailureException(message);
            }
            else {
                logger.warn("checkResultsEnabled is false, not throwing EventCheckFailureException with message: " + message);
            }
        }
    }

    @Override
    public String toString() {
        return "EventScheduler [testRunId:" + name + "]";
    }

    public EventSchedulerContext getEventSchedulerContext() {
        return eventSchedulerContext;
    }

    public void sendMessage(EventMessage message) {
        messageBus.send(message);
    }

    private interface StartTestFunction {
        void start();
    }

    private void sendTestConfig() {
        Map<String, String> testConfigKeyValues = createTestConfigKeyValues(getEventSchedulerContext().getTestContext());

        // use newline to make sure the info "breaks" nicely on UI display
        String events = getEventSchedulerContext().getEventContexts().stream()
                .map(EventContext::getName)
                .sorted()
                .collect(Collectors.joining("\n"));

        testConfigKeyValues.put("testEvents", events);
        testConfigKeyValues.put("scheduleScript", getEventSchedulerContext().getScheduleScript());

        EventMessage message = TestRunConfigUtil.createTestRunConfigMessageKeys(
                "event-scheduler",
                testConfigKeyValues,
                "event-scheduler");

        sendMessage(message);
    }

    private Map<String, String> createTestConfigKeyValues(TestContext testContext) {
        Map<String, String> lines = new HashMap<>();
        String prefix = "testContext.";
        lines.put(prefix + "testRunId", testContext.getTestRunId());
        lines.put(prefix + "testEnvironment", testContext.getTestEnvironment());
        lines.put(prefix + "annotations", testContext.getAnnotations());
        lines.put(prefix + "rampupTime", String.valueOf(testContext.getRampupTime()));
        lines.put(prefix + "constantLoadTime", String.valueOf(testContext.getConstantLoadTime()));
        lines.put(prefix + "workload", testContext.getWorkload());
        lines.put(prefix + "productName", testContext.getProductName());
        lines.put(prefix + "version", testContext.getVersion());
        lines.put(prefix + "dashboardName", testContext.getDashboardName());
        lines.put(prefix + "buildResultsUrl", testContext.getBuildResultsUrl());
        lines.put(prefix + "tags", String.join("\n", testContext.getTags()));
        return lines;
    }

}
