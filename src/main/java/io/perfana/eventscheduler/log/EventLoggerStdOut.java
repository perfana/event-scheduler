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

/**
 * Logs to standard out for convenience. Add your own logger preferably where possible.
 */
public class EventLoggerStdOut implements EventLogger {

    public static final EventLoggerStdOut INSTANCE = new EventLoggerStdOut(false);
    public static final EventLoggerStdOut INSTANCE_DEBUG = new EventLoggerStdOut(true);

    private final boolean debug;

    protected EventLoggerStdOut(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void info(final String message) {
        say("INFO ", message);
    }

    @Override
    public void warn(final String message) {
        say("WARN ", message);
    }

    @Override
    public void error(final String message) {
        say("ERROR", message);
    }

    @Override
    public void error(final String message, Throwable throwable) {
        say("ERROR", message, throwable);
    }

    @Override
    public void debug(final String message) {
        if (debug) say("DEBUG", message);
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    private void say(String level, String something) {
        System.out.printf("## %s ## %s%n", level, something);
    }
    private void say(String level, String something, Throwable throwable) {
        System.out.printf("## %s ## %s %s: %s%n", level, something, throwable.getClass().getName(), throwable.getMessage());
        throwable.printStackTrace();
    }
}
