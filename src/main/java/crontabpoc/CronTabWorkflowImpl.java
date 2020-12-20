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

  private static Logger logger = Workflow.getLogger(CronTabWorkflowImpl.class);

  private final CronTabWorkflowActivities CronTabWorkflowActivities =
      Workflow.newActivityStub(
          CronTabWorkflowActivities.class,
          ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(300)).build());

  String mMethod;
  String mURL;
  String mFailureURL;

  @Override
  public void test(String data) {
    System.out.println("\n\n\n\nYES - test method received " + data + "!\n\n\n\n");
  }

  @Override
  public String getURL() {
    return mURL;
  }

  @Override
  public void run(String method, String URL, String failureURL) {
    mMethod = method;
    mURL = URL;
    mFailureURL = failureURL;

    // TODO: check if our YAML file exists
    // TODO: check if our YAML file didn't change [verify checksum]

    logger.info(
        " ### WORKFLOW " + method + ", " + URL + ", " + failureURL + ": RUN START ### \n\n\n");
    int responseCode = CronTabWorkflowActivities.makeHTTPCall(mMethod, mURL);

    logger.info("executed makeHTTPCall activity");
    logger.info(
        " ### WORKFLOW " + method + ", " + URL + ", " + failureURL + ": RUN END ### \n\n\n");

    if (responseCode != 200) {
      logger.info(
          " ### WORKFLOW "
              + method
              + ", "
              + URL
              + ", "
              + failureURL
              + ": FAILURE RUN START ### \n\n\n");
      CronTabWorkflowActivities.makeHTTPCall(mMethod, mFailureURL);

      logger.info("executed an activity");
      logger.info(
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
