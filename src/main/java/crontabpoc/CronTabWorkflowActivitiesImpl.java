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

class CronTabWorkflowActivitiesImpl implements CronTabWorkflowActivities {

  @Override
  public int makeHTTPCall(String method, String URL) {
    System.out.println(" ### ACTIVITY " + method + ", " + URL + " EXECUTE START ### \n\n\n");
    System.out.println("From " + Activity.getExecutionContext().getInfo().getWorkflowId());

    int status = 500;
    try {

      URL url = new URL(URL);

      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      con.setRequestMethod(method);
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
