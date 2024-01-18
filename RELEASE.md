# Release notes Perfana Event Scheduler

## v4.0.0 - april 2023 
* simplified TestContext usage: only available at `EventSchedulerContext` and not at `Event` level
* from java 8 to java 11

## v4.0.1 - november 2023
* default value of continueOnEventCheckFailure is now false
* improved documentation
* unique value of default test run ids

## v4.0.2 - january 2024
* fixed issue giving warnings for events of same class: should be warning for events with same name
* improved disabled event handling: do no use disabled events, only report them