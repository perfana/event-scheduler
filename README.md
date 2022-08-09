# event-scheduler

Add this java library to your project to generate timed events.

For instance during a Perfana performance test to dynamically increase response times over time.

To be used in combination with plugins:

* [perfana-java-client](https://github.com/perfana/perfana-java-client) - connect with Perfana
* [test-events-hello-world](https://github.com/perfana/test-events-hello-world) - test and example plugin 
* [test-events-command-runner](https://github.com/perfana/test-events-command-runner) - run a command at certain events 
* [test-events-loadrunner-cloud](https://github.com/perfana/test-events-loadrunner-cloud) - connect with LoadRunner Cloud, start/stop tests 
* [test-events-wiremock](https://github.com/perfana/test-events-wiremock) - dynamically change wiremock delays at specified times 
* [test-events-springboot](https://github.com/perfana/test-events-springboot) - connect to spring boot app, e.g. fetch values of settings via actuator

Custom plugins can be build as described below. Tip: base a new plugin on the test-events-hello-world example.

## usage via Maven

The following event-scheduler Maven plugins can be used, these are ready-to-go:

* [event-scheduler-maven-plugin](https://github.com/perfana/event-scheduler-maven-plugin) runs plain event-scheduler via Maven
* [event-gatling-maven-plugin](https://github.com/perfana/events-gatling-maven-plugin) runs Gatling load test via Maven with event-scheduler build-in
* [event-jmeter-maven-plugin](https://github.com/perfana/events-jmeter-maven-plugin)  runs jMeter load test via Maven with event-scheduler build-in

## event-scheduler Maven plugin example

To use the events via the `event-scheduler-maven-plugin`, the jar with the
implementation details must be on the classpath of the plugin.

You can use the `dependencies` element inside the `plugin` element.

For example, using the `test-events-hello-world` event-scheduler plugin (yes, a plugin of a plugin):

```xml 
<plugin>
    <groupId>io.perfana</groupId>
    <artifactId>event-scheduler-maven-plugin</artifactId>
    <version>1.1.0</version>
    <configuration>
        <eventSchedulerConfig>
            <debugEnabled>false</debugEnabled>
            <schedulerEnabled>true</schedulerEnabled>
            <failOnError>true</failOnError>
            <continueOnEventCheckFailure>true</continueOnEventCheckFailure>
            <eventConfigs>
                <eventConfig implementation="io.perfana.helloworld.event.HelloWorldEventConfig">
                    <name>HelloEvent1</name>
                    <testConfig>
                        <systemUnderTest>my-application</systemUnderTest>
                        <version>1.2.3</version>
                        <workload>stress-test</workload>
                        <testEnvironment>loadtest</testEnvironment>
                        <testRunId>my-test-123</testRunId>
                        <buildResultsUrl>http://localhost:4000/my-test-123</buildResultsUrl>
                        <rampupTimeInSeconds>1</rampupTimeInSeconds>
                        <constantLoadTimeInSeconds>4</constantLoadTimeInSeconds>
                        <annotations>${annotation}</annotations>
                        <tags>
                            <tag>tag1-value</tag>
                            <tag>tag2-value</tag>
                        </tags>
                    </testConfig>
                    <scheduleScript>
                        PT1S|restart(restart with 2 replicas)|{ server:'myserver' replicas:2 tags: [ 'first', 'second' ] }
                    </scheduleScript>
                    <myRestService>https://my-rest-api</myRestService>
                    <myCredentials>${env.SECRET}</myCredentials>
                    <helloMessage>Hello, Hello World!</helloMessage>
                    <helloInitialSleepSeconds>1</helloInitialSleepSeconds>
                </eventConfig>
            </eventConfigs>
        </eventSchedulerConfig>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.perfana</groupId>
            <artifactId>test-events-hello-world</artifactId>
            <version>1.1.0</version>
        </dependency>
    </dependencies>
</plugin>
```

Note that the `<eventConfig implementation="...">` implementation field is mandatory, it defines the `EventConfig` subtype to use.
The name of an event, here `HelloEvent1`, should a unique event name. The event name is used in the logging.

## usage via code

Create an `EventScheduler` using the builder with an `EventSchedulerConfig`:

```java
EventLogger eventLogger = EventLoggerStdOut.INSTANCE;

String scheduleScript1 =
    "PT600S|scale-down|{ 'replicas':1 }\n" +
    "PT660S|heapdump|server=myserver.example.com;port=1567";

String scheduleScript2 =
    "PT1S|restart(restart to reset replicas)|{ 'server':'myserver' 'replicas':2, 'tags': [ 'first', 'second' ] }";

TestConfig testConfig = TestConfig.builder()
    .workload("testType")
    .testEnvironment("testEnv")
    .testRunId("testRunId")
    .buildResultsUrl("http://url")
    .version("version")
    .rampupTimeInSeconds(10)
    .constantLoadTimeInSeconds(300)
    .annotations("annotation")
    .variables(Collections.emptyMap())
    .tags(Arrays.asList("tag1","tag2"))
    .build();

// this class really needs to be on the classpath, otherwise: runtime exception, not found on classpath
String factoryClassName = "io.perfana.eventscheduler.event.EventFactoryDefault";

List<EventConfig> eventConfigs = new ArrayList<>();
eventConfigs.add(EventConfig.builder().name("myEvent1").eventFactory(factoryClassName).scheduleScript(scheduleScript2).build());
eventConfigs.add(EventConfig.builder().name("myEvent2").eventFactory(factoryClassName).build());
eventConfigs.add(EventConfig.builder().name("myEvent3").eventFactory(factoryClassName).build());

EventSchedulerConfig eventSchedulerConfig = EventSchedulerConfig.builder()
    .schedulerEnabled(true)
    .debugEnabled(false)
    .continueOnAssertionFailure(false)
    .failOnError(true)
    .keepAliveIntervalInSeconds(120)
    .testConfig(testConfig)
    .eventConfigs(eventConfigs)
    .scheduleScript(scheduleScript1)
    .build();

EventScheduler scheduler = EventSchedulerBuilder.of(eventSchedulerConfig, eventLogger);
```

Note that a lot of properties of the builders have decent defaults 
and do not need to be called, such as the retry and keep alive properties.

The `TestConfig` describes high level properties of a test run. 

A `TestConfig` can be set at the `EventSchedulerConfig` and at the `EventConfig`. When
set at the `EventSchedulerConfig`, no `EventConfig` are allowed to have an `TestConfig`.
If one `EventConfig` has a `TestConfig`, that one will be used as top level `TestConfig`.
It is not allowed to have multiple `EventConfig`s with a `TestConfig`.

When adding events, each event can have its own values. That makes it possible
to configure multiple Wiremock event that use different Wiremock urls for instance.
 
Then call these methods at the appropriate time:

* `scheduler.startSession()` - at start of the load test
* `scheduler.stopSession()` - at end of the load test
* `scheduler.checkResults()` - call to see if all checks of the test run are ok
* `scheduler.abortSession()` - call when the load test was aborted abnormally
* `scheduler.sendMessage(message)` - put a message on the event message bus

The `checkResults()` throws `EventCheckFailureException` in case there are
events that report a failure.

## test events

During a test run this Event Scheduler emits events. You can put
your own implementation of the `EventFactory` and `Event` interface on the classpath
and add your own code to these events. 

For event specific properties create a subclass of `EventConfig` and add the properties.
Use this `XxxEventConfig` in the generics of the `XxxEventFactory`. 
Override the `getEventFactory()` method in the `XxxEventConfig` class to define the
factory class to use:

```java 
@Override
public String getEventFactory() {
    return XxxEventFactory.class.getName();
}
```

Events triggers available, with example usage:
* _before test_ - use to restart servers or setup/cleanup environment
* _after test_ - start generating reports, clean up environment
* _keep alive calls_ - send keep alive calls to any remote API 
* _check result_ - after a test check results for the event, if failures are present the CI build can fail 
* _abort test_ - abort a running test, do not run to end
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 

The keep alive is scheduled each 15 seconds during the test. The keep-alive schedule can also be changed.
	
## custom events

You can provide custom events via a list of 

    <duration,eventName(description),eventSettings> 

tuples, one per line.

The eventName can be any unique name among the custom events. You can use this eventName
in your own implementation of the Event interface to select what code to execute.

The description can be any text to explain what the event at that time is about. It will
be sent to remote systems and is for instance shown in the graphs as an event marker. 
If no description is provided, the description is 'eventName-duration'.

You can even send some specific settings to the event, using the eventSettings String.
Decide for your self if you want this to be just one value, a list of key-value pairs, 
json snippet or event base64 encoded contents.

Example:

```java
EventSchedulerConfig.builder().scheduleScript(eventSchedule).build()
```    
And as input:

```java
String eventSchedule =
        "PT5S|restart(restart with 2 replicas)|{ server:'myserver' replicas:2 tags: [ 'first', 'second' ] }\n" +
        "PT10M|scale-down\n" +
        "PT10M45S|heapdump(generate heapdump on port 1567)|server=myserver.example.com;port=1567\n" +
        "PT15M|scale-up|{ replicas:2 }\n";
```

Note the usage of ISO-8601 duration or period format, defined as PT(n)H(n)M(n)S.
Each period is from the start of the test, so not between events!

Above can be read as: 
* send restart event 5 seconds after start of test run. 
* send scale-down event 10 minutes after start of the test run.
* send heapdump event 10 minutes and 45 seconds after start of test run.
* send scale-up event 15 minutes after start of test run.

The settings will be sent along with the event as well, for your own code to interpret.

When no settings are present, like with de scale-down event in this example, the settings
event will receive null for settings.

# custom events generator

Create your own event by implementing the `io.perfana.eventscheduler.api.EventFactory` interface.

Create your own event generator by implementing the `io.perfana.eventscheduler.api.EventGeneratorFactory` interface.

Add the `@generatorFactoryClass` and settings to the `customEvents` tag to have
an eventSchedule generated instead of an explicit list of timestamps and events.

```xml
<customEvents>
    @generatorFactoryClass=io.perfana.event.PerfanaEventGeneratorFactory
    events-file=${project.basedir}/src/test/resources/events.json
    foo=bar
</customEvents>
```

The class defined by `@generatorFactoryClass` should be available on the classpath.
The `foo=bar` is an example of properties for the event generator.
You can use multiple lines for multiple properties.

Properties that start with @-sign are so-called "meta" properties and
should properties with @-sign should preferably not be used as custom properties
inside the implementation class.   

## class loaders
If classes are not available on the default classpath of the Thread, you can provide your
own ClassLoader via `io.perfana.eventscheduler.api.EventSchedulerBuilder.of(EventSchedulerConfig, ClassLoader)`.
Useful when running with Gradle instead of Maven.

## event logging
Two convenience logger implementations are provided for the `io.perfana.eventscheduler.api.EventLogger` interface.

* `...log.EventLoggerStdOut.INSTANCE` logs to standard out (debug disabled)
* `...log.EventLoggerStdOut.INSTANCE_DEBUG` logs to standard out (debug enabled)

## kill switch

The keep-alive call can receive data from remote systems and decide to throw a `KillSwitchException` based
on that data. If a `KillSwitchException` is thrown and passed on to the user of the event-scheduler, running
tests can for instance be aborted based on the received data.

A `SchedulerExceptionHandler` implementation can be set on the `EventScheduler`. It contains a `kill` and `abort`
method that you can implement to handle these exceptions.

An example is that the analysis tool in use discovers too high response times and decides to kill the
running test.

## fat jar

If you create a fat jar that contains both the `event-scheduler` and one or more `test-event` plugins, such
as the `perfana-java-client`, you probably run into an issue that not all interface implementation defined in 
META-INF/services are registered correctly. This results in class loading issues, like the following:
" io.perfana.event.PerfanaEventFactory not registered via META-INF/services".

If you use the `maven-shade-plugin` you can solve this by adding the `org.apache.maven.plugins.shade.resource.ServicesResourceTransformer`
to the transformers section of the configuration.
