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
import io.perfana.eventscheduler.api.config.EventConfig;
import io.perfana.eventscheduler.api.config.EventContext;
import io.perfana.eventscheduler.api.config.TestConfig;
import io.perfana.eventscheduler.exception.EventSchedulerRuntimeException;
import io.perfana.eventscheduler.exception.handler.AbortSchedulerException;
import io.perfana.eventscheduler.exception.handler.KillSwitchException;
import io.perfana.eventscheduler.exception.handler.StopTestRunException;
import io.perfana.eventscheduler.log.CountErrorsEventLogger;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.perfana.eventscheduler.log.EventLoggerStdOut.INSTANCE_DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class EventBroadcasterTest {

    @Test
    public void broadcastAbort() {
        Event myEvent = mock(Event.class);
        CustomEvent scheduleEvent = mock(CustomEvent.class);

        List<Event> events = new ArrayList<>();
        events.add(myEvent);

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, countErrorsEventLogger);

        broadcaster.broadcastBeforeTest();
        broadcaster.broadcastKeepAlive();
        broadcaster.broadcastCustomEvent(scheduleEvent);
        broadcaster.broadcastCheck();
        broadcaster.broadcastAbortTest();

        broadcaster.shutdownAndWaitAllTasksDone(2);
        
        verify(myEvent, times(1)).beforeTest();
        verify(myEvent, times(1)).keepAlive();
        verify(myEvent, times(1)).customEvent(scheduleEvent);
        verify(myEvent, times(1)).check();
        verify(myEvent, times(1)).abortTest();

        assertEquals("zero errors expected in logger", 0, countErrorsEventLogger.errorCount());
    }

    @Test
    public void broadcastCustomEventWithFailureShouldProceed() {

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        // not multi-threading code, but used as a convenience to change an object in the inner classes below
        // beware: expects a certain order for the events to be called, which can be different depending on implementation
        final AtomicInteger counter = new AtomicInteger(0);

        List<Event> events = new ArrayList<>();
        // this should succeed
        events.add(new MyTestEventThatCanFail(counter, 0, 1, countErrorsEventLogger));
        // this will fail: counter is 0
        events.add(new MyTestEventThatCanFail(counter, 10, 11, countErrorsEventLogger));
        // this should succeed
        events.add(new MyTestEventThatCanFail(counter, 1, 2, countErrorsEventLogger));

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, countErrorsEventLogger);

        broadcaster.broadcastCustomEvent(CustomEvent.createFromLine("PT1M|test-event"));

        broadcaster.shutdownAndWaitAllTasksDone(2);

        assertEquals("counter should be set to 2 even though the middle event failed", 2, counter.intValue());
        assertEquals("one errors expected in logger", 1, countErrorsEventLogger.errorCount());
    }

    private static class MyTestEventThatCanFail extends EventAdapter<EventContext> {

        private final static EventContext eventContext = configWithName("MyTestEventThatCanFail");

        private final AtomicInteger counter;
        private final int expectValue;
        private final int newValue;


        MyTestEventThatCanFail(AtomicInteger counter, int expectValue, int newValue, EventLogger eventLogger) {
            super(eventContext, TestConfig.builder().build().toContext(), new EventMessageBusSimple(), eventLogger);
            this.counter = counter;
            this.expectValue= expectValue;
            this.newValue = newValue;
        }

        @Override
        public void customEvent(CustomEvent customEvent) {
            if (!counter.compareAndSet(expectValue, newValue)) throw new RuntimeException("counter was not " + expectValue);
        }
    }

    @Test
    public void broadcastTakesTooLongBehaviourBeforeTest() {
        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        // what happens when events "hijack" the event thread?
        List<Event> events = createTestEvents(countErrorsEventLogger);

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, countErrorsEventLogger);
        long startTime = System.currentTimeMillis();
        // blocks to wait for results, but this should not take longer
        // than the longest wait time of a task
        broadcaster.broadcastBeforeTest();
        long durationMillis = System.currentTimeMillis() - startTime;

        sleep(1000);
        
        assertTrue("should not take more than a 300 millis! actual: " + durationMillis, durationMillis < 300);

        broadcaster.shutdownAndWaitAllTasksDone(2);
        assertEquals("one errors expected in logger", 1, countErrorsEventLogger.errorCount());
    }

    @Test
    public void broadcastTakesTooLongBehaviourCheck() {
        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        // what happens when events "hijack" the event thread?
        List<Event> events = createTestEvents(countErrorsEventLogger);

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, countErrorsEventLogger);

        long startTime = System.currentTimeMillis();
        List<EventCheck> eventChecks = broadcaster.broadcastCheck();
        long durationMillis = System.currentTimeMillis() - startTime;

        assertEquals(4, eventChecks.size());
        assertEquals(1, eventChecks.stream().filter(e -> e.getEventStatus() == EventStatus.FAILURE).count());
        assertEquals(3, eventChecks.stream().filter(e -> e.getEventStatus() == EventStatus.SUCCESS).count());

        assertTrue("should not take more than 600 millis: " + durationMillis, durationMillis < 600);

        broadcaster.shutdownAndWaitAllTasksDone(2);
        assertEquals("five errors expected in logger", 5, countErrorsEventLogger.errorCount());
    }

    @Test(expected = KillSwitchException.class)
    public void broadcastKeepAliveWithKillSwitchExceptionAsync() {
        // what happens when an event throws a KillSwitchException?
        List<Event> events = createKillSwitchTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = KillSwitchException.class)
    public void broadcastKeepAliveWithKillSwitchExceptionDefault() {
        // what happens when an event throws a KillSwitchException?
        List<Event> events = createKillSwitchTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterDefault(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = AbortSchedulerException.class)
    public void broadcastKeepAliveWithAbortSchedulerExceptionAsync() {
        List<Event> events = createKillSwitchAndAbortTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = AbortSchedulerException.class)
    public void broadcastKeepAliveWithAbortSchedulerExceptionDefault() {
        List<Event> events = createKillSwitchAndAbortTestEvents();

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        EventBroadcaster broadcaster = new EventBroadcasterDefault(events, countErrorsEventLogger);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = StopTestRunException.class)
    public void broadcastKeepAliveWithStopTestRunExceptionDefault() {
        List<Event> events = createTwoStopTestRunEvents();

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        EventBroadcaster broadcaster = new EventBroadcasterDefault(events, countErrorsEventLogger);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = EventSchedulerRuntimeException.class)
    public void broadcastKeepAliveWithStopTestRunExceptionWrongNumber() {
        List<Event> events = createWrongStopTestRunEvents();

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        EventBroadcaster broadcaster = new EventBroadcasterDefault(events, countErrorsEventLogger);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    private List<Event> createTestEvents(EventLogger eventLogger) {
        MySleepyEvent sleepyEvent1 = new MySleepyEvent(configWithName("sleepy1"), eventLogger);
        MySleepyEvent sleepyEvent2 = new MySleepyEvent(configWithName("sleepy2"), eventLogger);
        MySleepyEvent sleepyEvent3 = new MySleepyEvent(configWithName("sleepy3"), eventLogger);
        MyErrorEvent errorEvent = new MyErrorEvent(configWithName("error1"), eventLogger);

        List<Event> events = new ArrayList<>();
        events.add(sleepyEvent1);
        events.add(sleepyEvent2);
        events.add(sleepyEvent3);
        events.add(errorEvent);
        return events;
    }

    private static EventContext configWithName(String name) {
        return EventConfig.builder().name(name).build().toContext();
    }

    private List<Event> createKillSwitchTestEvents() {
        MyKillSwitchEvent killSwitchEvent1 = new MyKillSwitchEvent(configWithName("no-killer"));
        MyKillSwitchEvent killSwitchEvent2 = new MyKillSwitchEvent(configWithName("killer-one"));

        List<Event> events = new ArrayList<>();
        events.add(killSwitchEvent1);
        events.add(killSwitchEvent2);
        return events;
    }

    private List<Event> createKillSwitchAndAbortTestEvents() {
        MyKillSwitchEvent killSwitchEvent1 = new MyKillSwitchEvent(configWithName("no-killer"));
        MyKillSwitchEvent killSwitchEvent2 = new MyKillSwitchEvent(configWithName("killer-one"));
        MyKillSwitchEvent killSwitchEvent3 = new MyKillSwitchEvent(configWithName("abort-one"));

        List<Event> events = new ArrayList<>();
        events.add(killSwitchEvent1);
        events.add(killSwitchEvent2);
        events.add(killSwitchEvent3);
        return events;
    }

    private List<Event> createTwoStopTestRunEvents() {
        MyStopTestRunEvent stopTestRunEvent1 = new MyStopTestRunEvent(EventConfig.builder().name("stop-one").continueOnKeepAliveParticipant(true).build().toContext());
        MyStopTestRunEvent stopTestRunEvent2 = new MyStopTestRunEvent(EventConfig.builder().name("stop-two").continueOnKeepAliveParticipant(true).build().toContext());
        MyStopTestRunEvent nonStopTestRunEvent = new MyStopTestRunEvent(EventConfig.builder().name("non-stop").continueOnKeepAliveParticipant(false).build().toContext());

        List<Event> events = new ArrayList<>();
        events.add(stopTestRunEvent1);
        events.add(stopTestRunEvent2);
        events.add(nonStopTestRunEvent);
        return events;
    }

    private List<Event> createWrongStopTestRunEvents() {
        // two will throw a StopTestRunExceptions, while only two ContinueOnKeepAliveParticipants are registered
        MyStopTestRunEvent stopTestRunEvent1 = new MyStopTestRunEvent(EventConfig.builder().name("stop-one").continueOnKeepAliveParticipant(true).build().toContext());
        MyStopTestRunEvent stopTestRunEvent2 = new MyStopTestRunEvent(EventConfig.builder().name("stop-two").continueOnKeepAliveParticipant(false).build().toContext());
        MyStopTestRunEvent nonStopTestRunEvent = new MyStopTestRunEvent(EventConfig.builder().name("non-stop").continueOnKeepAliveParticipant(false).build().toContext());

        List<Event> events = new ArrayList<>();
        events.add(stopTestRunEvent1);
        events.add(stopTestRunEvent2);
        events.add(nonStopTestRunEvent);
        return events;
    }

    private static class MySleepyEvent extends EventAdapter<EventContext> {

        public MySleepyEvent(EventContext context, EventLogger eventLogger) {
            super(context, TestConfig.builder().build().toContext(), new EventMessageBusSimple(), eventLogger);
        }

        @Override
        public void beforeTest() {
            logger.info(System.currentTimeMillis() + " Sleep in before test in thread: " + Thread.currentThread().getName());
            sleep(200);
            logger.info(System.currentTimeMillis() + " After sleep in before test in thread: " + Thread.currentThread().getName());
        }

        @Override
        public EventCheck check() {
            logger.info(System.currentTimeMillis() + " Sleep in check in thread: " + Thread.currentThread().getName());
            sleep(500);
            logger.error(System.currentTimeMillis() + " After sleep in check in thread: " + Thread.currentThread().getName());
             return new EventCheck(eventContext.getName(), getClass().getSimpleName(), EventStatus.SUCCESS, "All ok");
        }

    }

    private static class MyKillSwitchEvent extends EventAdapter<EventContext> {

        public MyKillSwitchEvent(EventContext eventContext) {
            super(eventContext, TestConfig.builder().build().toContext(), new EventMessageBusSimple(), INSTANCE_DEBUG);
        }

        @Override
        public void keepAlive() {
            String eventName = eventContext.getName();
            logger.info("keep alive called for " + eventName);
            if (eventName.startsWith("killer")) {
                throw new KillSwitchException("kill switch requested from " + eventName);
            }
            if (eventName.startsWith("abort")) {
                throw new AbortSchedulerException("abort scheduler requested from " + eventName);
            }
        }
    }

    private static class MyStopTestRunEvent extends EventAdapter<EventContext> {

        public MyStopTestRunEvent(EventContext eventContext) {
            super(eventContext, TestConfig.builder().build().toContext(), new EventMessageBusSimple(), INSTANCE_DEBUG);
        }

        @Override
        public void keepAlive() {
            String eventName = eventContext.getName();
            logger.info("keep alive called for " + eventName);
            if (eventName.startsWith("stop")) {
                throw new StopTestRunException("stop test run requested from " + eventName);
            }
        }
    }

    private static class MyErrorEvent extends EventAdapter<EventContext> {

        public MyErrorEvent(EventContext context, EventLogger eventLogger) {
            super(context, TestConfig.builder().build().toContext(), new EventMessageBusSimple(), eventLogger);
        }

        @Override
        public void beforeTest() {
            logger.info(System.currentTimeMillis() + " Sleep in before test error in thread: " + Thread.currentThread().getName());
            sleep(200);
            logger.info(System.currentTimeMillis() + " After sleep in test error in thread: " + Thread.currentThread().getName());
            throw new RuntimeException("oops, something went wrong in before test!");
        }

        @Override
        public EventCheck check() {
            logger.info(System.currentTimeMillis() + " Sleep in error check in thread: " + Thread.currentThread().getName());
            sleep(500);
            logger.error(System.currentTimeMillis() + " After sleep in error check in thread: " + Thread.currentThread().getName());
            throw new RuntimeException("oops, something went wrong in check!");
        }
    }

    private static void sleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            System.out.println("interrupt received: " + Thread.currentThread().getName());
            Thread.currentThread().interrupt();        }
    }

}