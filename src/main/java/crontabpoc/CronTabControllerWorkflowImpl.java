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

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * CronTabWorkflow implementation that calls {@link #makeHTTPCall(String)} once. The cron
 * functionality comes from {@link WorkflowOptions.Builder#setCronSchedule(String)} property.
 */
public class CronTabControllerWorkflowImpl implements CronTabControllerWorkflow {
  // Temporal queue name for the CronTabControllerWorkflow
  static final String TASK_QUEUE_CONTROLLER = "CronTabController";
  static final String PATH_TO_CRONTABS = "crontabs";

  private static Logger logger = Workflow.getLogger(CronTabControllerWorkflowImpl.class);

  // Fine tune activities timeouts and retries as needed
  private final CronTabControllerWorkflowActivities CronTabControllerWorkflowActivities =
      Workflow.newActivityStub(
          CronTabControllerWorkflowActivities.class,
          ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(300)).build());

  // Path to the crontabs folder
  String mCrontabsFolder;

  @Override
  public void run(String crontabsFolder) {

    mCrontabsFolder = crontabsFolder;

    logger.info("executing initial crontabs scan");

    // Initially load and parse all .yml files in the crontabs folder
    CronTabControllerWorkflowActivities.initialScanCrontabs();

    while (true) {
      logger.info("scanning for crontabs folder changes");

      CronTabControllerWorkflowActivities.stepScanForChanges();

      Workflow.sleep(
          Duration.ofSeconds(1)); // Fine tune desired timeout of pulling file changes events here
    }
  }
}
