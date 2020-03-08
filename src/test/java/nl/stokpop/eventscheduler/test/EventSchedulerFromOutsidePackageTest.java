/*
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.eventscheduler.test;

import nl.stokpop.eventscheduler.EventScheduler;
import nl.stokpop.eventscheduler.EventSchedulerBuilder;
import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test class is in another package to check access package private fields.
 */
public class EventSchedulerFromOutsidePackageTest
{
    @Test
    public void createEventSchedulerAndFireSomeEvents() {

        EventLogger testLogger = EventLoggerStdOut.INSTANCE;

        String eventSchedule =
                "   \n" +
                "    PT1S  |restart   (   restart to reset replicas  )   |{ 'server':'myserver' 'replicas':2, 'tags': [ 'first', 'second' ] }    \n" +
                "PT600S   |scale-down |   { 'replicas':1 }   \n" +
                "PT660S|    heapdump|server=    myserver.example.com;   port=1567  \n" +
                "   PT900S|scale-up|{ 'replicas':2 }\n" +
                "  \n";

        EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
                .setKeepAliveInterval(Duration.ofMinutes(2))
                .build();

        TestContext context = new TestContextBuilder()
                .setWorkload("testType")
                .setEnvironment("testEnv")
                .setTestRunId("testRunId")
                .setCIBuildResultsUrl("http://url")
                .setVersion("release")
                .setRampupTimeInSeconds("10")
                .setConstantLoadTimeInSeconds("300")
                .setAnnotations("annotation")
                .setVariables(Collections.emptyMap())
                .setTags("tag1,tag2")
                .build();

        Properties properties = new Properties();
        properties.put("name", "value");
        // this class really needs to be on the classpath, otherwise: runtime exception, not found on classpath
        properties.put(EventProperties.PROP_FACTORY_CLASSNAME, "nl.stokpop.eventscheduler.event.EventFactoryDefault");

        EventScheduler scheduler = new EventSchedulerBuilder()
                .setEventSchedulerSettings(settings)
                .setTestContext(context)
                .setAssertResultsEnabled(true)
                .addEvent("myEvent1", properties)
                .addEvent("myEvent2", properties)
                .addEvent("myEvent3", properties)
                .setCustomEvents(eventSchedule)
                .setLogger(testLogger)
                .build();

        assertNotNull(scheduler);
        assertEquals(120, settings.getKeepAliveDuration().getSeconds());

        scheduler.startSession();
        scheduler.stopSession();
        // no failure exception expected
        scheduler.checkResults();
    }
}
