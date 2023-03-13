/*
 * Copyright (C) 2023 Peter Paul Bakker, Perfana
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.jcip.annotations.NotThreadSafe;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NotThreadSafe
public class TestConfig {
    @Builder.Default
    private String systemUnderTest = "UNKNOWN_SYSTEM_UNDER_TEST";
    @Builder.Default
    private String workload = "UNKNOWN_WORKLOAD";
    @Builder.Default
    private String testEnvironment = "UNKNOWN_TEST_ENVIRONMENT";
    @Builder.Default
    private String productName = "ANONYMOUS_PRODUCT";
    @Builder.Default
    private String dashboardName = "ANONYMOUS_DASHBOARD";
    @Builder.Default
    private String testRunId = "ANONYMOUS_TEST_ID";
    @Builder.Default
    private String buildResultsUrl = null;
    @Builder.Default
    private String version = "1.0.0-SNAPSHOT";
    @Builder.Default
    private String annotations = "";
    @Builder.Default
    private List<String> tags = Collections.emptyList();
    @Builder.Default
    private Integer rampupTimeInSeconds = 30;
    @Builder.Default
    private Integer constantLoadTimeInSeconds = 570;

    public TestContext toContext() {
        return TestContext.builder()
            .systemUnderTest(systemUnderTest)
            .workload(workload)
            .testEnvironment(testEnvironment)
            .productName(productName)
            .dashboardName(dashboardName)
            .testRunId(testRunId)
            .buildResultsUrl(buildResultsUrl)
            .version(version)
            .annotations(annotations)
            .tags(tags)
            .rampupTime(Duration.ofSeconds(rampupTimeInSeconds))
            .constantLoadTime(Duration.ofSeconds(constantLoadTimeInSeconds))
            .build();
    }
}
