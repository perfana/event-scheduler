# Release notes Perfana Event Scheduler

## v4.0.0 - april 2023 
* simplified TestContext usage: only available at `EventSchedulerContext` and not at `Event` level
* from java 8 to java 11

## v4.0.1 - november 2023
* default value of continueOnEventCheckFailure is now false
* improved documentation
* unique value of default test run ids

## v4.0.3 - january 2024
* fixed issue giving warnings for events of same class: should be warning for events with same name
* improved disabled event handling: do no use disabled events, only report them
* note: do not use `v4.0.2`, it has null pointer issue

## v4.0.4 - april 2024
* new immutable test contexts can be created using the `withX` methods

## v4.0.5 - januari 2025
* improved executor handling on shutdown
* improved logging
* code cleanup and dependencies update
