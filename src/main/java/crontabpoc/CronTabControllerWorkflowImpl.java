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

  private static Logger logger = Workflow.getLogger(CronTabControllerWorkflowImpl.class);

  private final CronTabControllerWorkflowActivities CronTabControllerWorkflowActivities =
      Workflow.newActivityStub(
          CronTabControllerWorkflowActivities.class,
          ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(300)).build());

  String mCrontabsFolder;

  @Override
  public void run(String crontabsFolder) {

    mCrontabsFolder = crontabsFolder;

    logger.info("executing initial crontabs scan");

    CronTabControllerWorkflowActivities.initialScanCrontabs();

    while (true) {
      // do work

      // while (!"Bye".equals(greeting)) {
      // Workflow.await(() -> !Objects.equals(greeting, oldGreeting));

      logger.info("!!!do work step!!! 1");

      CronTabControllerWorkflowActivities.stepScanForChanges();

      Workflow.sleep(Duration.ofSeconds(5));
    }
  }
}
