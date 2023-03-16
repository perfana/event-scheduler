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
package io.perfana.eventscheduler.api.config;

import io.perfana.eventscheduler.api.EventLogger;
import lombok.*;
import net.jcip.annotations.NotThreadSafe;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NotThreadSafe
public class EventSchedulerConfig {
    @Builder.Default
    private boolean debugEnabled = false;
    @Builder.Default
    private boolean schedulerEnabled = true;
    @Builder.Default
    private boolean failOnError = true;
    @Builder.Default
    private boolean continueOnEventCheckFailure = true;
    @Builder.Default
    private Integer keepAliveIntervalInSeconds = 30;
    @Builder.Default
    private String scheduleScript = null;
    @Singular
    private List<EventConfig> eventConfigs;
    @Builder.Default
    private TestConfig testConfig = TestConfig.builder().build();

    public EventSchedulerContext toContext(EventLogger logger) {

        // inject top level config in all event contexts
        List<EventContext> eventContexts = eventConfigs.stream()
            .map(EventConfig::toContext)
            .collect(Collectors.toList());

        String allScheduleScripts = collectScheduleScripts(eventContexts, this.scheduleScript);

        return EventSchedulerContext.builder()
            .debugEnabled(debugEnabled)
            .schedulerEnabled(schedulerEnabled)
            .failOnError(failOnError)
            .continueOnEventCheckFailure(continueOnEventCheckFailure)
            .keepAliveInterval(Duration.ofSeconds(keepAliveIntervalInSeconds))
            .scheduleScript(allScheduleScripts)
            .eventContexts(eventContexts)
            .testContext(testConfig.toContext())
            .build();
    }

    private static String collectScheduleScripts(List<EventContext> eventContexts, String topScheduleScript) {

        String allSubScripts = eventContexts.stream()
            .map(EventContext::getScheduleScript)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        if (topScheduleScript == null) {
            return removeEmptyLines(allSubScripts);
        }
        else {
            return removeEmptyLines(String.join("\n", topScheduleScript, allSubScripts));
        }

    }

    private static String removeEmptyLines(String text) {
        return Arrays.stream(text.split("\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));
    }

}
