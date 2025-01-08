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

import io.perfana.eventscheduler.api.TestContextInitializerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TestContextInitializerFactoryProvider {

    private final Map<String, TestContextInitializerFactory<?>> testContextInitializerFactories;

    private TestContextInitializerFactoryProvider(List<TestContextInitializerFactory<?>> testContextInitializerFactories) {
        Map<String, TestContextInitializerFactory<?>> map = testContextInitializerFactories.stream()
                .collect(Collectors.toMap(f -> f.getClass().getName(), f -> f));
        this.testContextInitializerFactories = Collections.unmodifiableMap(map);
    }

    public static TestContextInitializerFactoryProvider createInstanceFromClasspath() {
        return createInstanceFromClasspath(null);
    }

    public static TestContextInitializerFactoryProvider createInstanceFromClasspath(ClassLoader classLoader) {
        ServiceLoader<TestContextInitializerFactory> testContextInitializerServiceLoader = classLoader == null
                ? ServiceLoader.load(TestContextInitializerFactory.class)
                : ServiceLoader.load(TestContextInitializerFactory.class, classLoader);
        // java 9+: List<EventFactory> testContextInitializerFactories = testContextInitializerServiceLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        List<TestContextInitializerFactory<?>> testContextInitializerFactories = new ArrayList<>();
        for (TestContextInitializerFactory<?> testContextInitializerFactory : testContextInitializerServiceLoader) {
            testContextInitializerFactories.add(testContextInitializerFactory);
        }
        return new TestContextInitializerFactoryProvider(testContextInitializerFactories);
    }

    /**
     * Find factory by given class name.
     *
     * @param className the full class name of the factory
     * @return an optional which is empty if the factory for given class name is not present
     */
    public Optional<TestContextInitializerFactory> factoryByClassName(String className) {
        return Optional.ofNullable(testContextInitializerFactories.get(className));
    }

    public List<TestContextInitializerFactory> getTestContextInitializerFactories() {
        return new ArrayList<>(testContextInitializerFactories.values());
    }

}
