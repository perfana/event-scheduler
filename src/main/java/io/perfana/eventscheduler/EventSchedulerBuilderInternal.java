/*
 * Copyright (C) 2023 Peter Paul Bakker, Perfana
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
import io.perfana.eventscheduler.api.message.EventMessageBus;
import io.perfana.eventscheduler.event.EventFactoryProvider;
import io.perfana.eventscheduler.event.TestContextInitializerFactoryProvider;
import io.perfana.eventscheduler.exception.EventSchedulerRuntimeException;
import io.perfana.eventscheduler.generator.EventGeneratorDefault;
import io.perfana.eventscheduler.generator.EventGeneratorFactoryDefault;
import io.perfana.eventscheduler.generator.EventGeneratorFactoryProvider;
import io.perfana.eventscheduler.log.EventLoggerDevNull;
import io.perfana.eventscheduler.log.EventLoggerWithName;
import net.jcip.annotations.NotThreadSafe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Builder: intended to be used in one thread for construction and then to be discarded.
 */
@NotThreadSafe
class EventSchedulerBuilderInternal {

    private final AtomicReference<EventSchedulerContext> eventSchedulerContext = new AtomicReference<>();

    private final Map<String, EventContext> eventContexts = new ConcurrentHashMap<>();

    private String customEventsText = "";

    private EventLogger logger = EventLoggerDevNull.INSTANCE;

    private EventFactoryProvider eventFactoryProvider;

    private TestContextInitializerFactoryProvider testContextInitializerFactoryProvider;

    private EventBroadcasterFactory eventBroadcasterFactory;

    private SchedulerExceptionHandler schedulerExceptionHandler;

    private EventSchedulerEngine eventSchedulerEngine;

    private EventMessageBus eventMessageBus;

    public EventSchedulerBuilderInternal setEventSchedulerEngine(EventSchedulerEngine executorEngine) {
        this.eventSchedulerEngine = executorEngine;
        return this;
    }

    public EventSchedulerBuilderInternal setEventMessageBus(EventMessageBus eventMessageBus) {
        this.eventMessageBus = eventMessageBus;
        return this;
    }

    public EventSchedulerBuilderInternal setSchedulerExceptionHandler(SchedulerExceptionHandler callback) {
        this.schedulerExceptionHandler = callback;
        return this;
    }

    public EventSchedulerBuilderInternal setLogger(EventLogger logger) {
        this.logger = logger;
        return this;
    }

    public EventSchedulerBuilderInternal setEventSchedulerContext(EventSchedulerContext context) {
        this.eventSchedulerContext.set(context);
        return this;
    }

    public EventSchedulerBuilderInternal setTestContextInitializerFactoryProvider(TestContextInitializerFactoryProvider testContextInitializerFactoryProvider) {
        this.testContextInitializerFactoryProvider = testContextInitializerFactoryProvider;
        return this;
    }

    public EventScheduler build() {
        return build(null);
    }

    /**
     * Clients can use this build method to define a different classloader.
     * <br/>
     * By default the classloader from the current thread is used to load event providers and related resources.
     * <br/>
     * For example in a Gradle plugin the thread classpath is limited to plugin classes,
     * and does not contain classes from the project context, such as the custom event providers used in the project.
     *
     * @param classLoader the class loader, if null the default classloader of Java's ServiceLoader will be used
     * @return a new EventScheduler
     */
    public EventScheduler build(ClassLoader classLoader) {

        if (eventSchedulerContext.get() == null) {
            throw new EventSchedulerRuntimeException("eventSchedulerContext must be set, it is null.");
        }

        initializeTestContext(classLoader);

        EventMessageBus messageBus = (this.eventMessageBus == null)
            ? new EventMessageBusSimple()
            : this.eventMessageBus;

        List<EventContext> allEventContexts = eventSchedulerContext.get().getEventContexts();

        // only add the enabled events
        allEventContexts.stream()
                .filter(EventContext::isEnabled)
                .forEach(this::addEvent);

        // report disabled events
        allEventContexts.stream()
                .filter(eventConfig -> !eventConfig.isEnabled())
                .forEach(eventConfig -> logger.info("Event disabled: " + eventConfig.getName()));

        List<CustomEvent> customEvents =
                generateCustomEventSchedule(customEventsText, logger, classLoader);

        // check if provider is already injected (for testing)
        final EventFactoryProvider myEventFactoryProvider = (this.eventFactoryProvider == null)
                ? EventFactoryProvider.createInstanceFromClasspath(classLoader)
                : this.eventFactoryProvider;


        List<Event> events = this.eventContexts.values().stream()
                .map(context -> createEvent(myEventFactoryProvider, context, eventSchedulerContext.get().getTestContext(), messageBus))
                .collect(Collectors.toList());

        EventBroadcasterFactory broadcasterFactory = (eventBroadcasterFactory == null)
                ? EventBroadcasterAsync::new
                : eventBroadcasterFactory;

        EventBroadcaster broadcaster = broadcasterFactory.create(events, logger);

        eventSchedulerEngine = (eventSchedulerEngine == null)
            ? new EventSchedulerEngine(logger)
            : eventSchedulerEngine;

        return new EventScheduler(
                broadcaster,
                customEvents,
                eventSchedulerContext.get(),
                messageBus,
                logger,
                eventSchedulerEngine,
                schedulerExceptionHandler);
    }

    private void initializeTestContext(ClassLoader classLoader) {
        // check if provider is already injected (for testing)
        TestContextInitializerFactoryProvider testContextInitProvider = (testContextInitializerFactoryProvider == null)
                ? TestContextInitializerFactoryProvider.createInstanceFromClasspath(classLoader)
                : testContextInitializerFactoryProvider;


        Map<String, EventContext> eventContextMap = eventSchedulerContext.get().getEventContexts().stream()
                .filter(EventContext::isEnabled) // only initialize test contexts for enabled events
                .collect(Collectors.toMap(EventContext::getName, e -> e,
                        (e1, e2) -> { logger.warn("found duplicate event context: " + e2.getEventFactory() + "-" + e2.getName()); return e1; }));

        List<TestContextInitializerFactory> testContextInitializerFactories = testContextInitProvider.getTestContextInitializerFactories();

        List<TestContextInitializer> testContextInitializers = testContextInitializerFactories.stream()
                .map(factory -> factory.create(eventContextMap.get(factory.getEventContextClassname()), logger))
                .collect(Collectors.toList());

        logger.info("init test context");
        final AtomicReference<TestContext> testContext = new AtomicReference<>(eventSchedulerContext.get().getTestContext());
        testContextInitializers.forEach(testContextInitializer -> {
            logger.info("found TestContextInitializer: " + testContextInitializer.getClass().getName());
            testContext.set(testContextInitializer.extendTestContext(testContext.get()));
        });

        this.eventSchedulerContext.set(eventSchedulerContext.get().withTestContext(testContext.get()));
    }

    @SuppressWarnings("unchecked")
    private Event createEvent(EventFactoryProvider provider, EventContext context, TestContext testContext, EventMessageBus messageBus) {
        String factoryClassName = context.getEventFactory();
        String eventName = context.getName();
        EventLogger eventLogger = new EventLoggerWithName(eventName, removeFactoryPostfix(factoryClassName), logger);

        logger.debug("create event: " + eventName + " with factory: " + factoryClassName + " and context: " + context);

        // create has raw type usage, so we have @SuppressWarnings("unchecked")
        return provider.factoryByClassName(factoryClassName)
                .orElseThrow(() -> new RuntimeException(factoryClassName + " not registered via META-INF/services"))
                .create(context, testContext, messageBus, eventLogger);
    }

    private String removeFactoryPostfix(String factoryClassName) {
        int index = factoryClassName.indexOf("Factory");
        return index != -1 ? factoryClassName.substring(0, index) : factoryClassName;
    }

    private List<CustomEvent> generateCustomEventSchedule(String text, EventLogger logger, ClassLoader classLoader) {
        EventGenerator eventGenerator;
        EventGeneratorProperties eventGeneratorProperties;

        if (text == null) {
            eventGeneratorProperties = new EventGeneratorProperties();
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(eventGeneratorProperties, myLogger);
        }
        else if (EventGeneratorProperties.hasLinesThatStartWithMetaPropertyPrefix(text)) {

            eventGeneratorProperties = new EventGeneratorProperties(text);

            String generatorClassname = eventGeneratorProperties.getMetaProperty(EventGeneratorMetaProperty.generatorFactoryClass.name());

            EventGeneratorFactory eventGeneratorFactory = findAndCreateEventScheduleGenerator(logger, generatorClassname, classLoader);

            EventLoggerWithName myLogger = new EventLoggerWithName("customFactory", generatorClassname, logger);
            eventGenerator = eventGeneratorFactory.create(eventGeneratorProperties, myLogger);
        }
        else {
            // assume the default input of lines of events
            Map<String, String> properties = new HashMap<>();
            properties.put("eventSchedule", text);

            eventGeneratorProperties = new EventGeneratorProperties(properties);
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(eventGeneratorProperties, myLogger);
        }

        return eventGenerator.generate();
    }

    /**
     * Provide schedule event as "duration|eventname(description)|json-settings".
     * The duration is in ISO-8601 format period format, e.g. 3 minutes 15 seconds
     * is PT3M15S.
     * <br/>
     * One schedule event per line.
     * <br/>
     * Or provide an EventScheduleGenerator implementation as:
     *
     * <pre>
     *      {@literal @}generator-class=io.perfana.event.MyEventGenerator
     *      foo=bar
     * </pre>
     *
     * @param customEventsText e.g. PT3M15S|heapdump(1st heapdump)|{'server': 'test-server-1'}
     * @return this
     */
    public EventSchedulerBuilderInternal setCustomEvents(String customEventsText) {
        if (customEventsText != null) {
            this.customEventsText = customEventsText;
        }
        return this;
    }

    private EventGeneratorFactory findAndCreateEventScheduleGenerator(EventLogger logger, String generatorFactoryClassname, ClassLoader classLoader) {
        EventGeneratorFactoryProvider provider =
                EventGeneratorFactoryProvider.createInstanceFromClasspath(logger, classLoader);

        EventGeneratorFactory generatorFactory = provider.find(generatorFactoryClassname);

        if (generatorFactory == null) {
            throw new EventSchedulerRuntimeException("unable to find EventScheduleGeneratorFactory implementation class: " + generatorFactoryClassname);
        }
        return generatorFactory;
    }

    /**
     * Event name should be unique: it is used in logging and as lookup key.
     * @param eventContext the config to add, the eventConfig must contain an testConfig
     * @return this
     */
    private EventSchedulerBuilderInternal addEvent(EventContext eventContext) {
        EventContext existingEventContext = eventContexts.putIfAbsent(eventContext.getName(), eventContext);
        if (existingEventContext != null) {
            throw new EventSchedulerRuntimeException("Event name is not unique: " + eventContext.getName());
        }
        return this;
    }

    /**
     * Optional. Default is probably good.
     * @param eventFactoryProvider The event factory provider to use.
     */
    EventSchedulerBuilderInternal setEventFactoryProvider(EventFactoryProvider eventFactoryProvider) {
        this.eventFactoryProvider = eventFactoryProvider;
        return this;
    }

    /**
     * Optional. Default is probably good: the async event broadcaster.
     * @param eventBroadcasterFactory the broadcaster implementation to use
     */
    EventSchedulerBuilderInternal setEventBroadcasterFactory(EventBroadcasterFactory eventBroadcasterFactory) {
        this.eventBroadcasterFactory = eventBroadcasterFactory;
        return this;
    }

}