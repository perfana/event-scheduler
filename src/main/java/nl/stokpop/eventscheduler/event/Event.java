package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.TestContext;

/**
 * This interface can be implemented in other jars and be put on the classpath.
 *
 * Provide a file in META-INF/services/nl.stokpop.eventscheduler.Event that contains the
 * fully qualified name of the implementing class.
 *
 * This class will be used when these events are called. Possibly you can even provide multiple implementations
 * on the classpath that will all be called.
 *
 * For more information on how this technically works, check out javadoc of java.util.ServiceLoader.
 */
public interface Event {

    /**
     * @return name of the test event.
     */
    String getName();

    /**
     * Called before the test run starts. You can for instance cleanup the test environment and/or
     * restart the server under test.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     */
    void beforeTest(TestContext context, EventProperties properties);

    /**
     * Called after the test run is done. Use for instance to start creating a report of some sort or
     * remove the test environment.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     */
    void afterTest(TestContext context, EventProperties properties);
    
    /**
     * Called for each keep alive event for this test run.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     */
    void keepAlive(TestContext context, EventProperties properties);

    /**
     * Called for test abort.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     */
    void abortTest(TestContext context, EventProperties properties);

    /**
     * Called to check test results. Can be used to have a test run fail or succeed.
     * Fail by throwing a runtime exception.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     */
    void checkTest(TestContext context, EventProperties properties);

    /**
     * Called for each custom event, according to the custom even schedule.
     * @param context the test run context
     * @param properties e.g. REST_URL="https://my-rest-url"
     * @param scheduleEvent the custom event, use to execute specific behaviour in the event handler
     */
    void customEvent(TestContext context, EventProperties properties, ScheduleEvent scheduleEvent);


}