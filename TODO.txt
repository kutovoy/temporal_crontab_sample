WIP:

1. finish implementation of file system watcher
2. cleanup & document code

optional:

1. separate worker into a separate class
2. re-use same workflow on a crontab file change event rather than re-create a new workflow - requirement for the "fully implement deterministic key concept" long term todo

long term / big improvements:

1. fully implement deterministic key concept: implement fully deterministic model for CronTabWorkflow by let's say passing created/updated crontab content via a signal into CronTabWorkflow so it can recover it's state, ensure p
2. fully support distributed key concept: support some code version control system and integrate via some hooks - currently this sample is not distributed - sample expects that crontab folder exists on the host with the worker executing workflows
