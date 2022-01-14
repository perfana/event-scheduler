/*
 * Copyright (C) 2022 Peter Paul Bakker, Perfana
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
package io.perfana.eventscheduler.api;

import io.perfana.eventscheduler.api.config.EventContext;
import io.perfana.eventscheduler.api.message.EventMessageBus;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adapter class with empty method implementations of the Event interface.
 * Extend this class so you only have to implement the methods that are used.
 *
 * Always provide a proper name for an Event for traceability.
 */
public abstract class EventAdapter<T extends EventContext> implements Event {

    protected final T eventContext;
    protected final EventLogger logger;
    protected final EventMessageBus eventMessageBus;

    public EventAdapter(T context, EventMessageBus messageBus, EventLogger logger) {
        this.eventContext = context;
        this.logger = logger;
        this.eventMessageBus = messageBus;
    }

    @Deprecated
    public EventAdapter(T context, EventLogger logger) {
        this(context, null, logger);
    }

    @Override
    public void beforeTest() {
        logger.debug(String.format("[%s] [%s] beforeTest (not implemented)", eventContext.getName(), this.getClass().getName()));
    }

    @Override
    public void startTest() {
        logger.debug(String.format("[%s] [%s] startTest (not implemented)", eventContext.getName(), this.getClass().getName()));
    }

    @Override
    public void afterTest() {
        logger.debug(String.format("[%s] [%s] afterTest (not implemented)", eventContext.getName(), this.getClass().getName()));
    }

    @Override
    public void keepAlive() {
        logger.debug(String.format("[%s] [%s] keepAlive (not implemented)", eventContext.getName(), this.getClass().getName()));
    }

    @Override
    public void abortTest() {
        logger.debug(String.format("[%s] [%s] abortTest (not implemented)", eventContext.getName(), this.getClass().getName()));
    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent customEvent) {
        logger.debug(String.format("[%s] [%s] [%s] customEvent (not implemented)", eventContext.getName(), this.getClass().getName(), customEvent.getName()));
    }

    @Override
    public final String getName() {
        return eventContext.getName();
    }

    /**
     * Convenience method for the allowed properties or events.
     * @param items the allowed props or events
     * @return unmodifiable and ordered set of items
     */
    public static Set<String> setOf(String... items) {
        // TreeSet is ordered
        return Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(items)));
    }

}
