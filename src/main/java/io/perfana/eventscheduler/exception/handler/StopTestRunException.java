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
package io.perfana.eventscheduler.exception.handler;

import io.perfana.eventscheduler.api.SchedulerExceptionType;

/**
 * An event can throw StopTestRunException from keepAlive event to
 * indicate that the test run can be stopped. Only when all registered
 * "continueOnKeepAlive" participants throw this exception, the test run
 * shall be stopped.
 */
public class StopTestRunException extends SchedulerHandlerException {
    public StopTestRunException(String message) {
        super(message);
    }

    @Override
    public SchedulerExceptionType getExceptionType() {
        return SchedulerExceptionType.STOP;
    }
}
