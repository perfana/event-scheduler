/*
 * Copyright (C) 2024 Peter Paul Bakker, Perfana
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