# Basic HTTP ping-like Crontab Sample

Demonstrates a way on how to implement a basic HTTP ping-like Crontab schedules. Can be easily extended.

The sample has folder crontabs which has YAML crontab samples. Folder with crontabs is monitored for file changes.

### Launch sample

Install local Docker Desktop (for development, see [Project Page](https://www.docker.com/products/docker-desktop) for more informations) or ensure you have other means on how to run "docker-compose up" command to launch local dev temporal server.

Install local temporal platform. See [Quick Install](https://docs.temporal.io/docs/server-quick-install) for instructions.

Install Gradle. See [Install Instructions](https://gradle.org/install/) for more details.

Start the worker and the CronTabControllerWorkflow:

    gradle -q execute -PmainClass=crontabpoc.CronTab

Check the output of the worker window if you want to read some debugging info.

### TODO

See [TODO.md](TODO.md) for more details.
