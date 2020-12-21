### WIP

1. add unit tests

### Optional

1. re-use same workflow on a crontab file modified event rather than re-create a new workflow - requirement for the "fully implement deterministic key concept" long term todo

### Long term / big improvements:

1. fully implement deterministic key concept: implement fully deterministic model for CronTabWorkflow by let's say passing created/updated crontab content via a signal into CronTabWorkflow so it can recover it's state. Scheduled workflows currently run as new on each pass.
2. fully support distributed key concept: support some code version control system and integrate via some hooks - currently this sample is not distributed - sample expects that crontab folder exists on the same host with the worker executing CronTabControllerWorkflow.

### Minor improvements

1. Determine and set all required workflows/activities timeouts and retries. Hibernating local Docker with Temporal will lead to : "Activity task timedOut. Caused By: activity timeout - RetryPolicyNotSet"
