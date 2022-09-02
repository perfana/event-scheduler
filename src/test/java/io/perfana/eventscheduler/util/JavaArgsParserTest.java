package io.perfana.eventscheduler.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaArgsParserTest extends TestCase {

    public void testIsNoSecret() {
        assertFalse(JavaArgsParser.isNoSecret(new JavaArgsParser.KeyValuePair("password", "secret")));
        assertTrue(JavaArgsParser.isNoSecret(new JavaArgsParser.KeyValuePair("anything", "secret")));
    }

    public void testCreateJvmArgsTestConfigLinesAsString() {
        Map<String, String> mapOfArgs = JavaArgsParser.createJvmArgsTestConfigLines("-Xms1g -DskipTests");
        assertEquals("1g", mapOfArgs.get("jvmArg.Xms"));
        assertEquals("skipTests", mapOfArgs.get("jvmArg.DskipTests"));
    }

    public void testCreateJvmArgsTestConfigLines() {
        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-Xms1g");
        jvmArgs.add("-Xmx2g");
        jvmArgs.add("-Xss10k");
        jvmArgs.add("-XX:-UseSerialGC");
        jvmArgs.add("-XX:PreBlockSpin=10");
        jvmArgs.add("-XX:HeapDumpPath=./java_pid1234.hprof");
        jvmArgs.add("-javaagent:/full/path/to/agent.jar");
        jvmArgs.add("-Dthis.is.a.system.property=as-Defined-by-user");
        jvmArgs.add("-Xthr:minimizeUserCPU"); // ibm jvm option
        jvmArgs.add("-Xbootclasspath/a:/path/to/jar1.jar:/path/to/jar2.jar");
        jvmArgs.add("-Xbootclasspath/p:/path/to/jar1.jar:/path/to/jar2.jar");
        jvmArgs.add("-Xbootclasspath:/path/to/jar1.jar:/path/to/jar2.jar");
        jvmArgs.add("-Xnolinenumbers"); // ibm jvm option
        jvmArgs.add("-XX:StartFlightRecording=duration=60s,filename=c:\\temp\\myrecording.jfr");
        // duplicates are possible too: we expect these to be merged
        jvmArgs.add("-Xlog:gc*=debug:stdout");
        jvmArgs.add("-Xlog:gc*=debug:file=/tmp/gc.log");
        jvmArgs.add("-Xloggc:/home/user/log/gc.log");
        jvmArgs.add("-d32");
        jvmArgs.add("-server");
        jvmArgs.add("-XX:OnOutOfMemoryError=/bin/date; /bin/echo custom message;/bin/kill -9 %p");
        jvmArgs.add("-XX:SomeDoubleEqualsProp=/bin/date=123341");
        jvmArgs.add("option=test");
        jvmArgs.add("-DmyPassword=s3cr3t");
        jvmArgs.add("-DmyToken=s3cr3t");

        Map<String, String> jvmArgsTestConfigLines = JavaArgsParser.createJvmArgsTestConfigLines(jvmArgs);
        //System.out.println(jvmArgsTestConfigLines);
        // minus one because we expect one merged entry for Xlog, and two secrets filtered out
        assertEquals(jvmArgs.size() - 3, jvmArgsTestConfigLines.size());
        assertEquals("1g", jvmArgsTestConfigLines.get("jvmArg.Xms"));
        assertEquals("2g", jvmArgsTestConfigLines.get("jvmArg.Xmx"));
        assertEquals("10k", jvmArgsTestConfigLines.get("jvmArg.Xss"));
        assertEquals("-UseSerialGC", jvmArgsTestConfigLines.get("jvmArg.XXUseSerialGC"));
        assertEquals("10", jvmArgsTestConfigLines.get("jvmArg.XXPreBlockSpin"));
        assertEquals("./java_pid1234.hprof", jvmArgsTestConfigLines.get("jvmArg.XXHeapDumpPath"));
        assertEquals("/full/path/to/agent.jar", jvmArgsTestConfigLines.get("jvmArg.javaagent"));
        assertEquals("as-Defined-by-user", jvmArgsTestConfigLines.get("jvmArg.Dthis.is.a.system.property"));
        assertEquals("minimizeUserCPU", jvmArgsTestConfigLines.get("jvmArg.Xthr"));
        assertEquals("/path/to/jar1.jar:/path/to/jar2.jar", jvmArgsTestConfigLines.get("jvmArg.Xbootclasspatha"));
        assertEquals("/path/to/jar1.jar:/path/to/jar2.jar", jvmArgsTestConfigLines.get("jvmArg.Xbootclasspathp"));
        assertEquals("/path/to/jar1.jar:/path/to/jar2.jar", jvmArgsTestConfigLines.get("jvmArg.Xbootclasspath"));
        assertEquals("nolinenumbers", jvmArgsTestConfigLines.get("jvmArg.Xnolinenumbers"));
        assertEquals("duration=60s,filename=c:\\temp\\myrecording.jfr", jvmArgsTestConfigLines.get("jvmArg.XXStartFlightRecording"));
        // expect two -Xlog entries, concatenated by newline
        assertEquals("gc*=debug:stdout\ngc*=debug:file=/tmp/gc.log", jvmArgsTestConfigLines.get("jvmArg.Xlog"));
        assertEquals("/home/user/log/gc.log", jvmArgsTestConfigLines.get("jvmArg.Xloggc"));
        assertEquals("32", jvmArgsTestConfigLines.get("jvmArg.d"));
        assertEquals("/bin/date=123341", jvmArgsTestConfigLines.get("jvmArg.XXSomeDoubleEqualsProp"));
        // unexpected format
        assertEquals("test", jvmArgsTestConfigLines.get("jvmArg.option"));
        assertNull("should not contain s3cr3t", jvmArgsTestConfigLines.get("jvmArg.DmyPassword"));
        assertNull("should not contain s3cr3t", jvmArgsTestConfigLines.get("jvmArg.DmyToken"));
    }
}