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
package io.perfana.eventscheduler.log;

import io.perfana.eventscheduler.api.EventLogger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CountLogMatchesEventLogger implements EventLogger {

    private final EventLogger wrappedEventLogger;

    private final AtomicInteger matchCounter = new AtomicInteger(0);
    private final Function<String, Boolean> matchFunction;

    private CountLogMatchesEventLogger(EventLogger wrappedEventLogger, Function<String, Boolean> matchFunction) {
        this.wrappedEventLogger = wrappedEventLogger;
        this.matchFunction = matchFunction;
    }

    public static CountLogMatchesEventLogger of(EventLogger eventLogger, Function<String, Boolean> matchFunction) {
        return new CountLogMatchesEventLogger(eventLogger, matchFunction);
    }

    @Override
    public void info(String message) {
        checkForMatch(message);
        wrappedEventLogger.info(message);
    }

    @Override
    public void warn(String message) {
        wrappedEventLogger.warn(message);
    }

    @Override
    public void error(String message) {
        checkForMatch(message);
        wrappedEventLogger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        checkForMatch(message);
        wrappedEventLogger.error(message, throwable);
    }

    @Override
    public void debug(String message) {
        checkForMatch(message);
        wrappedEventLogger.debug(message);
    }

    @Override
    public boolean isDebugEnabled() {
        return wrappedEventLogger.isDebugEnabled();
    }

    public int matchCount() {
        return matchCounter.get();
    }

    private int checkForMatch(String message) {
        return matchCounter.updateAndGet(i -> matchFunction.apply(message) ? i + 1 : i);
    }
}
