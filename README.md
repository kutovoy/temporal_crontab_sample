# Basic HTTP ping-like Crontab Sample

Demonstrates a way on how to implement a basic HTTP ping-like Crontab schedules. Can be easily extended.

The sample has folder crontabs which has YAML crontab samples. Folder with crontabs is monitored for file changes.

### Launch sample

Start the worker and the CronTabControllerWorkflow:

    gradle -q execute -PmainClass=crontabpoc.CronTab

Check the output of the worker window if you want to read some debugging info.

### TODO

See [TODO.md](TODO.md) for more details.
