package io.perfana.eventscheduler.util;

import org.junit.Assert;
import org.junit.Test;

public class TestRunConfigUtilTest {

    @Test
    public void testHashSecret() {
        // this is without salting, so should be improved
        Assert.assertEquals("(hashed-secret)5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", TestRunConfigUtil.hashSecret("password"));
    }
}