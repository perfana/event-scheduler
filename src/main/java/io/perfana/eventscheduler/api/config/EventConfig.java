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
import net.jcip.annotations.NotThreadSafe;

/**
 * The EventConfig is used is given to each event call.
 *
 * One required field is the 'eventFactory' that contains the fully qualified
 * class name of the factory class for the event.
 *
 * Another field is 'enabled', default is true.
 * If set to false, the event will not be active.
 */
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NotThreadSafe
@EqualsAndHashCode
public class EventConfig {
    @Builder.Default
    private String name = "anonymous-" + System.currentTimeMillis();
    @Builder.Default
    private String eventFactory = "eventFactory.not.Set";
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private String scheduleScript = null;
    @Builder.Default
    private boolean readyForStartParticipant = false;

    // if all vote stop (via keep alive calls with StopTestRunException): then stop the test run
    @Builder.Default
    private boolean continueOnKeepAliveParticipant = false;

    public EventContext toContext() {
        return EventContext.builder()
                .name(name)
                .eventFactory(eventFactory)
                .enabled(enabled)
                .scheduleScript(scheduleScript)
                .readyForStartParticipant(readyForStartParticipant)
                .continueOnKeepAliveParticipant(continueOnKeepAliveParticipant)
            .build();
    }

}
