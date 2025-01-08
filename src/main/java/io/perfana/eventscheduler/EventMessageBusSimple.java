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
package io.perfana.eventscheduler;

import io.perfana.eventscheduler.api.message.EventMessage;
import io.perfana.eventscheduler.api.message.EventMessageBus;
import io.perfana.eventscheduler.api.message.EventMessageReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very simple EventMessageBus implementation.
 * Sends messages to all receivers known at send time.
 * Also sends message to original sender if it is also receiving.
 */
public class EventMessageBusSimple implements EventMessageBus {

    private final List<EventMessageReceiver> receivers = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void send(EventMessage message) {
        receivers.forEach(r -> r.receive(message));
    }

    @Override
    public void addReceiver(EventMessageReceiver eventMessageReceiver) {
        receivers.add(eventMessageReceiver);
    }
}
