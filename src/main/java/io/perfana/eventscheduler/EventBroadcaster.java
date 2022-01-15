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
package io.perfana.eventscheduler;

import io.perfana.eventscheduler.api.CustomEvent;
import io.perfana.eventscheduler.api.EventCheck;
import io.perfana.eventscheduler.exception.handler.AbortSchedulerException;
import io.perfana.eventscheduler.exception.handler.KillSwitchException;

import java.util.List;
import java.util.Queue;

public interface EventBroadcaster {

    void broadcastBeforeTest();

    void broadcastStartTest();

    void broadcastAfterTest();

    void broadcastKeepAlive();

    void broadcastAbortTest();

    void broadcastCustomEvent(CustomEvent event);

    List<EventCheck> broadcastCheck();

    void shutdownAndWaitAllTasksDone(long timeoutSeconds);

    default void throwAbortOrKillWitchException(Queue<Throwable> exceptions) {
        exceptions.stream()
            .filter(t -> t instanceof AbortSchedulerException)
            .findFirst()
            .ifPresent(abort -> {
                throw new AbortSchedulerException("Found abort scheduler request during keep-alive broadcast: " + abort.getMessage());
            });

        exceptions.stream()
            .filter(t -> t instanceof KillSwitchException)
            .findFirst()
            .ifPresent(kill -> {
                throw new KillSwitchException("Found kill switch request during keep-alive broadcast: " + kill.getMessage());
            });
    }
}