package io.perfana.eventscheduler.api;

import io.perfana.eventscheduler.api.config.TestContext;

public interface TestContextInitializer {
    TestContext extendTestContext(TestContext testContext);
}
