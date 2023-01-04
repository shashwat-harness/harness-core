/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.queue;

import io.harness.beans.execution.CIInitTaskArgs;
import io.harness.ci.enforcement.CIBuildEnforcer;
import io.harness.ci.states.V1.InitializeTaskStepV2;
import io.harness.hsqs.client.model.DequeueResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIInitTaskMessageProcessorImpl implements CIInitTaskMessageProcessor {
  @Inject InitializeTaskStepV2 initializeTaskStepV2;
  @Inject CIBuildEnforcer buildEnforcer;
  @Inject @Named("ciInitTaskExecutor") ExecutorService initTaskExecutor;
  @Inject AsyncWaitEngine asyncWaitEngine;

  @Override
  public ProcessMessageResponse processMessage(DequeueResponse dequeueResponse) {
    ProcessMessageResponse.ProcessMessageResponseBuilder builder = ProcessMessageResponse.builder();
    try {
      String payload = dequeueResponse.getPayload();
      CIInitTaskArgs ciInitTaskArgs = RecastOrchestrationUtils.fromJson(payload, CIInitTaskArgs.class);
      Ambiance ambiance = ciInitTaskArgs.getAmbiance();
      builder.accountId(AmbianceUtils.getAccountId(ambiance));
      if (!buildEnforcer.checkBuildEnforcement(AmbianceUtils.getAccountId(ambiance))) {
        log.info(String.format("skipping execution for account id: %s because of concurrency enforcement failure",
            AmbianceUtils.getAccountId(ambiance)));
        return builder.success(false).build();
      }
      initTaskExecutor.submit(() -> {
        String taskId = initializeTaskStepV2.executeBuild(ambiance, ciInitTaskArgs.getStepElementParameters());
        CIInitDelegateTaskStatusNotifier ciInitDelegateTaskStatusNotifier =
            CIInitDelegateTaskStatusNotifier.builder().waitId(ciInitTaskArgs.getCallbackId()).build();
        asyncWaitEngine.waitForAllOn(ciInitDelegateTaskStatusNotifier, null, Arrays.asList(taskId), 0);
      });
      return builder.success(true).build();
    } catch (Exception ex) {
      log.info("ci init task processing failed", ex);
      return builder.success(false).build();
    }
  }
}