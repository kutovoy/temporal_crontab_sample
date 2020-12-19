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

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * Demonstrates a "cron" workflow that executes activity periodically. Internally each iteration of
 * the workflow creates a new run using "continue as new" feature.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class CronTab {

  static final String TASK_QUEUE = "CronTabJob";
  static final String CRON_WORKFLOW_ID_PREFIX = "CronTab-";

  @WorkflowInterface
  public interface CronTabWorkflow {
    @WorkflowMethod
    void run(String method, String url, String failureURL);
  }

  @ActivityInterface
  public interface CronTabWorkflowActivities {
    int makeHTTPCall(String method, String URL);
  }

  /**
   * CronTabWorkflow implementation that calls {@link #makeHTTPCall(String)} once. The cron
   * functionality comes from {@link WorkflowOptions.Builder#setCronSchedule(String)} property.
   */
  public static class CronTabWorkflowImpl implements CronTabWorkflow {

    private final CronTabWorkflowActivities CronTabWorkflowActivities =
        Workflow.newActivityStub(
            CronTabWorkflowActivities.class,
            ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

    String mMethod;
    String mURL;
    String mFailureURL;

    @Override
    public void run(String method, String URL, String failureURL) {
      mMethod = method;
      mURL = URL;
      mFailureURL = failureURL;

      System.out.println(
          " ### WORKFLOW " + method + ", " + URL + ", " + failureURL + ": RUN START ### \n\n\n");
      int responseCode = CronTabWorkflowActivities.makeHTTPCall(mMethod, mURL);

      System.out.println("executed makeHTTPCall activity");
      System.out.println(
          " ### WORKFLOW " + method + ", " + URL + ", " + failureURL + ": RUN END ### \n\n\n");

      if (responseCode != 200) {
        System.out.println(
            " ### WORKFLOW "
                + method
                + ", "
                + URL
                + ", "
                + failureURL
                + ": FAILURE RUN START ### \n\n\n");
        CronTabWorkflowActivities.makeHTTPCall(mMethod, mFailureURL);

        System.out.println("executed an activity");
        System.out.println(
            " ### WORKFLOW "
                + method
                + ", "
                + URL
                + ", "
                + failureURL
                + ": FAILURE RUN END ### \n\n\n");
      }
    }
  }

  static class CronTabWorkflowActivitiesImpl implements CronTabWorkflowActivities {
    String mMethod;
    String mURL;

    @Override
    public int makeHTTPCall(String method, String URL) {
      mMethod = method;
      mURL = URL;
      System.out.println(" ### ACTIVITY " + method + ", " + URL + " EXECUTE START ### \n\n\n");
      System.out.println("From " + Activity.getExecutionContext().getInfo().getWorkflowId());

      int status = 500;
      try {

        URL url = new URL(mURL);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod(mMethod);
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        status = con.getResponseCode();
      } catch (IOException e) {
        System.out.println(" exception! ");
      }

      System.out.println(" ### ACTIVITY " + method + ", " + URL + " EXECUTE END ### \n\n\n");

      return status;
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(CronTabWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new CronTabWorkflowActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    File folder = new File("crontabs");
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
            entryIndex++;

            java.util.HashMap<String, Object> map = (java.util.HashMap<String, Object>) el;

            System.out.println(
                "parsing cron file entry #" + entryIndex + ", entry url " + map.get("url"));

            Boolean enabled = true;

	    if ( map.containsKey("enabled") )
		enabled = (Boolean) map.get("enabled");

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
                      .setWorkflowId(
                          CRON_WORKFLOW_ID_PREFIX
                              + file.getName()
                              + "."
                              + method
                              + "."
                              + url) // TODO: think about how to ensure that the ID is unique in
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
              //  WorkflowOptions.newBuilder().setCronSchedule("@every 2s").build();

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
