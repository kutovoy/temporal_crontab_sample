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
import io.temporal.client.WorkflowOptions;
import io.temporal.workflow.Workflow;
import java.net.URL;
import java.time.Duration;
import org.slf4j.Logger;

/**
 * CronTabWorkflow implementation that calls {@link #makeHTTPCall(String)} once. The cron
 * functionality comes from {@link WorkflowOptions.Builder#setCronSchedule(String)} property.
 */
public class CronTabWorkflowImpl implements CronTabWorkflow {
  // Temporal queue name for the CronTabWorkflow
  static final String TASK_QUEUE_CRONTAB = "CronTabJobs";

  private static Logger logger = Workflow.getLogger(CronTabWorkflowImpl.class);

  // Fine tune activities timeouts and retries as needed
  private final CronTabWorkflowActivities CronTabWorkflowActivities =
      Workflow.newActivityStub(
          CronTabWorkflowActivities.class,
          ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(300)).build());

  String mMethod; // HTTP method to ping URLs
  String mURL; // URL to ping on a schedule
  String mFailureURL; // URL to ping if we are unable to reach mURL
  Boolean mCrontabDeleted =
      false; // Will be set to true if we need to terminate our execution. Extra check

  // CronTabControllerWorkflow will notify us via this method when our corresponding crontab file
  // will be deleted. we should not make any url pings should this happen and terminate.
  @Override
  public void crontabDeletedEvent() {
    mCrontabDeleted = true;

    System.out.println("\n\ncrontabDeletedEvent RECEIVED!\n\n");
  }

  // This main workflow method is executed as new on a schedule
  @Override
  public void run(String method, String URL, String failureURL) {

    // check if crontab received any notification that it was deleted
    if (mCrontabDeleted) {
      // stop workflow if we were notified that our crontab was deleted

      // FIXME: how to terminate scheduled workflow?
      // FIXME: how to terminate scheduled workflow?
      // FIXME: how to terminate scheduled workflow?
      // FIXME: how to terminate scheduled workflow?

      // throw new Exception("Crontab file was deleted. Terminating workflow");
      // Thread.currentThread().interrupt(); // this will simply execute workflow as new

      return;
    }

    mMethod = method;
    mURL = URL;
    mFailureURL = failureURL;

    // next 2 TODO can't be done if worker is not running on the same host with cron tabs folder.
    // temporal is a distributed system.
    // TODO: potentially do extra check if our YAML file exists
    // TODO: potentially do extra check to see if our YAML file didn't change [save and verify file
    // checksum of some kind]

    int responseCode = CronTabWorkflowActivities.makeHTTPCall(mMethod, mURL);

    logger.info("executed makeHTTPCall activity on URL");

    if (responseCode != 200) {
      CronTabWorkflowActivities.makeHTTPCall(mMethod, mFailureURL);

      logger.info("executed makeHTTPCall activity on failureURL");
    }
  }
}
