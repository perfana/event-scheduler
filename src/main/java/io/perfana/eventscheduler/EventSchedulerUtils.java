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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EventSchedulerUtils {

    private EventSchedulerUtils() {}

    public static int parseInt(String variableName, String numberString, int defaultValue) {
        int value = defaultValue;
        try {
            value = Integer.parseInt(numberString);
        } catch (NumberFormatException e) {
            System.err.printf("unable to parse value of [%s=%s]: using default value [%d]. Error message: %s.%n", variableName, numberString, defaultValue, e.getMessage());
        }
        return value;
    }

    public static boolean hasValue(String variable) {
        return variable != null && !variable.trim().isEmpty();
    }

    public static int countOccurrences(String search, String text) {
        Matcher matcher = Pattern.compile(search).matcher(text);
        int count = 0;
        while (matcher.find()) { count = count + 1; }
        return count;
    }

    public static List<String> splitAndTrim(String text, String separator) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
