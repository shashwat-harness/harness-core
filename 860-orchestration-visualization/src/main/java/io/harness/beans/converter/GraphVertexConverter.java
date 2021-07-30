package io.harness.beans.converter;

import io.harness.DelegateInfoHelper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.data.structure.CollectionUtils;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.execution.NodeExecution;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.data.stepparameters.PmsStepParameters;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.utils.OrchestrationMapBackwardCompatibilityUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class GraphVertexConverter {
  @Inject DelegateInfoHelper delegateInfoHelper;

  public GraphVertex convertFrom(NodeExecution nodeExecution) {
    List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParamsList =
        delegateInfoHelper.getDelegateInformationForGivenTask(nodeExecution.getExecutableResponses(),
            nodeExecution.getMode(), AmbianceUtils.getAccountId(nodeExecution.getAmbiance()));

    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .ambiance(nodeExecution.getAmbiance())
        .planNodeId(nodeExecution.getNode().getUuid())
        .identifier(nodeExecution.getNode().getIdentifier())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .skipInfo(nodeExecution.getSkipInfo())
        .nodeRunInfo(nodeExecution.getNodeRunInfo())
        .stepParameters(obtainStepParameters(nodeExecution.getResolvedStepInputs()))
        .mode(nodeExecution.getMode())
        .executableResponses(CollectionUtils.emptyIfNull(nodeExecution.getExecutableResponses()))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipType())
        .unitProgresses(nodeExecution.getUnitProgresses())
        .progressData(
            OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(nodeExecution.getProgressData()))
        .graphDelegateSelectionLogParams(graphDelegateSelectionLogParamsList)
        .build();
  }

  public GraphVertex convertFrom(
      NodeExecution nodeExecution, Map<String, PmsOutcome> outcomes, Map<String, OrchestrationMap> stepDetails) {
    List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParamsList =
        delegateInfoHelper.getDelegateInformationForGivenTask(nodeExecution.getExecutableResponses(),
            nodeExecution.getMode(), AmbianceUtils.getAccountId(nodeExecution.getAmbiance()));
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .ambiance(nodeExecution.getAmbiance())
        .planNodeId(nodeExecution.getNode().getUuid())
        .identifier(nodeExecution.getNode().getIdentifier())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(obtainStepParameters(nodeExecution.getResolvedStepInputs()))
        .skipInfo(nodeExecution.getSkipInfo())
        .nodeRunInfo(nodeExecution.getNodeRunInfo())
        .mode(nodeExecution.getMode())
        .executableResponses(CollectionUtils.emptyIfNull(nodeExecution.getExecutableResponses()))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipType())
        .outcomeDocuments(outcomes)
        .unitProgresses(nodeExecution.getUnitProgresses())
        .progressData(
            OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(nodeExecution.getProgressData()))
        .graphDelegateSelectionLogParams(graphDelegateSelectionLogParamsList)
        .stepDetails(stepDetails)
        .build();
  }

  private PmsStepParameters obtainStepParameters(Map<String, Object> resolvedStepInputs) {
    return PmsStepParameters.parse(
        OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(resolvedStepInputs));
  }
}
