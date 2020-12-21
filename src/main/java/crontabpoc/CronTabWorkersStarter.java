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

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.file.FileSystems;

/**
 * Launches one worker for CronTabControllerWorkflow and one worker for CronTabWorkflow. Does not
 * launch CronTabControllerWorkflow. Execute CronTabControllerWorkflowStarter on same/another host.
 *
 * <p>Requires a local instance of Temporal server to be running.
 */
public class CronTabWorkersStarter {

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
  }
}
