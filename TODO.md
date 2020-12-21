### WIP

1. add unit tests

### Optional

1. re-use same CronTabWorkflow when we receive "crontab file modified" event rather than re-create a new workflow - requirement for the "fully implement deterministic key concept". Scheduled workflows currently run as new on each pass. With crontabs users might not need state recovery in most cases.

### Long term / big improvements:

1. fully implement deterministic key concept: implement fully deterministic model for CronTabWorkflow by let's say passing created/updated crontab content via a signal into CronTabWorkflow so  it can recover it's state in case of retries. Scheduled workflows currently run as new on each pass.
2. fully support distributed key concept: support some code version control system and integrate via some hooks - currently this sample is not fully distributed - sample expects that crontab folder exists on the same host with the worker executing CronTabControllerWorkflow.

### Minor improvements

1. Determine and set all required workflows/activities timeouts and retries. For example hibernating local Docker with Temporal (closing laptop lid) will lead to : "Activity task timedOut. Caused By: activity timeout - RetryPolicyNotSet"
2. Potentially optimize polling of events out of WatcherService in CronTabControllerWorkflow activities should this become an issue. Currently polling is done every second. This task will become obsolete once a code version control system integration will be done.
