/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.eventscheduler.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSchedulerConfig {
    @Builder.Default
    private boolean debugEnabled = false;
    @Builder.Default
    private boolean schedulerEnabled = true;
    @Builder.Default
    private boolean failOnError = true;
    @Builder.Default
    private boolean continueOnAssertionFailure = true;
    @Builder.Default
    private Integer keepAliveIntervalInSeconds = 30;
    private String scheduleScript;
    private TestConfig testConfig;
    private List<EventConfig> eventConfigs;
}