/*
 * Copyright (C) 2024 Peter Paul Bakker, Perfana
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
import io.perfana.eventscheduler.exception.handler.SchedulerHandlerException;
import io.perfana.eventscheduler.log.EventLoggerDevNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EventBroadcasterDefault implements EventBroadcaster {

    private final List<Event> events;
    private final EventLogger logger;
    private final int continueTestRunParticipantsCount;

    EventBroadcasterDefault(Collection<Event> events, EventLogger logger) {
        this.events = events == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(events));
        this.continueTestRunParticipantsCount = (int) this.events.stream().filter(Event::isContinueOnKeepAliveParticipant).count();
        this.logger = logger == null ? EventLoggerDevNull.INSTANCE : logger;
    }

    @Override
    public void broadcastBeforeTest() {
        logger.info("broadcast before test event");
        events.forEach(catchExceptionWrapper(Event::beforeTest));
    }

    @Override
    public void broadcastStartTest() {
        logger.info("broadcast start test event");
        events.forEach(catchExceptionWrapper(Event::startTest));
    }

    @Override
    public void broadcastAfterTest() {
        logger.info("broadcast after test event");
        events.forEach(catchExceptionWrapper(Event::afterTest));
    }

    @Override
    public void broadcastKeepAlive() throws SchedulerHandlerException {
        logger.debug("broadcast keep alive event");
        Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();
        events.forEach(catchExceptionWrapper(Event::keepAlive, exceptions));
        logger.debug("Keep Alive found exceptions: " + exceptions);
        throwAbortOrKillWitchOrStopTestRunException(exceptions, continueTestRunParticipantsCount, logger);
    }

    @Override
    public void broadcastAbortTest() {
        logger.debug("broadcast abort test event");
        events.forEach(catchExceptionWrapper(Event::abortTest));
    }

    @Override
    public void broadcastCustomEvent(CustomEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        events.forEach(catchExceptionWrapper(event -> event.customEvent(scheduleEvent)));
    }

    @Override
    public List<EventCheck> broadcastCheck() {
        logger.info("broadcast check test");
        return events.stream().map(Event::check).collect(Collectors.toList());
    }

    @Override
    public void shutdownAndWaitAllTasksDone(long timeoutSeconds) {
        logger.debug("shutdown broadcaster called, is noop in this implementation.");
    }

    /**
     * Make sure events continue, even when exceptions are thrown, except when kill switch or abort is requested.
     */
    private Consumer<Event> catchExceptionWrapper(Consumer<Event> consumer) throws SchedulerHandlerException {
        return catchExceptionWrapper(consumer, null);
    }

    /**
     * Make sure events continue, even when exceptions are thrown.
     * All exceptions are added to the queue.
     */
    private Consumer<Event> catchExceptionWrapper(Consumer<Event> consumer, Queue<Throwable> errors) {
        return event -> {
            try {
                consumer.accept(event);
            } catch (SchedulerHandlerException e) {
                if (errors != null) {
                    errors.add(e);
                }
            } catch (Exception e) {
                String message = "exception in event (" + event.getName() + ")";
                if (logger != null) {
                    logger.error(message, e);
                }
                else {
                    System.err.printf("exception found (note: better provide a logger): %s %s", message, e.getMessage());
                }
                if (errors != null) {
                    errors.add(e);
                }
            }
        };
    }

}
