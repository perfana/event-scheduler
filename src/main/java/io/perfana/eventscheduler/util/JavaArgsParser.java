package io.perfana.eventscheduler.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Convenience class for parsing jvm args into name-value pairs.
 * Can be used to parse JAVA_OPTS and other jvm arg constructions.
 */
public class JavaArgsParser {

    private JavaArgsParser() {}

    public static final String JMV_ARG_PREFIX = "jvmArg.";
    public static final Pattern CLEAN_OPTION_PATTERN = Pattern.compile("[+-:]");
    private static final List<String> SECRETS_KEY_PARTS = Arrays.asList("password,token,key".split(","));

    private static KeyValuePair jvmArgToKeyValue(String jvmArg) {
        final String key;
        final String value;

        String[] splitJvmArg = splitOnFirstOccurrence(jvmArg, "=");
        String jvmArgPart1 = splitJvmArg[0];
        String jvmArgPart2 = splitJvmArg.length == 2 ? splitJvmArg[1] : "";

        if (jvmArg.startsWith("-D")) {
            key = jvmArgPart1.substring(1);
            value = jvmArgPart2.length() == 0 ? jvmArgPart1.substring(2) : jvmArgPart2;
        } else if (jvmArg.startsWith("-XX:")) {
            key = CLEAN_OPTION_PATTERN.matcher(jvmArgPart1).replaceAll("");
            value = jvmArgPart2.length() == 0 ? jvmArgPart1.substring(4) : jvmArgPart2;
        } else if (jvmArg.startsWith("-")) {
            String option = jvmArg.substring(1);
            // -d32 and -d64 are possible, hard to parse generically
            if (jvmArg.equals("-d32")) {
                key = "d";
                value = "32";
            }
            else if (jvmArg.equals("-d64")) {
                key = "d";
                value = "64";
            }
            else if (option.startsWith("Xms") || option.startsWith("Xmx") || option.startsWith("Xss") || option.startsWith("Xssi")) {
                key = option.substring(0, 3);
                value = option.substring(3);
            }
            else if (jvmArg.contains(":") || jvmArg.contains("=")) {
                // limit = 2: only split on first occurrence
                String[] split = splitOnFirstOccurrence(jvmArg, "[:=]");
                key =  CLEAN_OPTION_PATTERN.matcher(split[0].substring(1)).replaceAll("");
                value = split.length == 2 ? split[1] : split[0].substring(2);
            }
            else {
                key = option;
                if (jvmArgPart2.length() == 0) {
                    value = option.startsWith("X") ? option.substring(1) : option;
                }
                else {
                    value = jvmArgPart2;
                }
            }
        }
        else {
            // unexpected jvmArg format found: does not start with - (dash)
            key = jvmArgPart1;
            value = jvmArgPart2;
        }
        return new KeyValuePair(JMV_ARG_PREFIX + key, value);
    }

    private static String[] splitOnFirstOccurrence(String text, String regex) {
        return text.split(regex, 2);
    }

    public static class KeyValuePair {

        private final String key;
        private final String value;

        KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static boolean isNoSecret(KeyValuePair kvp) {
        String lowerCaseKey = kvp.getKey().toLowerCase();
        return SECRETS_KEY_PARTS.stream().noneMatch(lowerCaseKey::contains);
    }

    /**
     * @param jvmArgs a space separated string with jvm args
     * @return map with key-value pairs for jvm arguments
     */
    public static Map<String, String> createJvmArgsTestConfigLines(String jvmArgs) {
        List<String> listOfArgs = Arrays.asList(jvmArgs.split(" "));
        return createJvmArgsTestConfigLines(listOfArgs);
    }

    public static Map<String, String> createJvmArgsTestConfigLines(List<String> jvmArgs) {
        // merge jvm args with same key by newline
        return jvmArgs.stream()
                .map(JavaArgsParser::jvmArgToKeyValue)
                .filter(JavaArgsParser::isNoSecret)
                .collect(Collectors.toMap(
                        JavaArgsParser.KeyValuePair::getKey,
                        JavaArgsParser.KeyValuePair::getValue,
                        (left, right) -> String.join(TestRunConfigUtil.VALUE_LIST_DELIMITER, left, right)));
    }

    public static boolean isJavaCommandArgsProperty(String name) {
        if (name == null) return false;
        return name.equals("JAVA_OPTS") || name.equals("JDK_JAVA_OPTIONS") || name.equals("JAVA_TOOL_OPTIONS");
    }

}
