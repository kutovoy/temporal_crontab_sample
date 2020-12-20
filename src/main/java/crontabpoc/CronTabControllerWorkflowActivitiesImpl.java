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

import static java.nio.file.StandardWatchEventKinds.*;

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

class CronTabControllerWorkflowActivitiesImpl implements CronTabControllerWorkflowActivities {
  static final String TASK_QUEUE = "CronTabJobs";

  private WorkflowServiceStubs service;
  private WorkflowClient client;

  // CronTab directory watchers variables
  private Path dir; // Path to crontabs directory
  private WatchService watcher;
  private WatchKey key;

  public CronTabControllerWorkflowActivitiesImpl(WatchService watcher) {
    service = WorkflowServiceStubs.newInstance();
    client = WorkflowClient.newInstance(service);

    this.watcher = watcher;
    dir = Paths.get("crontabs"); // fixme: hardcoded path
    try {
      key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    } catch (IOException x) {
      System.err.println(x);
      key = null;
    }
  }

  @Override
  public void stepScanForChanges() {
    CronTabWorkflow workflow = client.newWorkflowStub(CronTabWorkflow.class, "PingWorkflow.yml");

    // Returns the result after waiting for the Workflow to complete.
    //      String result = workflow.getURL();
    //    workflow.test("starting");
    try {
      key = watcher.take();
    } catch (InterruptedException x) {
      return;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      WatchEvent.Kind<?> kind = event.kind();

      // This key is registered only
      // for ENTRY_CREATE events,
      // but an OVERFLOW event can
      // occur regardless if events
      // are lost or discarded.
      if (kind == OVERFLOW) {
        continue;
      }

      // The filename is the
      // context of the event.
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path filename = ev.context();

      // Verify that the new
      //  file is a text file.
      //      try {
      // Resolve the filename against the directory.
      // If the filename is "test" and the directory is "foo",
      // the resolved name is "test/foo".
      //        Path child = dir.resolve(filename);
      //        if (!Files.probeContentType(child).equals("text/plain")) {
      //          System.err.format("New file '%s'" + " is not a plain text file.%n", filename);
      //          continue;
      //        }
      //      } catch (IOException x) {
      //        System.err.println(x);
      //        continue;
      //      }

      // Email the file to the
      //  specified email alias.
      //      System.out.format("Emailing file %s%n", filename);
      workflow.test("file " + filename + " changed!");

      // Details left to reader....
    }

    // Reset the key -- this step is critical if you want to
    // receive further watch events.  If the key is no longer valid,
    // the directory is inaccessible so exit the loop.
    boolean valid = key.reset();
    if (!valid) {
      return; // FIXME: do something in case if watcher failed
    }
  }

  @Override
  public void initialScanCrontabs(String cronTabsFolder) {

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    File folder = new File(cronTabsFolder);
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
      if (!file.isFile() || !file.getName().contains(".yml")) continue;

      try {
        java.io.InputStream in = new java.io.FileInputStream(file);

        org.yaml.snakeyaml.Yaml yamlParser = new org.yaml.snakeyaml.Yaml();
        Iterable<Object> itr = yamlParser.loadAll(in);

        for (Object o : itr) {
          System.out.println("Loaded file " + file.getName());
          System.out.println(o);

          java.util.ArrayList list = (java.util.ArrayList) o;

          int entryIndex = -1;
          for (Object el : list) {

            if (entryIndex
                >= 0) // FIXME: task was clarified : only one crontab entry per file but in an array
              // format. TODO: refactor
              break;

            entryIndex++;

            java.util.HashMap<String, Object> map = (java.util.HashMap<String, Object>) el;

            System.out.println(
                "parsing cron file entry #" + entryIndex + ", entry url " + map.get("url"));

            Boolean enabled = true;

            if (map.containsKey("enabled")) enabled = (Boolean) map.get("enabled");

            System.out.println("enabled: " + enabled);

            if (!enabled) {
              System.out.println("skipping disabled crontab entry");

              continue;
            }

            String type = (String) map.get("type");

            if (!type.equals("HTTP")) {
              System.out.println(
                  "skipping unsupported type "
                      + map.get("type")
                      + " crontab entry "
                      + file.getName());

              continue;
            }

            String url = (String) map.get("url");
            String method = (String) map.get("method");
            String schedule = (String) map.get("schedule");

            // Cut off unused "command" part with ? if present
            if (schedule.indexOf("?") != -1)
              schedule = schedule.substring(0, schedule.indexOf("?"));

            String failureURL = null;

            if (map.containsKey("failureURL")) failureURL = (String) map.get("failureURL");

            System.out.println(
                "ready to queue workflow!"
                    + url
                    + " "
                    + method
                    + " "
                    + schedule
                    + ", "
                    + failureURL);

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
                      // case if multiple cron actions will use same URL for some
                      // reason
                      .setTaskQueue(TASK_QUEUE)
                      .setCronSchedule(schedule)
                      // Execution timeout limits total time. Cron will stop executing after this
                      // timeout.
                      // .setWorkflowExecutionTimeout(Duration.ofMinutes(5)) // for debugging turn
                      // on to
                      // stop
                      // workflow after some time
                      // .setWorkflowRunTimeout(Duration.ofMinutes(10)) // Run timeout limits
                      // duration of
                      // a
                      // single workflow invocation.
                      .build();

              CronTabWorkflow workflow =
                  client.newWorkflowStub(CronTabWorkflow.class, workflowOptions);

              // FIXME: specify package for WorkflowExecution or import it
              // WorkflowExecution execution =
              WorkflowClient.start(workflow::run, method, url, failureURL);

              // System.out.println("Started " + execution);
            } catch (io.temporal.client.WorkflowExecutionAlreadyStarted e) {
              System.out.println("Already running as " + e.getExecution());
            }
          }
        }

      } catch (IOException e) {
        System.out.println("Failed to read file " + file.getName() + ", exception: " + e);
      }
    }
  }
}
