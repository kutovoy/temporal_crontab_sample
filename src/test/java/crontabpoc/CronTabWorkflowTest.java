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

import static crontabpoc.CronTabWorkflowImpl.TASK_QUEUE_CRONTAB;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;

// Unit test for {@link CronTabWorkflow}. Doesn't use an external Temporal service.
public class CronTabWorkflowTest {

  @Rule public Timeout globalTimeout = Timeout.seconds(2);

  // Prints a history of the workflow under test in case of a test failure.
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE_CRONTAB);
    worker.registerWorkflowImplementationTypes(CronTabWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  // Simple makeHTTPCall activity test
  @Test
  public void testMockedActivity() {
    CronTabWorkflowActivities activities = mock(CronTabWorkflowActivities.class);
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    String workflowId = "filename.yml";

    // Get a workflow stub using the same task queue the worker uses.
    CronTabWorkflow workflow =
        client.newWorkflowStub(
            CronTabWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE_CRONTAB)
                .setWorkflowId(workflowId)
                .build());
    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution =
        WorkflowClient.start(
            workflow::run,
            "GET",
            "http://www.example.com",
            "http://www.example.com?failed-call-alert");

    assertEquals(workflowId, execution.getWorkflowId());

    // Use TestWorkflowEnvironment.sleep to execute the unit test without really sleeping.
    testEnv.sleep(Duration.ofMinutes(1));
    verify(activities, times(1))
        .makeHTTPCall(
            "GET",
            "http://www.example.com"); // ensure that at least ping was attempted to the URL with
    // the right method
  }
}
