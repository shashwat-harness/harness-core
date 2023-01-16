/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.steps.policy.step.outcome.PolicySetOutcome")
public class PolicySetOutcome {
  String status;
  String identifier;
  String name;
  Map<String, PolicyOutcome> policyDetails;
}