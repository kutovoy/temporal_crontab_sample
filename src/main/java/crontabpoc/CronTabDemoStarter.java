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

import static crontabpoc.CronTabControllerWorkflowImpl.PATH_TO_CRONTABS;
import static crontabpoc.CronTabControllerWorkflowImpl.TASK_QUEUE_CONTROLLER;
import static crontabpoc.CronTabWorkflowImpl.TASK_QUEUE_CRONTAB;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.file.FileSystems;

/**
 * Demonstrates a "cron" orchestrator CronTabControllerWorkflow that monitors crontab folder and
 * launches/stops CronTabWorkflows which are executing activities periodically. Internally each
 * iteration of the CronTabWorkflow creates a new run using "continue as new" feature.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class CronTabDemoStarter {

  public static void main(String[] args) {

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Worker C(ontroller) that listens on a task queue and hosts both workflow and activity
    // implementations.
    Worker workerC = factory.newWorker(TASK_QUEUE_CONTROLLER);
    // Workflows are stateful. So you need a type to create instances.
    workerC.registerWorkflowImplementationTypes(CronTabControllerWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.

    try {
      // We need to create file system watcher service which will be used by
      // CronTabControllerWorkflow activities and also pass a path to the crontabs folder
      workerC.registerActivitiesImplementations(
          new CronTabControllerWorkflowActivitiesImpl(
              PATH_TO_CRONTABS, FileSystems.getDefault().newWatchService()));
    } catch (Exception e) {
      System.out.println("Exception occured: " + e);
    }
    // Start listening to the workflow and activity task queues.

    // Worker J(obs) that listens on a task queue and hosts both workflow and activity
    // implementations.
    Worker workerJ = factory.newWorker(TASK_QUEUE_CRONTAB);
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
              // timeout. We can limit this crontab orchestrator to run for 100 years max for
              // example.
              // .setWorkflowExecutionTimeout(ChronoUnit.YEARS.getDuration().multipliedBy(100))
              // .setWorkflowRunTimeout(ChronoUnit.YEARS.getDuration().multipliedBy(3))
              // Run timeout limits
              // duration of
              // a
              // single workflow invocation.

              .build();

      CronTabControllerWorkflow workflow =
          client.newWorkflowStub(CronTabControllerWorkflow.class, workflowOptions);

      WorkflowExecution execution = WorkflowClient.start(workflow::run, PATH_TO_CRONTABS);

      System.out.println("Started " + execution);
    } catch (io.temporal.client.WorkflowExecutionAlreadyStarted e) {
      System.out.println("Already running as " + e.getExecution());
    }
  }
}
