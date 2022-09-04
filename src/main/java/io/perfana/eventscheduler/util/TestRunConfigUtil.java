package io.perfana.eventscheduler.util;

import io.perfana.eventscheduler.api.message.EventMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestRunConfigUtil {

    private TestRunConfigUtil() {}

    public static EventMessage createTestRunConfigMessage(String pluginName, String key, String value, String output, String tags, String excludes, String includes) {

        return EventMessage.builder()
                .pluginName(pluginName)
                .variable("message-type", "test-run-config")
                .variable("output", output)
                .variable("key", key)
                .variable("tags", tags)
                .variable("excludes", excludes)
                .variable("includes", includes)
                .message(value).build();
    }

    public static EventMessage createTestRunConfigMessageKeys(String pluginName, Map<String, String> keyValuePairs, String tags) {

        List<String> keyValueList = new ArrayList<>();
        keyValuePairs.forEach((key, value) -> { keyValueList.add(key); keyValueList.add(value); });

        return EventMessage.builder()
                .pluginName(pluginName)
                .variable("message-type", "test-run-config")
                .variable("output", "keys")
                .variable("tags", tags)
                .variable("excludes", "")
                .variable("includes", "")
                .message(String.join("\u0000", keyValueList)).build();
    }

}
