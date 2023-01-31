/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.beans.FeatureName;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.artifact.outcome.ArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactoryArtifactOutcome;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.TanzuApplicationServiceInfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.manifest.yaml.TasManifestOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.pcf.request.TasManifestsPackage;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.logstreaming.ILogStreamingStepClient;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.TaskRequestsUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TasRollingDeployStepTest extends CDNGTestBase {
  @Inject private OutcomeService outcomeService;
  @Mock private TasStepHelper tasStepHelper;
  @Mock private KryoSerializer kryoSerializer;
  @Mock private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Mock private InstanceInfoService instanceInfoService;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private CDStepHelper cdStepHelper;
  @Mock private StepHelper stepHelper;
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private TasRollingDeployStep tasRollingDeployStep;

  private final TanzuApplicationServiceInfrastructureOutcome infrastructureOutcome =
      TanzuApplicationServiceInfrastructureOutcome.builder()
          .connectorRef("account.tas")
          .organization("dev-org")
          .space("dev-space")
          .build();
  private TasRollingDeployStepParameters parameters =
      TasRollingDeployStepParameters.infoBuilder()
          .delegateSelectors(ParameterField.createValueField(List.of(new TaskSelectorYaml("selector-1"))))
          .build();

  private final Ambiance ambiance = getAmbiance();

  @Before
  public void setup() {
    ILogStreamingStepClient logStreamingStepClient;
    logStreamingStepClient = mock(ILogStreamingStepClient.class);
    when(logStreamingStepClientFactory.getLogStreamingStepClient(any())).thenReturn(logStreamingStepClient);
    doReturn(infrastructureOutcome).when(cdStepHelper).getInfrastructureOutcome(ambiance);

    doReturn(EnvironmentType.PROD).when(stepHelper).getEnvironmentType(ambiance);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateResourcesFFEnabled() {
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(anyString(), eq(FeatureName.CDS_TAS_NG));
    Ambiance ambiance = getAmbiance();
    StepElementParameters stepElementParameters =
        StepElementParameters.builder().type("BasicAppSetup").spec(parameters).build();
    tasRollingDeployStep.validateResources(ambiance, stepElementParameters);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateResourcesFFDisabled() {
    doReturn(false).when(cdFeatureFlagHelper).isEnabled(anyString(), eq(FeatureName.CDS_TAS_NG));
    Ambiance ambiance = getAmbiance();
    StepElementParameters stepElementParameters =
        StepElementParameters.builder().type("BasicAppSetup").spec(parameters).build();
    assertThatThrownBy(() -> tasRollingDeployStep.validateResources(ambiance, stepElementParameters))
        .hasMessage("CDS_TAS_NG FF is not enabled for this account. Please contact harness customer care.");
  }

  private Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "account");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project");

    return Ambiance.newBuilder()
        .putAllSetupAbstractions(setupAbstractions)
        .setStageExecutionId("stageExecutionId")
        .build();
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetStepParametersClass() {
    assertThat(tasRollingDeployStep.getStepParametersClass()).isEqualTo(StepElementParameters.class);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testExecuteTasTask() {
    String additionalRoute = "route";
    TasRollingDeployStepParameters tasRollingDeployStepParameters =
        TasRollingDeployStepParameters.infoBuilder()
            .additionalRoutes(ParameterField.createValueField(Arrays.asList(additionalRoute)))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder()
                                                      .spec(tasRollingDeployStepParameters)
                                                      .timeout(ParameterField.createValueField("10m"))
                                                      .build();
    InfrastructureOutcome infrastructureOutcome = TanzuApplicationServiceInfrastructureOutcome.builder().build();
    doReturn(infrastructureOutcome).when(cdStepHelper).getInfrastructureOutcome(ambiance);
    Map<String, String> allFilesFetched = new HashMap<>();
    CfCliVersionNG cliVersionNG = CfCliVersionNG.V7;
    CfCliVersion cfCliVersion = CfCliVersion.V7;
    List<FileData> fileDataList = Collections.emptyList();
    TasManifestsPackage tasManifestsPackage = TasManifestsPackage.builder().build();
    TasExecutionPassThroughData tasExecutionPassThroughData = TasExecutionPassThroughData.builder()
                                                                  .tasManifestsPackage(tasManifestsPackage)
                                                                  .allFilesFetched(allFilesFetched)
                                                                  .cfCliVersion(cliVersionNG)
                                                                  .build();
    TasInfraConfig tasInfraConfig = TasInfraConfig.builder().build();
    UnitProgressData unitProgressData = UnitProgressData.builder().build();
    doReturn(tasInfraConfig).when(cdStepHelper).getTasInfraConfig(infrastructureOutcome, ambiance);
    doReturn(cfCliVersion).when(tasStepHelper).cfCliVersionNGMapper(cliVersionNG);

    ArtifactOutcome artifactsOutcome = ArtifactoryArtifactOutcome.builder().build();
    doReturn(Optional.of(artifactsOutcome)).when(cdStepHelper).resolveArtifactsOutcome(ambiance);

    List<String> routeMaps = Arrays.asList(additionalRoute);
    doReturn(routeMaps)
        .when(tasStepHelper)
        .getRouteMaps(tasExecutionPassThroughData.getTasManifestsPackage().getManifestYml(),
            getParameterFieldValue(tasRollingDeployStepParameters.getAdditionalRoutes()));

    Mockito.mockStatic(TaskRequestsUtils.class);
    PowerMockito.when(TaskRequestsUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    doReturn(EnvironmentType.PROD).when(stepHelper).getEnvironmentType(ambiance);

    TasManifestOutcome tasManifestOutcome = TasManifestOutcome.builder().build();
    TaskChainResponse taskChainResponse = tasRollingDeployStep.executeTasTask(
        tasManifestOutcome, ambiance, stepElementParameters, tasExecutionPassThroughData, true, unitProgressData);

    PowerMockito.verifyStatic(TaskRequestsUtils.class, times(1));
    TaskRequestsUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any());

    assertThat(taskChainResponse.isChainEnd()).isEqualTo(true);
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(TasExecutionPassThroughData.class);
  }
}