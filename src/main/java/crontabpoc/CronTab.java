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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.file.FileSystems;
import java.time.temporal.ChronoUnit;

/**
 * Demonstrates a "cron" workflow that executes activity periodically. Internally each iteration of
 * the workflow creates a new run using "continue as new" feature.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class CronTab {

  static final String TASK_QUEUE_CONTROLLER = "CronTabController";
  static final String TASK_QUEUE_CRONTABS = "CronTabJobs";

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker workerC = factory.newWorker(TASK_QUEUE_CONTROLLER);
    // Workflows are stateful. So you need a type to create instances.
    workerC.registerWorkflowImplementationTypes(CronTabControllerWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.

    try {
      workerC.registerActivitiesImplementations(
          new CronTabControllerWorkflowActivitiesImpl(FileSystems.getDefault().newWatchService()));
    } catch (Exception e) { // FIXME
    }
    // Start listening to the workflow and activity task queues.

    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker workerJ = factory.newWorker(TASK_QUEUE_CRONTABS);
    // Workflows are stateful. So you need a type to create instances.
    workerJ.registerWorkflowImplementationTypes(CronTabWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    workerJ.registerActivitiesImplementations(new CronTabWorkflowActivitiesImpl());
    // Start listening to the workflow and activity task queues.

    factory.start();

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
              .setWorkflowId("ControllerMain")
              .setTaskQueue(TASK_QUEUE_CONTROLLER)
              // Execution timeout limits total time. Cron will stop executing after this
              // timeout.
              .setWorkflowExecutionTimeout(
                  ChronoUnit.YEARS.getDuration().multipliedBy(3)) // for debugging turn
              // on to
              // stop
              // workflow after some time
              .setWorkflowRunTimeout(
                  ChronoUnit.YEARS.getDuration().multipliedBy(3)) // Run timeout limits
              // duration of
              // a
              // single workflow invocation.

              .build();

      CronTabControllerWorkflow workflow =
          client.newWorkflowStub(CronTabControllerWorkflow.class, workflowOptions);

      // FIXME: specify package for WorkflowExecution or import it
      // WorkflowExecution execution =
      WorkflowClient.start(workflow::run, "crontabs");

      // System.out.println("Started " + execution);
    } catch (io.temporal.client.WorkflowExecutionAlreadyStarted e) {
      System.out.println("Already running as " + e.getExecution());
    }
  }
}
