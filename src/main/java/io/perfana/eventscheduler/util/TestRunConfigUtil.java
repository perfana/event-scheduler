package io.perfana.eventscheduler.util;

import io.perfana.eventscheduler.api.message.EventMessage;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestRunConfigUtil {

    public static final String MESSAGE_KEY_VALUE_DELIMITER = "\u0000";
    public static final String VALUE_LIST_DELIMITER = "\n";

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
                .message(String.join(MESSAGE_KEY_VALUE_DELIMITER, keyValueList)).build();
    }

    public static String hashSecret(String secretToHash) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(secretToHash.getBytes());
            return "(hashed-secret)" + toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return "(hashed-secret)" + "(sorry, no algorithm found)";
        }
    }

    private static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

}
