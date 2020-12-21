# Basic HTTP ping-like Crontab Sample

Demonstrates a way on how to implement a basic HTTP ping-like Crontab schedules. Can be easily extended.

The sample has folder crontabs which has YAML crontab samples. Folder with crontabs is monitored for file changes.

### Launch sample

Install local Docker Desktop (for development only, see [Project Page](https://www.docker.com/products/docker-desktop) for more informations) or ensure you have other means on how to run "docker-compose up" command to launch local dev temporal server.

Install local temporal platform. See [Quick Install](https://docs.temporal.io/docs/server-quick-install) for instructions.

Install Gradle. See [Install Instructions](https://gradle.org/install/) for more details.

Either just start required workers and the CronTabControllerWorkflow locally on one host (for development)

    gradle -q execute -PmainClass=crontabpoc.CronTabDemoStarter

Or you can start workers on any number of host greater than 0 (ensure that you will specify where is temporal service running) with:

    gradle -q execute -PmainClass=crontabpoc.CronTabWorkersStarter

And launch CronTabControllerWorkflow locally where temporal service is running (or specify where is it running) with:

    gradle -q execute -PmainClass=crontabpoc.CronTabControllerWorkflowStarter


Check the output of the worker window if you want to read some debugging info or look for the workflow details/history events in the GUI - click [here](http://localhost:8088/) once Temporal server and worker is running.

### PENDING: Terminating scheduled workflow is NOT implemented

Example is currently missing implementation of the cron scheduled CronTabWorkflow termination so the "file changed" and "file deleted" events are being triggered but they partly fail until termination code is missing.

### TODO

See [TODO.md](TODO.md) for more details.
