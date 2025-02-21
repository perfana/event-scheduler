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
package io.perfana.eventscheduler.api.config;

import lombok.*;
import net.jcip.annotations.Immutable;

import java.time.Duration;
import java.util.List;

@Value
@Builder(access = AccessLevel.PROTECTED)
@Immutable
public class EventSchedulerContext {
    @Builder.Default
    boolean debugEnabled = false;
    @Builder.Default
    boolean schedulerEnabled = true;
    @Builder.Default
    boolean failOnError = true;
    @Builder.Default
    boolean continueOnEventCheckFailure = false;
    @Builder.Default
    Duration keepAliveInterval = Duration.ofSeconds(30);
    @Builder.Default
    String scheduleScript = "";
    @Singular
    List<EventContext> eventContexts;
    @Builder.Default
    @With
    TestContext testContext = TestContext.builder().build();
}
