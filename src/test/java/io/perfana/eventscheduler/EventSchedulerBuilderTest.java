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

import io.perfana.eventscheduler.api.config.EventConfig;
import io.perfana.eventscheduler.api.config.EventSchedulerConfig;
import io.perfana.eventscheduler.api.config.TestConfig;
import io.perfana.eventscheduler.exception.EventSchedulerRuntimeException;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

public class EventSchedulerBuilderTest {

    @Test
    public void createWithAlternativeClass() {
         String alternativeClassCustomEvents = "  @generatorFactoryClass=io.perfana.eventscheduler.generator.EventGeneratorFactoryDefault\n" +
                 "  my-setting=my-value \n";

        EventSchedulerConfig build = EventSchedulerConfig.builder()
            .testConfig(TestConfig.builder().build())
            .build();

        EventSchedulerBuilderInternal eventSchedulerBuilder = new EventSchedulerBuilderInternal()
             .setCustomEvents(alternativeClassCustomEvents)
             .setEventSchedulerContext(build.toContext(EventLoggerStdOut.INSTANCE));

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        EventScheduler eventScheduler =
                eventSchedulerBuilder.build(new URLClassLoader(new URL[]{}, classLoader));

        eventScheduler.checkResults();
        eventScheduler.abortSession();
    }

    /**
     * Example pom input:
     * <pre>
     * {@code
     * <io.perfana.event.wiremock.WiremockEventFactory>
     * 	<wiremockEnabled>true</wiremockEnabled>
     * 	<wiremockFilesDir>src/test/resources/wiremock</wiremockFilesDir>
     * 	<wiremockUrl>https://site.a/</wiremockUrl>
     * </io.perfana.event.wiremock.WiremockEventFactory>
     *
     * <io.perfana.event.wiremock.WiremockEventFactory>
     * 	<wiremockEnabled>true</wiremockEnabled>
     * 	<wiremockFilesDir>src/test/resources/wiremock</wiremockFilesDir>
     * 	<wiremockUrl>https://site.b/</wiremockUrl>
     * </io.perfana.event.wiremock.WiremockEventFactory>
     * }
     * </pre>
     *
     */
    @Test
    public void testFactoriesAndBroadcaster() {

        String factoryClassName = "io.perfana.eventscheduler.event.EventFactoryDefault";

        TestConfig testConfig = TestConfig.builder().build();

        EventConfig eventConfig1 = EventConfig.builder()
            .name("Event1")
            .eventFactory(factoryClassName)
            .enabled(true)
            .build();

        EventConfig eventConfig2 = EventConfig.builder()
            .name("Event2")
            .eventFactory(factoryClassName)
            .enabled(true)
            .build();

        EventSchedulerBuilderInternal builder = new EventSchedulerBuilderInternal();
        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .testConfig(testConfig)
            .eventConfig(eventConfig1)
            .eventConfig(eventConfig2)
            .build();
        builder.setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE));
        EventScheduler eventScheduler = builder.build();

        eventScheduler.startSession();
    }

    @Test(expected = EventSchedulerRuntimeException.class)
    public void testUniqueEventNameCheck() {
        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .testConfig(TestConfig.builder().build())
            .eventConfig(EventConfig.builder().name("one").build())
            .eventConfig(EventConfig.builder().name("one").build())
            .build();

        EventScheduler eventScheduler = new EventSchedulerBuilderInternal()
            .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
                .build();
    }

}