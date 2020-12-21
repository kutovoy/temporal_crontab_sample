/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package crontabpoc;

import static crontabpoc.CronTabWorkflowImpl.TASK_QUEUE_CRONTAB;
import static java.nio.file.StandardWatchEventKinds.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/** Activities class which implements all activities for CronTabControllerWorkflow */
class CronTabControllerWorkflowActivitiesImpl implements CronTabControllerWorkflowActivities {
  // We use these objects to launch/access other workflows
  private WorkflowServiceStubs service;
  private WorkflowClient client;

  // CronTab directory watchers variables
  private Path dir; // Path to crontabs directory
  private WatchService watcher;
  private WatchKey watcherKey;

  // Constructor
  public CronTabControllerWorkflowActivitiesImpl(String crontabsFolderPath, WatchService watcher) {
    // Setup workflow service and client objects, they should not be created more than once
    service = WorkflowServiceStubs.newInstance();
    client = WorkflowClient.newInstance(service);

    // save Java Watch Service which is monitoring folder with crontabs for file changes
    this.watcher = watcher;

    dir = Paths.get(crontabsFolderPath);

    // register what file change events we want to monitor
    try {
      watcherKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    } catch (IOException x) {
      // TODO: take some action if we failed to register for the folder watcher service
      System.err.println(x);
      watcherKey = null;
    }
  }

  // Activity where we will poll WatchService for the new folder changes events and take some
  // actions when they happens
  @Override
  public void stepScanForChanges() {

    try {
      watcherKey = watcher.take();
    } catch (InterruptedException x) {
      return;
    }

    // Get all new folder file change events
    for (WatchEvent<?> event : watcherKey.pollEvents()) {
      WatchEvent.Kind<?> kind = event.kind();

      // This key is registered only
      // for ENTRY_CREATE events,
      // but an OVERFLOW event can
      // occur regardless if events
      // are lost or discarded.
      if (kind == OVERFLOW) {
        continue;
      }

      // The filename is the context of the event.
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path fileNamePath = ev.context();
      String fileName = fileNamePath.toString();

      // Skip any files which are not .yml crontabs
      if (!fileName.contains(".yml")) {
        continue;
      }

      // start new CronTabWorkflow when new crontab file is found
      if (kind == ENTRY_CREATE) {
        System.out.println("\n\nENTRY_CREATE: " + fileNamePath + "\n\n");

        launchNewCrontabWorkflowFromFileName(fileName);

        continue;
      }

      // stop CronTabWorkflow when crontab file was deleted
      if (kind == ENTRY_DELETE) {
        System.out.println("\n\nENTRY_DELETE: " + fileNamePath + "\n\n");

        stopCrontabWorkflowFromFileName(fileName);

        continue;
      }

      // stop CronTabWorkflow when crontab file was modified
      // start new CronTabWorkflow which will pull new settings from the modified crontab file
      if (kind == ENTRY_MODIFY) {
        System.out.println("\n\nENTRY_MODIFY: " + fileNamePath + "\n\n");

        stopCrontabWorkflowFromFileName(fileName);

        launchNewCrontabWorkflowFromFileName(fileName);

        continue;
      }
    }

    // Reset the watcherKey -- this step is critical if you want to
    // receive further watch events.  If the key is no longer valid,
    // the directory is inaccessible so exit the loop.
    boolean valid = watcherKey.reset();
    if (!valid) {
      return; // TODO: do something in case if watcher failed
    }
  }

  // Starts new CronTabWorkflow from the crontab filename
  @Override
  public void launchNewCrontabWorkflowFromFileName(String fileName) {
    System.out.println("\nlaunchNewCrontabWorkflowFromFileName(" + fileName + ")\n");

    File file = new File(dir + "/" + fileName);

    try {
      java.io.InputStream in = new java.io.FileInputStream(file);

      // Lets parse yml file
      org.yaml.snakeyaml.Yaml yamlParser = new org.yaml.snakeyaml.Yaml();
      Iterable<Object> itr = yamlParser.loadAll(in);

      for (Object o : itr) {

        java.util.ArrayList list = (java.util.ArrayList) o;

        int entryIndex = -1;
        for (Object el : list) {

          /**
           * TODO: excercise task was clarified : only one crontab entry per file is expected now
           * but in an array format. so we only process 1st element. We need crontab workflows to
           * match .yml file name. A better way to refactor would be to not use arrays in the
           * crontab file definition. Then we can just parse whole file into a proper Java object
           * which we describe as a class. Part of the exercise definition.
           */
          if (entryIndex >= 0)
            break; // Do not process more than one crontab entry per crontab file.

          entryIndex++;

          java.util.HashMap<String, Object> map = (java.util.HashMap<String, Object>) el;

          Boolean enabled = true;

          if (map.containsKey("enabled")) enabled = (Boolean) map.get("enabled");

          if (!enabled) {
            System.out.println("skipping disabled crontab file");

            continue;
          }

          String type = (String) map.get("type");

          if (!type.equals("HTTP")) {
            System.out.println("skipping unsupported type " + map.get("type"));

            continue;
          }

          String url = (String) map.get("url");
          String method = (String) map.get("method");
          String schedule = (String) map.get("schedule");

          // Cut off unused "command" part with ? if present. Part of the exercise definition.
          if (schedule.indexOf("?") != -1) schedule = schedule.substring(0, schedule.indexOf("?"));

          String failureURL = null;

          if (map.containsKey("failureURL")) failureURL = (String) map.get("failureURL");

          try {
            /*
             */
            // Sets the cron schedule using the WorkflowOptions.
            // The cron format is parsed by "https://github.com/robfig/cron" library.
            // Besides the standard "* * * * *" format it supports @every and other extensions.
            // Note that unit testing framework doesn't support the extensions.
            // Use single fixed ID to ensure that there is at most one instance running. To run
            // multiple
            // instances set different IDs.

            WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                    .setWorkflowId(file.getName())
                    .setTaskQueue(TASK_QUEUE_CRONTAB)
                    .setCronSchedule(schedule)
                    // Execution timeout limits total time. Cron will stop executing after this
                    // timeout.
                    // .setWorkflowExecutionTimeout(Duration.ofMinutes(5))
                    // .setWorkflowRunTimeout(Duration.ofMinutes(10))// Run timeout limits
                    // duration of
                    // a
                    // single workflow invocation.
                    .build();

            CronTabWorkflow workflow =
                client.newWorkflowStub(CronTabWorkflow.class, workflowOptions);

            // Async launch the CronTabWorkflow
            WorkflowExecution execution =
                WorkflowClient.start(workflow::run, method, url, failureURL);

            System.out.println("Started " + execution);
          } catch (io.temporal.client.WorkflowExecutionAlreadyStarted e) {
            System.out.println("Already running as " + e.getExecution());
          }
        }
      }

    } catch (IOException e) {
      System.out.println("Failed to read file " + file.getName() + ", exception: " + e);
    }
  }

  // Activity to stop a CronTabWorkflow
  @Override
  public void stopCrontabWorkflowFromFileName(String fileName) {
    System.out.println("\nstopCrontabWorkflowFromFileName(" + fileName + ")\n");
    CronTabWorkflow workflow = client.newWorkflowStub(CronTabWorkflow.class, fileName);

    // Notify the workflow that it was stopped as an extra safety measure
    workflow.crontabDeletedEvent();

    // FIXME: how to terminate scheduled workflow?
    // FIXME: how to terminate scheduled workflow?
    // FIXME: how to terminate scheduled workflow?
    // FIXME: how to terminate scheduled workflow?
  }

  // Launch initial CronTabWorkflow during statup of CronTabControllerWorkflow - parse all .yml
  // files in crontab folder
  @Override
  public void initialScanCrontabs() {
    System.out.println("initialScanCrontabs() executed");

    File folder = new File(dir.toString());
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
      if (!file.isFile() || !file.getName().contains(".yml")) continue;

      launchNewCrontabWorkflowFromFileName(file.getName());
    }
  }
}
