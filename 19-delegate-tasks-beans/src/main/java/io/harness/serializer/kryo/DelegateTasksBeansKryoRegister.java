package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateStringResponseData;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO.ImageType;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO.OSType;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.AzureVMAuthType;
import io.harness.delegate.beans.azure.GalleryImageDefinitionDTO;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConstants;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsDelegateTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConstants;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.delegate.beans.connector.jira.JiraConnectionTaskParams;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskParams;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommandType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsElbListenerRuleData;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.aws.LoadBalancerType;
import io.harness.delegate.task.azure.AzureTaskExecutionRequest;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.AzureTaskParameters;
import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppDeploymentSlotNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import io.harness.delegate.task.azure.request.AzureLoadBalancerDetailForBGDeployment;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSGetVirtualMachineScaleSetParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListLoadBalancerBackendPoolsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListLoadBalancersNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListResourceGroupsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListSubscriptionsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVMDataParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVirtualMachineScaleSetsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSwitchRouteTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;
import io.harness.delegate.task.azure.response.AzureVMSSDeployTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSGetVirtualMachineScaleSetResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListLoadBalancerBackendPoolsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListLoadBalancersNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListResourceGroupsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListSubscriptionsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVMDataResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVirtualMachineScaleSetsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSSwitchRoutesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.delegate.task.ci.CIBuildPushParameters;
import io.harness.delegate.task.ci.CIBuildPushParameters.CIBuildPushTaskType;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse.JiraIssueData;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstGetElastigroupJsonParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupNamesParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbDeployParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSetupParameters;
import io.harness.delegate.task.spotinst.request.SpotinstTrafficShiftAlbSwapRoutesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstGetElastigroupJsonResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupNamesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbDeployResponse;
import io.harness.delegate.task.spotinst.response.SpotinstTrafficShiftAlbSetupResponse;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.serializer.KryoRegistrar;
import org.eclipse.jgit.api.GitCommand;
import org.json.JSONArray;
import org.json.JSONObject;

public class DelegateTasksBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AlwaysFalseValidationCapability.class, 19036);
    kryo.register(AppDynamicsConnectorDTO.class, 19105);
    kryo.register(AppDynamicsConnectionTaskParams.class, 19107);
    kryo.register(AppDynamicsConnectionTaskResponse.class, 19108);
    kryo.register(ArtifactFileMetadata.class, 19034);
    kryo.register(AwsElbListener.class, 5600);
    kryo.register(AwsElbListenerRuleData.class, 19035);
    kryo.register(AwsLoadBalancerDetails.class, 19024);
    kryo.register(AwsRegionCapability.class, 19008);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetParameters.class, 19075);
    kryo.register(AzureVMSSGetVirtualMachineScaleSetResponse.class, 19080);
    kryo.register(AzureVMSSListResourceGroupsNamesParameters.class, 19076);
    kryo.register(AzureVMSSListResourceGroupsNamesResponse.class, 19081);
    kryo.register(AzureVMSSListSubscriptionsParameters.class, 19077);
    kryo.register(AzureVMSSListSubscriptionsResponse.class, 19082);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsParameters.class, 19078);
    kryo.register(AzureVMSSListVirtualMachineScaleSetsResponse.class, 19083);
    kryo.register(AzureVMSSTaskExecutionResponse.class, 19084);
    kryo.register(AzureVMSSTaskParameters.class, 19079);
    kryo.register(AzureVMSSTaskResponse.class, 19085);
    kryo.register(AzureVMSSTaskType.class, 19086);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(ChartMuseumCapability.class, 19038);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(CommandExecutionResult.class, 5036);
    kryo.register(ConnectorValidationResult.class, 19059);
    kryo.register(CustomCommitAttributes.class, 19070);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateRetryableException.class, 5521);
    kryo.register(DelegateTaskDetails.class, 19044);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(DelegateTaskPackage.class, 7150);
    kryo.register(DelegateTaskResponse.class, 5006);
    kryo.register(DelegateTaskResponse.ResponseCode.class, 5520);
    kryo.register(DirectK8sInfraDelegateConfig.class, 19102);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(FetchType.class, 8030);
    kryo.register(GitAuthenticationDTO.class, 19063);
    kryo.register(GitAuthType.class, 19066);
    kryo.register(GitCommand.class, 19062);
    kryo.register(GitCommandExecutionResponse.class, 19067);
    kryo.register(GitCommandParams.class, 19061);
    kryo.register(GitCommandStatus.class, 19074);
    kryo.register(GitCommandType.class, 19071);
    kryo.register(GitConfigDTO.class, 19060);
    kryo.register(GitConnectionType.class, 19068);
    kryo.register(GitHTTPAuthenticationDTO.class, 19064);
    kryo.register(GitSSHAuthenticationDTO.class, 19065);
    kryo.register(GitStoreDelegateConfig.class, 19104);
    kryo.register(GitSyncConfig.class, 19069);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(HttpTaskParameters.class, 20002);
    kryo.register(K8sDeployRequest.class, 19101);
    kryo.register(K8sDeployResponse.class, 19099);
    kryo.register(K8sManifestDelegateConfig.class, 19103);
    kryo.register(K8sRollingDeployRequest.class, 19100);
    kryo.register(K8sTaskType.class, 7125);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(KubernetesClientKeyCertDTO.class, 19053);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesConnectionTaskParams.class, 19057);
    kryo.register(KubernetesConnectionTaskResponse.class, 19056);
    kryo.register(KubernetesCredentialSpecDTO.class, 19047);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesOpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesServiceAccountDTO.class, 19054);
    kryo.register(KubernetesUserNamePasswordDTO.class, 19052);
    kryo.register(LbDetailsForAlbTrafficShift.class, 19037);
    kryo.register(LoadBalancerDetailsForBGDeployment.class, 19031);
    kryo.register(LoadBalancerType.class, 19032);
    kryo.register(PcfManifestsPackage.class, 19033);
    kryo.register(ProcessExecutorCapability.class, 19007);
    kryo.register(RemoteMethodReturnValueData.class, 5122);
    kryo.register(ScriptType.class, 5253);
    kryo.register(SecretDetail.class, 19001);
    kryo.register(SelectorCapability.class, 19098);
    kryo.register(ShellScriptApprovalTaskParameters.class, 20001);
    kryo.register(SocketConnectivityExecutionCapability.class, 19009);
    kryo.register(SpotInstDeployTaskParameters.class, 19018);
    kryo.register(SpotInstDeployTaskResponse.class, 19017);
    kryo.register(SpotInstGetElastigroupJsonParameters.class, 19025);
    kryo.register(SpotInstGetElastigroupJsonResponse.class, 19028);
    kryo.register(SpotInstListElastigroupInstancesParameters.class, 19026);
    kryo.register(SpotInstListElastigroupInstancesResponse.class, 19029);
    kryo.register(SpotInstListElastigroupNamesParameters.class, 19027);
    kryo.register(SpotInstListElastigroupNamesResponse.class, 19030);
    kryo.register(SpotInstSetupTaskParameters.class, 19012);
    kryo.register(SpotInstSetupTaskResponse.class, 19016);
    kryo.register(SpotInstSwapRoutesTaskParameters.class, 19023);
    kryo.register(SpotInstTaskExecutionResponse.class, 19014);
    kryo.register(SpotInstTaskParameters.class, 19011);
    kryo.register(SpotInstTaskResponse.class, 19015);
    kryo.register(SpotInstTaskType.class, 19013);
    kryo.register(SpotinstTrafficShiftAlbDeployParameters.class, 19041);
    kryo.register(SpotinstTrafficShiftAlbDeployResponse.class, 19042);
    kryo.register(SpotinstTrafficShiftAlbSetupParameters.class, 19039);
    kryo.register(SpotinstTrafficShiftAlbSetupResponse.class, 19040);
    kryo.register(SpotinstTrafficShiftAlbSwapRoutesParameters.class, 19043);
    kryo.register(SystemEnvCheckerCapability.class, 19022);
    kryo.register(TaskData.class, 19002);
    kryo.register(YamlGitConfigDTO.class, 19087);
    kryo.register(YamlGitConfigDTO.RootFolder.class, 19095);
    kryo.register(AzureVMSSPreDeploymentData.class, 19106);
    kryo.register(SplunkConnectionTaskParams.class, 19109);
    kryo.register(SplunkConnectionTaskResponse.class, 19110);
    kryo.register(SplunkConnectorDTO.class, 19111);
    kryo.register(DockerAuthCredentialsDTO.class, 19112);
    kryo.register(DockerAuthenticationDTO.class, 19113);
    kryo.register(DockerAuthType.class, 19114);
    kryo.register(DockerConnectorDTO.class, 19115);
    kryo.register(DockerUserNamePasswordDTO.class, 19116);
    kryo.register(DockerTestConnectionTaskParams.class, 19117);
    kryo.register(DockerTestConnectionTaskResponse.class, 19118);
    kryo.register(ArtifactTaskParameters.class, 19300);
    kryo.register(ArtifactTaskResponse.class, 19301);
    kryo.register(DockerArtifactDelegateRequest.class, 19302);
    kryo.register(DockerArtifactDelegateResponse.class, 19303);
    kryo.register(ArtifactTaskType.class, 19304);
    kryo.register(ArtifactDelegateResponse.class, 19305);
    kryo.register(ArtifactTaskExecutionResponse.class, 19306);
    kryo.register(ArtifactBuildDetailsNG.class, 19307);
    kryo.register(ArtifactSourceType.class, 19308);
    kryo.register(DelegateStringResponseData.class, 19309);
    kryo.register(AzureVMSSSetupTaskParameters.class, 19310);
    kryo.register(AzureVMSSListVMDataParameters.class, 19311);
    kryo.register(AzureVMSSListLoadBalancersNamesParameters.class, 19312);
    kryo.register(AzureVMSSListLoadBalancerBackendPoolsNamesParameters.class, 19313);
    kryo.register(AzureVMSSDeployTaskParameters.class, 19314);
    kryo.register(AzureLoadBalancerDetailForBGDeployment.class, 19315);
    kryo.register(AzureVMInstanceData.class, 19316);
    kryo.register(AzureVMSSDeployTaskResponse.class, 19317);
    kryo.register(AzureVMSSListLoadBalancerBackendPoolsNamesResponse.class, 19318);
    kryo.register(AzureVMSSListLoadBalancersNamesResponse.class, 19319);
    kryo.register(AzureVMSSListVMDataResponse.class, 19320);
    kryo.register(AzureVMSSSetupTaskResponse.class, 19321);
    kryo.register(AzureVMSSSwitchRoutesResponse.class, 19322);
    kryo.register(AzureVMSSSwitchRouteTaskParameters.class, 19323);
    kryo.register(GitFetchRequest.class, 19324);
    kryo.register(GitFetchFilesConfig.class, 19325);
    kryo.register(GitFetchResponse.class, 19326);
    kryo.register(TaskStatus.class, 19327);
    kryo.register(K8sRollingDeployResponse.class, 19328);
    kryo.register(StepStatusTaskParameters.class, 19329);
    kryo.register(StepStatusTaskResponseData.class, 19330);
    kryo.register(StepStatus.class, 19331);
    kryo.register(StepMapOutput.class, 19332);
    kryo.register(StepExecutionStatus.class, 19333);
    kryo.register(GitConnectionNGCapability.class, 19334);
    kryo.register(GcpRequest.RequestType.class, 19335);
    kryo.register(GcpValidationRequest.class, 19336);
    kryo.register(GcpValidationTaskResponse.class, 19337);
    kryo.register(SSHConfigValidationTaskResponse.class, 19338);
    kryo.register(AzureConfigDTO.class, 19339);
    kryo.register(AzureVMAuthDTO.class, 19340);
    kryo.register(AzureVMAuthType.class, 19341);
    kryo.register(KubernetesCredentialDTO.class, 19342);
    kryo.register(ExecutionCapability.class, 19343);
    kryo.register(JiraConnectorDTO.class, 19344);
    kryo.register(GcpConnectorDTO.class, 19345);
    kryo.register(GcpConnectorCredentialDTO.class, 19346);
    kryo.register(GcpCredentialType.class, 19347);
    kryo.register(GcpConstants.class, 19348);
    kryo.register(GcpDelegateDetailsDTO.class, 19349);
    kryo.register(GcpManualDetailsDTO.class, 19350);
    kryo.register(AwsConnectorDTO.class, 19351);
    kryo.register(AwsConstants.class, 19352);
    kryo.register(AwsCredentialDTO.class, 19353);
    kryo.register(AwsCredentialSpecDTO.class, 19354);
    kryo.register(AwsCredentialType.class, 19355);
    kryo.register(AwsDelegateTaskResponse.class, 19356);
    kryo.register(AwsInheritFromDelegateSpecDTO.class, 19357);
    kryo.register(AwsManualConfigSpecDTO.class, 19358);
    kryo.register(AwsTaskParams.class, 19359);
    kryo.register(AwsTaskType.class, 19360);
    kryo.register(AwsValidateTaskResponse.class, 19361);
    kryo.register(CrossAccountAccessDTO.class, 19362);
    kryo.register(AzureMachineImageArtifactDTO.class, 19363);
    kryo.register(GalleryImageDefinitionDTO.class, 19364);
    kryo.register(OSType.class, 19365);
    kryo.register(ImageType.class, 19366);
    kryo.register(JiraTaskNGParameters.class, 19367);
    kryo.register(JiraTaskNGResponse.class, 19368);
    kryo.register(JiraIssueData.class, 19369);
    kryo.register(JiraConnectionTaskParams.class, 19370);
    kryo.register(JiraTestConnectionTaskNGResponse.class, 19371);
    kryo.register(ConnectorType.class, 19372);
    kryo.register(JSONArray.class, 19373);
    kryo.register(JSONObject.class, 19374);
    kryo.register(CVConnectorTaskParams.class, 19375);
    kryo.register(CVConnectorTaskResponse.class, 19376);
    kryo.register(BuildStatusPushResponse.class, 19377);
    kryo.register(BuildStatusPushResponse.Status.class, 19378);
    kryo.register(CIBuildPushParameters.class, 19379);
    kryo.register(CIBuildPushTaskType.class, 19380);
    kryo.register(CIBuildStatusPushParameters.class, 19381);
    kryo.register(AzureWebAppListWebAppDeploymentSlotNamesParameters.class, 19382);
    kryo.register(AzureWebAppListWebAppNamesParameters.class, 19383);
    kryo.register(AzureWebAppListWebAppDeploymentSlotNamesResponse.class, 19384);
    kryo.register(AzureWebAppListWebAppNamesResponse.class, 19385);
    kryo.register(AzureAppServiceTaskParameters.class, 19386);
    kryo.register(AzureAppServiceTaskResponse.class, 19387);
    kryo.register(AzureTaskParameters.class, 19388);
    kryo.register(AzureTaskResponse.class, 19389);
    kryo.register(AzureAppServiceTaskType.class, 19390);
    kryo.register(AzureAppServiceType.class, 19391);
    kryo.register(AzureTaskExecutionRequest.class, 19392);
    kryo.register(AzureTaskExecutionResponse.class, 19393);
  }
}
