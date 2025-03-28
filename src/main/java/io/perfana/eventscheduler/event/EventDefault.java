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
package io.perfana.eventscheduler.event;

import io.perfana.eventscheduler.api.CustomEvent;
import io.perfana.eventscheduler.api.EventAdapter;
import io.perfana.eventscheduler.api.EventCheck;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.EventContext;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessageBus;

public class EventDefault extends EventAdapter<EventContext> {

    EventDefault(EventContext context, TestContext testContext, EventMessageBus messageBus, EventLogger logger) {
        super(context, testContext, messageBus, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("Before test: " + testContext.getTestRunId());
    }

    @Override
    public void startTest() {
        logger.info("Start test: " + testContext.getTestRunId());
    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {
        logger.debug("Custom event not implemented: " + scheduleEvent);
    }
}
