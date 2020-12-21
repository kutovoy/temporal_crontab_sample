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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Activities class which implements all activities for CronTabWorkflow */
class CronTabWorkflowActivitiesImpl implements CronTabWorkflowActivities {

  // Activity to ping a URL
  @Override
  public int makeHTTPCall(String method, String URL) {
    System.out.println(
        "\n\n ### CronTabWorkflowActivitiesImpl["
            + Activity.getExecutionContext().getInfo().getWorkflowId()
            + "].makeHTTPCall("
            + method
            + ", "
            + URL
            + ") EXECUTION STARTED ### \n\n");

    int status =
        500; // in case if any exception/timeout occurs we will return 500 Response Code to trigger
    // FailureURL

    try {

      URL url = new URL(URL);

      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      con.setRequestMethod(method);
      con.setConnectTimeout(
          5000); // Fine tune these keeping in mind what timeouts/retries are set on the activities
      // and on the CronTabWorkflow itself
      con.setReadTimeout(
          5000); // Fine tune these keeping in mind what timeouts/retries are set on the activities
      // and on the CronTabWorkflow itself

      status = con.getResponseCode();
    } catch (IOException e) {
      System.out.println("Exception: " + e);
    }

    System.out.println(
        "\n\n ### CronTabWorkflowActivitiesImpl["
            + Activity.getExecutionContext().getInfo().getWorkflowId()
            + "].makeHTTPCall("
            + method
            + ", "
            + URL
            + ") EXECUTED, RESPONCE CODE = "
            + status
            + " ### \n\n");

    return status; // if we return non 200 Response Code then FailureURL will be triggered
  }
}
