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

import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.EventSchedulerConfig;
import io.perfana.eventscheduler.api.config.EventSchedulerContext;
import net.jcip.annotations.NotThreadSafe;

/**
 * Builder: intended to be used in one thread for construction and then to be discarded.
 */
@NotThreadSafe
public class EventSchedulerBuilder {

    private EventSchedulerBuilder() {}

    public static EventScheduler of(EventSchedulerConfig eventSchedulerConfig, EventLogger logger) {
        return of(eventSchedulerConfig, logger, null);
    }

    /**
     * Create an EventScheduler from an EventSchedulerConfig.
     * @param eventSchedulerConfig note that this eventSchedulerConfig will be modified in this method (beh: better make immutable)
     * @param logger the EventLogger for log lines from the EventScheduler and its construction
     * @param classLoader needed in cased where the dynamic class creation does not work in default classloader, can be null
     * @return a fully constructed EventScheduler
     */
    public static EventScheduler of(EventSchedulerConfig eventSchedulerConfig, EventLogger logger, ClassLoader classLoader) {

        final EventSchedulerContext schedulerContext = eventSchedulerConfig.toContext(logger);

        EventSchedulerBuilderInternal eventSchedulerBuilder = new EventSchedulerBuilderInternal()
            .setEventSchedulerContext(schedulerContext)
            .setCustomEvents(eventSchedulerConfig.getScheduleScript())
            .setLogger(logger);

        return eventSchedulerBuilder.build(classLoader);
    }

}