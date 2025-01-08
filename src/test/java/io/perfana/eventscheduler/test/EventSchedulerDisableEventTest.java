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
package io.perfana.eventscheduler.test;

import io.perfana.eventscheduler.EventScheduler;
import io.perfana.eventscheduler.EventSchedulerBuilder;
import io.perfana.eventscheduler.api.config.EventConfig;
import io.perfana.eventscheduler.api.config.EventSchedulerConfig;
import io.perfana.eventscheduler.api.config.TestConfig;
import io.perfana.eventscheduler.log.CountErrorsEventLogger;
import io.perfana.eventscheduler.log.CountLogMatchesEventLogger;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test class is in another package to check access package private fields.
 */
public class EventSchedulerDisableEventTest
{
    @Test
    public void disabledEventShouldDoNothing() {

        Function<String, Boolean> matchFunction = s -> s.contains("[myEvent2]");

        CountLogMatchesEventLogger matchLogger =
                CountLogMatchesEventLogger.of(EventLoggerStdOut.INSTANCE, matchFunction);

        TestConfig testConfig = TestConfig.builder()
            .workload("testType")
            .testEnvironment("testEnv")
            .testRunId("testRunId")
            .buildResultsUrl("http://url")
            .version("version")
            .rampupTimeInSeconds(10)
            .constantLoadTimeInSeconds(300)
            .annotations("annotation")
            .tags(Arrays.asList("tag1","tag2"))
            .build();

        // this class really needs to be on the classpath, otherwise: runtime exception, not found on classpath
        String factoryClassName = "io.perfana.eventscheduler.event.EventFactoryDefault";

        List<EventConfig> eventConfigs = new ArrayList<>();
        eventConfigs.add(EventConfig.builder().name("myEvent1").eventFactory(factoryClassName).build());
        eventConfigs.add(EventConfig.builder().name("myEvent2").eventFactory(factoryClassName).enabled(false).build());
        eventConfigs.add(EventConfig.builder().name("myEvent3").eventFactory(factoryClassName).build());

        EventSchedulerConfig eventSchedulerConfig = EventSchedulerConfig.builder()
            .schedulerEnabled(true)
            .debugEnabled(false)
            .continueOnEventCheckFailure(false)
            .failOnError(true)
            .keepAliveIntervalInSeconds(120)
            .testConfig(testConfig)
            .eventConfigs(eventConfigs)
            .build();

        EventScheduler scheduler = EventSchedulerBuilder.of(eventSchedulerConfig, matchLogger);
        assertNotNull(scheduler);
        scheduler.startSession();
        scheduler.stopSession();

        // no failure exception expected
        assertEquals("zero matches expected in logger", 0, matchLogger.matchCount());
        scheduler.checkResults();
    }

}
