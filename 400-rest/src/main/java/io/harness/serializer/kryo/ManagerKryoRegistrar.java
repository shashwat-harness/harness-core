/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessModule._360_CG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseType;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.delegate.NoEligibleDelegatesInAccountException;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.exception.DelegateTaskExpiredException;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.ARMStateExecutionData;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AppManifestCollectionExecutionData;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.AwsAmiSwitchRoutesStateExecutionData;
import software.wings.api.AwsAmiTrafficShiftAlbStateExecutionData;
import software.wings.api.AwsClusterExecutionData;
import software.wings.api.BambooExecutionData;
import software.wings.api.BarrierExecutionData;
import software.wings.api.BarrierStepExecutionSummary;
import software.wings.api.CloudWatchExecutionData;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContinuePipelineResponseData;
import software.wings.api.EcsServiceExecutionData;
import software.wings.api.EcsStepExecutionSummary;
import software.wings.api.ElbStateExecutionData;
import software.wings.api.EmailStateExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.GcbExecutionData;
import software.wings.api.GcpClusterExecutionData;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.HelmSetupExecutionSummary;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.InfraNodeRequest;
import software.wings.api.InstanceFetchStateExecutionSummary;
import software.wings.api.JenkinsExecutionData;
import software.wings.api.KubernetesSteadyStateCheckExecutionData;
import software.wings.api.KubernetesSteadyStateCheckExecutionSummary;
import software.wings.api.KubernetesSwapServiceSelectorsExecutionData;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.PipelineElement;
import software.wings.api.ResourceConstraintExecutionData;
import software.wings.api.ResourceConstraintStepExecutionSummary;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.api.SkipStateExecutionData;
import software.wings.api.SplunkStateExecutionData;
import software.wings.api.TemplatizedSecretManagerStateExecutionData;
import software.wings.api.TerraformApplyMarkerParam;
import software.wings.api.WaitStateExecutionData;
import software.wings.api.WingsTimestamp;
import software.wings.api.arm.ARMOutputVariables;
import software.wings.api.arm.ARMPreExistingTemplate;
import software.wings.api.artifact.ServiceArtifactElements;
import software.wings.api.artifact.ServiceArtifactVariableElements;
import software.wings.api.customdeployment.InstanceFetchStateExecutionData;
import software.wings.api.ecs.EcsListenerUpdateExecutionSummary;
import software.wings.api.ecs.EcsListenerUpdateStateExecutionData;
import software.wings.api.ecs.EcsRoute53WeightUpdateStateExecutionData;
import software.wings.api.ecs.EcsRunTaskStateExecutionData;
import software.wings.api.helm.HelmReleaseInfoElement;
import software.wings.api.helm.ServiceHelmElements;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.api.k8s.K8sApplicationManifestSourceInfo;
import software.wings.api.k8s.K8sCanaryDeleteServiceElement;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sExecutionSummary;
import software.wings.api.k8s.K8sGitConfigMapInfo;
import software.wings.api.k8s.K8sHelmDeploymentElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.api.k8s.K8sSwapServiceElement;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.InfoVariables;
import software.wings.api.pcf.PcfDeployExecutionSummary;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfPluginExecutionSummary;
import software.wings.api.pcf.PcfPluginStateExecutionData;
import software.wings.api.pcf.PcfRouteSwapExecutionSummary;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupExecutionSummary;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.api.pcf.SwapRouteRollbackSweepingOutputPcf;
import software.wings.api.terraform.TerraformOutputVariables;
import software.wings.api.terragrunt.TerragruntApplyMarkerParam;
import software.wings.api.terragrunt.TerragruntOutputVariables;
import software.wings.beans.Account;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.AccountPreferences;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.AuthToken;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.Base;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ConfigFile;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityVersion;
import software.wings.beans.Event;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.Graph;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphLink;
import software.wings.beans.GraphNode;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmCommandFlagConfig;
import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Permission;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.PipelineStageExecutionAdvisor;
import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.ServiceInstance;
import software.wings.beans.Setup;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.TechStack;
import software.wings.beans.TrialSignupOptions;
import software.wings.beans.User;
import software.wings.beans.UserGroupEntityReference;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.ManifestCollectionStatus;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.AzureArtifactsArtifactStream;
import software.wings.beans.artifact.AzureMachineImageArtifactStream;
import software.wings.beans.artifact.AzureMachineImageArtifactStream.OSType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.SftpArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.OverridingCommandUnitDescriptor;
import software.wings.beans.loginSettings.UserLockoutInfo;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistConfig;
import software.wings.beans.security.access.WhitelistStatus;
import software.wings.beans.sso.SSOType;
import software.wings.beans.template.CopiedTemplateMetadata;
import software.wings.beans.template.ImportedTemplateMetadata;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.beans.utm.UtmInfo;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.collect.ArtifactCollectionCallback;
import software.wings.common.RancherK8sClusterProcessor;
import software.wings.delegatetasks.buildsource.BuildSourceCallback;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupCallback;
import software.wings.delegatetasks.event.EventsDeliveryCallback;
import software.wings.expression.EncryptedDataDetails;
import software.wings.expression.ShellScriptEnvironmentVariables;
import software.wings.helpers.ext.cloudformation.CloudFormationCompletionFlag;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.AzureVMSSInfra;
import software.wings.infra.AzureWebAppInfra;
import software.wings.infra.CodeDeployInfrastructure;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.infra.RancherKubernetesInfrastructure;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.service.impl.WorkflowTree;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.aws.model.AwsEcsAllPhaseRollbackData;
import software.wings.service.impl.azure.manager.AzureVMSSAllPhaseRollbackData;
import software.wings.service.impl.email.EmailNotificationCallBack;
import software.wings.service.impl.newrelic.NewRelicMarkerExecutionData;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;
import software.wings.service.impl.spotinst.SpotinstAllPhaseRollbackData;
import software.wings.service.impl.trigger.TriggerCallback;
import software.wings.service.impl.yaml.GitCommandCallback;
import software.wings.service.impl.yaml.gitdiff.gitaudit.AuditYamlHelperForFailedChanges;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResumeAllCallback;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.ExecutionWaitCallback;
import software.wings.sm.ExecutionWaitRetryCallback;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineContinueWithInputsCallback;
import software.wings.sm.ResourceConstraintStatusData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineResumeCallback;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.sm.states.CVNGState.CVNGStateExecutionData;
import software.wings.sm.states.CVNGState.CVNGStateResponseData;
import software.wings.sm.states.EcsRunTaskDataBag;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;
import software.wings.sm.states.ForkState.ForkStateExecutionData;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;
import software.wings.sm.states.azure.AzureVMSSDeployExecutionSummary;
import software.wings.sm.states.azure.AzureVMSSDeployStateExecutionData;
import software.wings.sm.states.azure.AzureVMSSSetupExecutionSummary;
import software.wings.sm.states.azure.AzureVMSSSetupStateExecutionData;
import software.wings.sm.states.azure.AzureVMSSSwitchRouteStateExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionSummary;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotShiftTrafficExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotShiftTrafficExecutionSummary;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSwapExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSwapExecutionSummary;
import software.wings.sm.states.k8s.K8sResourcesSweepingOutput;
import software.wings.sm.states.rancher.RancherStateExecutionData;
import software.wings.sm.states.spotinst.SpotInstDeployStateExecutionData;
import software.wings.sm.states.spotinst.SpotInstListenerUpdateStateExecutionData;
import software.wings.sm.states.spotinst.SpotInstSetupExecutionSummary;
import software.wings.sm.states.spotinst.SpotInstSetupStateExecutionData;
import software.wings.sm.states.spotinst.SpotinstDeployExecutionSummary;
import software.wings.sm.states.spotinst.SpotinstTrafficShiftAlbDeployExecutionData;
import software.wings.sm.states.spotinst.SpotinstTrafficShiftAlbSetupExecutionData;
import software.wings.sm.states.spotinst.SpotinstTrafficShiftAlbSwapRoutesExecutionData;
import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
@TargetModule(_360_CG_MANAGER)
@Deprecated
public class ManagerKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AmiStepExecutionSummary.class, 5219);
    kryo.register(ApprovalStateExecutionData.class, 5087);
    kryo.register(ArtifactCollectionExecutionData.class, 5252);
    kryo.register(AwsAmiDeployStateExecutionData.class, 5526);
    kryo.register(AwsAmiSwitchRoutesStateExecutionData.class, 5545);
    kryo.register(AwsClusterExecutionData.class, 5088);
    kryo.register(BarrierExecutionData.class, 5258);
    kryo.register(BarrierStepExecutionSummary.class, 5257);
    kryo.register(CloudFormationCompletionFlag.class, 8071);
    kryo.register(CloudWatchExecutionData.class, 5091);
    kryo.register(CommandStateExecutionData.class, 5093);
    kryo.register(CommandStepExecutionSummary.class, 5094);
    kryo.register(EcsListenerUpdateExecutionSummary.class, 5612);
    kryo.register(EcsListenerUpdateStateExecutionData.class, 5614);
    kryo.register(EcsServiceExecutionData.class, 5097);
    kryo.register(EcsStepExecutionSummary.class, 5098);
    kryo.register(ElbStateExecutionData.class, 5099);
    kryo.register(EmailStateExecutionData.class, 5100);
    kryo.register(EnvStateExecutionData.class, 5101);
    kryo.register(GcpClusterExecutionData.class, 5102);
    kryo.register(HelmDeployStateExecutionData.class, 5465);
    kryo.register(HelmSetupExecutionSummary.class, 5267);
    kryo.register(HttpStateExecutionData.class, 5103);
    kryo.register(InfraNodeRequest.class, 5104);
    kryo.register(JenkinsExecutionData.class, 5106);
    kryo.register(KubernetesSteadyStateCheckExecutionData.class, 5278);
    kryo.register(KubernetesSteadyStateCheckExecutionSummary.class, 5331);
    kryo.register(KubernetesSwapServiceSelectorsExecutionData.class, 5367);
    kryo.register(PcfDeployExecutionSummary.class, 5287);
    kryo.register(PcfDeployStateExecutionData.class, 5292);
    kryo.register(PcfRouteSwapExecutionSummary.class, 5300);
    kryo.register(PcfRouteUpdateStateExecutionData.class, 5299);

    kryo.register(PcfSetupExecutionSummary.class, 5288);
    kryo.register(PhaseExecutionData.class, 5108);
    kryo.register(PhaseStepExecutionData.class, 5109);
    kryo.register(PipelineElement.class, 5527);
    kryo.register(ResourceConstraintExecutionData.class, 5482);
    kryo.register(ResourceConstraintStepExecutionSummary.class, 5483);
    kryo.register(SelectedNodeExecutionData.class, 5110);
    kryo.register(SelectNodeStepExecutionSummary.class, 5111);
    kryo.register(SplunkStateExecutionData.class, 5115);
    kryo.register(WaitStateExecutionData.class, 5116);
    kryo.register(WingsTimestamp.class, 5085);
    kryo.register(OverridingCommandUnitDescriptor.class, 5047);

    kryo.register(Account.class, 5356);
    kryo.register(Base.class, 5001);
    kryo.register(CanaryWorkflowExecutionAdvisor.class, 5024);

    kryo.register(ElementExecutionSummary.class, 5027);
    kryo.register(Graph.class, 5060);
    kryo.register(ChangeContext.class, 5199);
    kryo.register(GraphGroup.class, 5063);
    kryo.register(GraphLink.class, 5062);
    kryo.register(GraphNode.class, 5061);
    kryo.register(Permission.class, 5310);
    kryo.register(Role.class, 5194);
    kryo.register(RoleType.class, 5195);
    kryo.register(AccountPermissions.class, 5350);
    kryo.register(AppPermission.class, 5351);
    kryo.register(UserGroup.class, 5349);
    kryo.register(ServiceInstance.class, 5028);
    kryo.register(SSOType.class, 5503);
    kryo.register(User.class, 5355);
    kryo.register(UserInvite.class, 5182);
    kryo.register(UserInviteSource.SourceType.class, 5507);
    kryo.register(UserInviteSource.class, 5506);
    kryo.register(YamlType.class, 5211);
    kryo.register(EcsListenerUpdateRequestConfigData.class, 5610);
    kryo.register(TwoFactorAuthenticationMechanism.class, 5358);

    kryo.register(NewRelicMarkerExecutionData.class, 5243);
    kryo.register(PrometheusMetricDataResponse.PrometheusMetric.class, 5489);
    kryo.register(PrometheusMetricDataResponse.PrometheusMetricData.class, 5487);
    kryo.register(PrometheusMetricDataResponse.PrometheusMetricDataResult.class, 5488);
    kryo.register(PrometheusMetricDataResponse.class, 5315);
    kryo.register(WorkflowTree.class, 5369);
    kryo.register(WorkflowExecutionUpdate.class, 5126);
    kryo.register(ElementNotifyResponseData.class, 5147);
    kryo.register(ExecutionInterrupt.class, 4009);
    kryo.register(ExecutionResponse.class, 5135);
    kryo.register(ExecutionStatusData.class, 5137);
    kryo.register(InfraMappingSummary.class, 5138);
    kryo.register(InstanceStatusSummary.class, 5139);
    kryo.register(PhaseExecutionSummary.class, 5140);
    kryo.register(PhaseStepExecutionSummary.class, 5141);
    kryo.register(StateExecutionInstance.class, 5134);
    kryo.register(ApprovalStateType.class, 5617);
    kryo.register(ForkStateExecutionData.class, 4006);
    kryo.register(RepeatStateExecutionData.class, 4003);
    kryo.register(StateType.class, 4001);
    kryo.register(VerificationStateAnalysisExecutionData.class, 5552);
    kryo.register(EcsRoute53WeightUpdateStateExecutionData.class, 7105);

    kryo.register(AwsAmiSetupExecutionData.class, 7120);
    kryo.register(K8sExecutionSummary.class, 7131);
    kryo.register(K8sStateExecutionData.class, 7134);
    kryo.register(K8sElement.class, 7144);
    kryo.register(TemplateReference.class, 7161);
    kryo.register(MLAnalysisType.class, 7175);
    kryo.register(UserLockoutInfo.class, 7196);
    kryo.register(ConfigFile.class, 71107);
    kryo.register(ConfigFile.ConfigOverrideType.class, 71108);
    kryo.register(EntityVersion.class, 71109);
    kryo.register(EntityVersion.ChangeType.class, 71110);
    kryo.register(ApprovalState.class, 7219);
    kryo.register(SpotInstSetupExecutionSummary.class, 7224);
    kryo.register(SpotInstDeployStateExecutionData.class, 7225);
    kryo.register(SpotInstListenerUpdateStateExecutionData.class, 7227);
    kryo.register(PcfSetupStateExecutionData.class, 7237);
    kryo.register(SpotInstSetupStateExecutionData.class, 7241);
    kryo.register(SpotinstDeployExecutionSummary.class, 7242);
    kryo.register(SpotinstAllPhaseRollbackData.class, 7245);
    kryo.register(K8sSwapServiceElement.class, 7260);

    kryo.register(PcfPluginExecutionSummary.class, 7264);
    kryo.register(PcfPluginStateExecutionData.class, 7265);

    kryo.register(EnvExecutionResponseData.class, 7270);
    kryo.register(BambooExecutionData.class, 7271);
    kryo.register(VerificationDataAnalysisResponse.class, 7275);
    kryo.register(ResourceConstraintStatusData.class, 7276);

    kryo.register(SetupSweepingOutputPcf.class, 7278);
    kryo.register(InfoVariables.class, 7279);
    kryo.register(SwapRouteRollbackSweepingOutputPcf.class, 7280);
    kryo.register(DeploySweepingOutputPcf.class, 7281);
    kryo.register(InfraMappingSweepingOutput.class, 7282);
    kryo.register(UtmInfo.class, 7291);

    kryo.register(TerraformApplyMarkerParam.class, 7292);

    kryo.register(SkipStateExecutionData.class, 7322);
    kryo.register(InstanceInfoVariables.class, 7331);

    kryo.register(SpotinstTrafficShiftAlbSetupExecutionData.class, 7339);
    kryo.register(SpotinstTrafficShiftAlbDeployExecutionData.class, 7340);
    kryo.register(ServiceArtifactElements.class, 7342);
    kryo.register(ServiceArtifactVariableElements.class, 7343);
    kryo.register(SpotinstTrafficShiftAlbSwapRoutesExecutionData.class, 7344);
    kryo.register(HarnessImportedTemplateDetails.class, 7373);
    kryo.register(ImportedTemplateMetadata.class, 7375);
    kryo.register(CopiedTemplateMetadata.class, 7376);
    kryo.register(AppPermissionSummaryForUI.class, 7395);
    kryo.register(UserRestrictionInfo.class, 7396);
    kryo.register(AppPermissionSummary.class, 7397);
    kryo.register(EnvInfo.class, 7399);
    kryo.register(AuthToken.class, 7400);
    kryo.register(WhitelistConfig.class, 7401);
    kryo.register(Whitelist.class, 7402);
    kryo.register(UserPermissionInfo.class, 7403);
    kryo.register(AccountPermissionSummary.class, 7404);
    kryo.register(WhitelistStatus.class, 7405);
    kryo.register(ApiKeyEntry.class, 7406);
    kryo.register(AppPermissionSummary.ExecutableElementInfo.class, 7407);

    kryo.register(GcbExecutionData.class, 7410);
    kryo.register(Event.class, 7429);
    kryo.register(Event.Type.class, 7430);
    kryo.register(AwsAmiTrafficShiftAlbStateExecutionData.class, 7436);
    kryo.register(AccountEvent.class, 7445);
    kryo.register(AccountEventType.class, 7446);
    kryo.register(TechStack.class, 7447);
    kryo.register(K8sHelmDeploymentElement.class, 7449);
    kryo.register(io.harness.dashboard.Action.class, 7453);

    kryo.register(TemplatizedSecretManagerStateExecutionData.class, 7457);
    kryo.register(CeLicenseInfo.class, 7465);
    kryo.register(CeLicenseType.class, 7466);
    kryo.register(AzureVMSSSetupStateExecutionData.class, 7468);
    kryo.register(AzureVMSSSetupExecutionSummary.class, 7469);
    kryo.register(InstanceFetchStateExecutionData.class, 7471);
    kryo.register(InstanceFetchStateExecutionSummary.class, 7472);

    kryo.register(AzureVMSSDeployExecutionSummary.class, 8060);
    kryo.register(TerraformOutputVariables.class, 8063);
    kryo.register(EcsRunTaskStateExecutionData.class, 8084);
    kryo.register(EcsRunTaskDataBag.class, 8085);
    kryo.register(PipelineStageExecutionAdvisor.class, 8072);
    kryo.register(ContinuePipelineResponseData.class, 8073);

    kryo.register(AzureVMSSAllPhaseRollbackData.class, 8092);
    kryo.register(TrialSignupOptions.class, 8093);
    kryo.register(TrialSignupOptions.Products.class, 8094);

    kryo.register(HelmSubCommand.class, 8076);
    kryo.register(HelmCommandFlagConfig.class, 8077);

    kryo.register(AzureAppServiceSlotSetupExecutionData.class, 8099);
    kryo.register(AzureAppServiceSlotSetupExecutionSummary.class, 8110);

    kryo.register(AzureAppServiceSlotShiftTrafficExecutionData.class, 8111);
    kryo.register(AzureAppServiceSlotShiftTrafficExecutionSummary.class, 8112);
    kryo.register(AzureAppServiceSlotSwapExecutionData.class, 8113);
    kryo.register(AzureAppServiceSlotSwapExecutionSummary.class, 8114);
    kryo.register(CVNGStateResponseData.class, 8115);
    kryo.register(CVNGVerificationTask.Status.class, 8117);
    kryo.register(HelmReleaseInfoElement.class, 8118);
    kryo.register(AwsEcsAllPhaseRollbackData.class, 8119);
    kryo.register(ShellScriptEnvironmentVariables.class, 8120);

    kryo.register(CVNGStateExecutionData.class, 8501);

    kryo.register(ARMOutputVariables.class, 8121);
    kryo.register(ARMPreExistingTemplate.class, 8122);
    kryo.register(ARMStateExecutionData.class, 8123);
    kryo.register(AccountPreferences.class, 8124);
    kryo.register(TerragruntApplyMarkerParam.class, 8506);
    kryo.register(TerragruntOutputVariables.class, 8508);

    kryo.register(StateMachineResumeCallback.class, 40001);
    kryo.register(BuildSourceCleanupCallback.class, 40002);
    kryo.register(ExecutionResumeAllCallback.class, 40003);
    kryo.register(TriggerCallback.class, 40004);
    kryo.register(PipelineContinueWithInputsCallback.class, 40005);
    kryo.register(GitCommandCallback.class, 40006);
    kryo.register(ExecutionWaitRetryCallback.class, 40007);
    kryo.register(DataCollectionCallback.class, 40008);
    kryo.register(EmailNotificationCallBack.class, 40009);
    kryo.register(BuildSourceCallback.class, 40010);
    kryo.register(ArtifactCollectionCallback.class, 40011);
    kryo.register(ExecutionWaitCallback.class, 40012);

    kryo.register(EventsDeliveryCallback.class, 40014);
    kryo.register(K8sResourcesSweepingOutput.class, 40019);
    kryo.register(K8sGitConfigMapInfo.class, 40023);
    kryo.register(K8sApplicationManifestSourceInfo.class, 40024);
    kryo.register(DirectKubernetesCluster.class, 40051);
    kryo.register(EcsCluster.class, 40052);
    kryo.register(io.harness.ccm.cluster.entities.GcpKubernetesCluster.class, 40053);
    kryo.register(ClusterRecord.class, 40054);
    kryo.register(io.harness.ccm.cluster.entities.AzureKubernetesCluster.class, 40055);
    kryo.register(ApplicationManifest.class, 40056);
    kryo.register(HelmChartConfig.class, 40057);
    kryo.register(AuditYamlHelperForFailedChanges.ArtifactStreamWithOnlyAuditNeededData.class, 40058);
    kryo.register(NexusArtifactStream.class, 40079);
    kryo.register(AzureMachineImageArtifactStream.class, 40059);
    kryo.register(SmbArtifactStream.class, 40061);
    kryo.register(JenkinsArtifactStream.class, 40062);
    kryo.register(DockerArtifactStream.class, 40063);
    kryo.register(AmazonS3ArtifactStream.class, 40064);
    kryo.register(SftpArtifactStream.class, 40065);
    kryo.register(AcrArtifactStream.class, 40066);
    kryo.register(AzureMachineImageArtifactStream.ImageDefinition.class, 40067);
    kryo.register(AzureArtifactsArtifactStream.class, 40068);
    kryo.register(AmiArtifactStream.class, 40069);
    kryo.register(GcrArtifactStream.class, 40070);
    kryo.register(BambooArtifactStream.class, 40071);
    kryo.register(ArtifactoryArtifactStream.class, 40072);
    kryo.register(Activity.class, 40073);
    kryo.register(Setup.class, 40075);
    kryo.register(EcrArtifactStream.class, 40076);
    kryo.register(CustomArtifactStream.class, 40077);
    kryo.register(GcsArtifactStream.class, 40078);
    kryo.register(software.wings.beans.Service.class, 40080);
    kryo.register(AzureVMSSInfra.class, 40081);
    kryo.register(AzureKubernetesService.class, 40082);
    kryo.register(AwsEcsInfrastructure.class, 40083);
    kryo.register(AwsInstanceInfrastructure.class, 40084);
    kryo.register(AwsAmiInfrastructure.class, 40085);
    kryo.register(PhysicalInfraWinrm.class, 40086);
    kryo.register(CustomInfrastructure.class, 40087);
    kryo.register(PcfInfraStructure.class, 40088);
    kryo.register(InfrastructureDefinition.class, 40089);
    kryo.register(AzureInstanceInfrastructure.class, 40090);
    kryo.register(DirectKubernetesInfrastructure.class, 40091);
    kryo.register(GoogleKubernetesEngine.class, 40093);
    kryo.register(AwsLambdaInfrastructure.class, 40094);
    kryo.register(CodeDeployInfrastructure.class, 40095);
    kryo.register(PhysicalInfra.class, 40096);
    kryo.register(AzureWebAppInfra.class, 40097);
    kryo.register(AzureVMSSInfrastructureMapping.class, 400106);
    kryo.register(AwsLambdaInfraStructureMapping.class, 400107);
    kryo.register(EcsInfrastructureMapping.class, 400108);
    kryo.register(GcpKubernetesInfrastructureMapping.class, 400109);
    kryo.register(PhysicalInfrastructureMapping.class, 400110);
    kryo.register(AwsInfrastructureMapping.class, 400111);
    kryo.register(PhysicalInfrastructureMappingWinRm.class, 400112);
    kryo.register(AuditYamlHelperForFailedChanges.InfraMappingWithOnlyAuditNeededData.class, 400113);
    kryo.register(CustomInfrastructureMapping.class, 400114);
    kryo.register(AzureKubernetesInfrastructureMapping.class, 400115);
    kryo.register(DirectKubernetesInfrastructureMapping.class, 400116);
    kryo.register(AzureWebAppInfrastructureMapping.class, 400117);
    kryo.register(AzureInfrastructureMapping.class, 400118);
    kryo.register(AwsAmiInfrastructureMapping.class, 400119);
    kryo.register(PcfInfrastructureMapping.class, 400120);
    kryo.register(CodeDeployInfrastructureMapping.class, 400121);
    kryo.register(StateStatusUpdateInfo.class, 400122);
    kryo.register(ManifestCollectionStatus.class, 400123);
    kryo.register(CommandUnitDetails.CommandUnitType.class, 400129);
    kryo.register(SetupStatus.class, 400130);
    kryo.register(Type.class, 400131);
    kryo.register(OSType.class, 400132);
    kryo.register(ServiceHelmElements.class, 400136);
    kryo.register(AppManifestCollectionExecutionData.class, 400137);
    kryo.register(RancherK8sClusterProcessor.RancherClusterElementList.class, 50001);
    kryo.register(RancherKubernetesInfrastructureMapping.class, 50003);
    kryo.register(RancherKubernetesInfrastructure.class, 50007);
    kryo.register(RancherStateExecutionData.class, 50009);
    kryo.register(UserGroupEntityReference.class, 50010);
    kryo.register(EncryptedDataDetails.class, 50013);
    kryo.register(K8sCanaryDeleteServiceElement.class, 50016);
    kryo.register(AzureVMSSDeployStateExecutionData.class, 50017);
    kryo.register(AzureVMSSSwitchRouteStateExecutionData.class, 50018);
    kryo.register(NoInstalledDelegatesException.class, 73988);
    kryo.register(NoEligibleDelegatesInAccountException.class, 73989);
    kryo.register(NoAvailableDelegatesException.class, 73990);
    kryo.register(NoDelegatesException.class, 73991);
    kryo.register(DelegateTaskExpiredException.class, 980036);
  }
}
