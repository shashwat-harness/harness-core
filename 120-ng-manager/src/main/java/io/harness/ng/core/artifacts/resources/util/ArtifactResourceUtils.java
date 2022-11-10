/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.artifacts.resources.util;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GoogleArtifactRegistryConfig;
import io.harness.cdng.artifact.bean.yaml.NexusRegistryArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusConstant;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryDockerConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryMavenConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryNpmConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryNugetConfig;
import io.harness.cdng.artifact.bean.yaml.nexusartifact.NexusRegistryRawConfig;
import io.harness.cdng.artifact.resources.googleartifactregistry.dtos.GARResponseDTO;
import io.harness.cdng.artifact.resources.googleartifactregistry.service.GARResourceService;
import io.harness.cdng.artifact.resources.nexus.dtos.NexusResponseDTO;
import io.harness.cdng.artifact.resources.nexus.service.NexusResourceService;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.common.NGExpressionUtils;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.evaluators.CDYamlExpressionEvaluator;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.template.TemplateApplyRequestDTO;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.TemplateResponseDTO;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.inputset.MergeInputSetResponseDTOPMS;
import io.harness.pms.inputset.MergeInputSetTemplateRequestDTO;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.remote.client.NGRestUtils;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.template.yaml.TemplateRefHelper;
import io.harness.utils.IdentifierRefHelper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class ArtifactResourceUtils {
  private final String preStageFqn = "pipeline/stages/";
  @Inject PipelineServiceClient pipelineServiceClient;
  @Inject TemplateResourceClient templateResourceClient;
  @Inject ServiceEntityService serviceEntityService;
  @Inject EnvironmentService environmentService;
  @Inject NexusResourceService nexusResourceService;
  @Inject GARResourceService garResourceService;

  // Checks whether field is fixed value or not, if empty then also we return false for fixed value.
  public static boolean isFieldFixedValue(String fieldValue) {
    return !isEmpty(fieldValue) && !NGExpressionUtils.isRuntimeOrExpressionField(fieldValue);
  }

  private String getMergedCompleteYaml(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String runtimeInputYaml, GitEntityFindInfoDTO gitEntityBasicInfo) {
    if (isEmpty(pipelineIdentifier)) {
      return runtimeInputYaml;
    }
    MergeInputSetResponseDTOPMS response =
        NGRestUtils.getResponse(pipelineServiceClient.getMergeInputSetFromPipelineTemplate(accountId, orgIdentifier,
            projectIdentifier, pipelineIdentifier, gitEntityBasicInfo.getBranch(),
            gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getBranch(),
            gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getDefaultFromOtherRepo(),
            MergeInputSetTemplateRequestDTO.builder().runtimeInputYaml(runtimeInputYaml).build()));
    if (response.isErrorResponse()) {
      log.error("Failed to get Merged Pipeline Yaml with error yaml - \n "
          + response.getInputSetErrorWrapper().getErrorPipelineYaml());
      log.error("Error map to identify the errors - \n"
          + response.getInputSetErrorWrapper().getUuidToErrorResponseMap().toString());
      throw new InvalidRequestException("Failed to get Merged Pipeline yaml.");
    }
    return response.getCompletePipelineYaml();
  }

  private String applyTemplatesOnGivenYaml(String accountId, String orgIdentifier, String projectIdentifier,
      String yaml, GitEntityFindInfoDTO gitEntityBasicInfo) {
    TemplateMergeResponseDTO response = NGRestUtils.getResponse(templateResourceClient.applyTemplatesOnGivenYaml(
        accountId, orgIdentifier, projectIdentifier, gitEntityBasicInfo.getBranch(),
        gitEntityBasicInfo.getYamlGitConfigId(), gitEntityBasicInfo.getDefaultFromOtherRepo(),
        TemplateApplyRequestDTO.builder().originalEntityYaml(yaml).build()));
    return response.getMergedPipelineYaml();
  }

  public String getResolvedImagePath(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String runtimeInputYaml, String imagePath, String fqnPath,
      GitEntityFindInfoDTO gitEntityBasicInfo, String serviceId) {
    if (EngineExpressionEvaluator.hasExpressions(imagePath)) {
      String mergedCompleteYaml = getMergedCompleteYaml(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, runtimeInputYaml, gitEntityBasicInfo);
      if (isNotEmpty(mergedCompleteYaml) && TemplateRefHelper.hasTemplateRef(mergedCompleteYaml)) {
        mergedCompleteYaml = applyTemplatesOnGivenYaml(
            accountId, orgIdentifier, projectIdentifier, mergedCompleteYaml, gitEntityBasicInfo);
      }
      String[] split = fqnPath.split("\\.");
      String stageIdentifier = split[2];
      YamlConfig yamlConfig = new YamlConfig(mergedCompleteYaml);
      Map<FQN, Object> fqnObjectMap = yamlConfig.getFqnToValueMap();

      if (isEmpty(serviceId)) {
        // pipelines with inline service definitions
        serviceId = getServiceRef(fqnObjectMap, stageIdentifier);
      }
      // get environment ref
      String environmentId = getEnvironmentRef(fqnObjectMap, stageIdentifier);
      List<YamlField> aliasYamlField =
          getAliasYamlFields(accountId, orgIdentifier, projectIdentifier, serviceId, environmentId);
      CDYamlExpressionEvaluator CDYamlExpressionEvaluator =
          new CDYamlExpressionEvaluator(mergedCompleteYaml, fqnPath, aliasYamlField);
      imagePath = CDYamlExpressionEvaluator.renderExpression(imagePath);
    }
    return imagePath;
  }

  /**
   * Returns the serviceRef using stage identifier and fqnToObjectMap.
   * Field name should be serviceRef and fqn should have stage identifier to get the value of serviceRef
   *
   * @param fqnToObjectMap fqn to object map depicting yaml tree as key value pair
   * @param stageIdentifier stage identifier to fetch serviceRef from
   * @return String
   */
  private String getServiceRef(Map<FQN, Object> fqnToObjectMap, String stageIdentifier) {
    for (Map.Entry<FQN, Object> mapEntry : fqnToObjectMap.entrySet()) {
      String nodeStageIdentifier = mapEntry.getKey().getStageIdentifier();
      String fieldName = mapEntry.getKey().getFieldName();
      if (stageIdentifier.equals(nodeStageIdentifier) && YamlTypes.SERVICE_REF.equals(fieldName)
          && mapEntry.getValue() instanceof TextNode) {
        return ((TextNode) mapEntry.getValue()).asText();
      }
    }
    return null;
  }

  /**
   * Returns the environmentRef using stage identifier and fqnToObjectMap.
   * Field name should be environmentRef and fqn should have stage identifier to get the value of environmentRef
   *
   * @param fqnToObjectMap fqn to object map depicting yaml tree as key value pair
   * @param stageIdentifier stage identifier to fetch serviceRef from
   * @return String
   */
  private String getEnvironmentRef(Map<FQN, Object> fqnToObjectMap, String stageIdentifier) {
    for (Map.Entry<FQN, Object> mapEntry : fqnToObjectMap.entrySet()) {
      String nodeStageIdentifier = mapEntry.getKey().getStageIdentifier();
      String fieldName = mapEntry.getKey().getFieldName();
      if (stageIdentifier.equals(nodeStageIdentifier) && YamlTypes.ENVIRONMENT_REF.equals(fieldName)
          && mapEntry.getValue() instanceof TextNode) {
        return ((TextNode) mapEntry.getValue()).asText();
      }
    }
    return null;
  }

  private List<YamlField> getAliasYamlFields(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceId, String environmentId) {
    List<YamlField> yamlFields = new ArrayList<>();
    if (isNotEmpty(serviceId)) {
      Optional<ServiceEntity> optionalService =
          serviceEntityService.get(accountId, orgIdentifier, projectIdentifier, serviceId, false);
      optionalService.ifPresent(
          service -> yamlFields.add(getYamlField(service.fetchNonEmptyYaml(), YAMLFieldNameConstants.SERVICE)));
    }
    if (isNotEmpty(environmentId)) {
      Optional<Environment> optionalEnvironment =
          environmentService.get(accountId, orgIdentifier, projectIdentifier, environmentId, false);
      optionalEnvironment.ifPresent(environment
          -> yamlFields.add(getYamlField(environment.fetchNonEmptyYaml(), YAMLFieldNameConstants.ENVIRONMENT)));
    }
    return yamlFields;
  }

  private YamlField getYamlField(String yaml, String fieldName) {
    try {
      YamlField yamlField = YamlUtils.readTree(yaml);
      return yamlField.getNode().getField(fieldName);
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid service yaml passed.");
    }
  }

  /**
   * Locates ArtifactConfig in a service entity for a given FQN of type
   * pipeline.stages.s1.spec.service.serviceInputs.serviceDefinition.spec.artifacts.primary.spec.tag
   * pipeline.stages.s1.spec.service.serviceInputs.serviceDefinition.spec.artifacts.sidecars[0].sidecar.spec.tag
   * @return ArtifactConfig
   */
  @NotNull
  public ArtifactConfig locateArtifactInService(
      String accountId, String orgId, String projectId, String serviceRef, String imageTagFqn) {
    YamlNode artifactTagLeafNode =
        serviceEntityService.getYamlNodeForFqn(accountId, orgId, projectId, serviceRef, imageTagFqn);

    YamlNode artifactSpecNode = artifactTagLeafNode.getParentNode().getParentNode();

    if (artifactSpecNode.getParentNode() != null
        && "template".equals(artifactSpecNode.getParentNode().getFieldName())) {
      YamlNode templateNode = artifactSpecNode.getParentNode();
      String templateRef = templateNode.getField("templateRef").getNode().getCurrJsonNode().asText();
      String versionLabel = templateNode.getField("versionLabel") != null
          ? templateNode.getField("versionLabel").getNode().getCurrJsonNode().asText()
          : null;

      if (isNotEmpty(templateRef)) {
        IdentifierRef templateIdentifier =
            IdentifierRefHelper.getIdentifierRef(templateRef, accountId, orgId, projectId);
        TemplateResponseDTO response = NGRestUtils.getResponse(
            templateResourceClient.get(templateIdentifier.getIdentifier(), templateIdentifier.getAccountIdentifier(),
                templateIdentifier.getOrgIdentifier(), templateIdentifier.getProjectIdentifier(), versionLabel, false));
        if (!response.getTemplateEntityType().equals(TemplateEntityType.ARTIFACT_SOURCE_TEMPLATE)) {
          throw new InvalidRequestException(
              String.format("Provided template ref: [%s], version: [%s] is not an artifact source template",
                  templateRef, versionLabel));
        }
        if (isEmpty(response.getYaml())) {
          throw new InvalidRequestException(
              String.format("Received empty artifact source template yaml for template ref: %s, version label: %s",
                  templateRef, versionLabel));
        }
        YamlNode artifactTemplateSpecNode;
        try {
          artifactTemplateSpecNode = YamlNode.fromYamlPath(response.getYaml(), "template/spec");
          artifactSpecNode = artifactTemplateSpecNode;
        } catch (IOException e) {
          throw new InvalidRequestException("Cannot read spec from the artifact source template");
        }
      }
    }
    final ArtifactInternalDTO artifactDTO;
    try {
      artifactDTO = YamlUtils.read(artifactSpecNode.toString(), ArtifactInternalDTO.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Unable to read artifact spec in service yaml", e);
    }

    return artifactDTO.spec;
  }

  static class ArtifactInternalDTO {
    @JsonProperty("type") ArtifactSourceType sourceType;
    @JsonProperty("spec")
    @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
    ArtifactConfig spec;
  }

  public NexusResponseDTO getBuildDetails(String nexusConnectorIdentifier, String repositoryName, String repositoryPort,
      String artifactPath, String repositoryFormat, String artifactRepositoryUrl, String orgIdentifier,
      String projectIdentifier, String groupId, String artifactId, String extension, String classifier,
      String packageName, String pipelineIdentifier, String fqnPath, GitEntityFindInfoDTO gitEntityBasicInfo,
      String runtimeInputYaml, String serviceRef, String accountId, String group) {
    if (isNotEmpty(serviceRef)) {
      final ArtifactConfig artifactSpecFromService =
          locateArtifactInService(accountId, orgIdentifier, projectIdentifier, serviceRef, fqnPath);
      NexusRegistryArtifactConfig nexusRegistryArtifactConfig = (NexusRegistryArtifactConfig) artifactSpecFromService;
      switch (nexusRegistryArtifactConfig.getRepositoryFormat().getValue()) {
        case NexusConstant.DOCKER:
          NexusRegistryDockerConfig nexusRegistryDockerConfig =
              (NexusRegistryDockerConfig) nexusRegistryArtifactConfig.getNexusRegistryConfigSpec();
          if (isEmpty(repositoryPort)) {
            repositoryPort = (String) nexusRegistryDockerConfig.getRepositoryPort().fetchFinalValue();
          }

          if (isEmpty(artifactPath)) {
            artifactPath = (String) nexusRegistryDockerConfig.getArtifactPath().fetchFinalValue();
          }

          if (isEmpty(artifactRepositoryUrl)) {
            artifactRepositoryUrl = (String) nexusRegistryDockerConfig.getRepositoryUrl().fetchFinalValue();
          }
          break;
        case NexusConstant.NPM:
          NexusRegistryNpmConfig nexusRegistryNpmConfig =
              (NexusRegistryNpmConfig) nexusRegistryArtifactConfig.getNexusRegistryConfigSpec();
          if (isEmpty(packageName)) {
            packageName = (String) nexusRegistryNpmConfig.getPackageName().fetchFinalValue();
          }
          break;
        case NexusConstant.NUGET:
          NexusRegistryNugetConfig nexusRegistryNugetConfig =
              (NexusRegistryNugetConfig) nexusRegistryArtifactConfig.getNexusRegistryConfigSpec();
          if (isEmpty(packageName)) {
            packageName = (String) nexusRegistryNugetConfig.getPackageName().fetchFinalValue();
          }
          break;
        case NexusConstant.MAVEN:
          NexusRegistryMavenConfig nexusRegistryMavenConfig =
              (NexusRegistryMavenConfig) nexusRegistryArtifactConfig.getNexusRegistryConfigSpec();
          if (isEmpty(artifactId)) {
            artifactId = (String) nexusRegistryMavenConfig.getArtifactId().fetchFinalValue();
          }
          if (isEmpty(groupId)) {
            groupId = (String) nexusRegistryMavenConfig.getGroupId().fetchFinalValue();
          }
          if (isEmpty(classifier)) {
            classifier = (String) nexusRegistryMavenConfig.getClassifier().fetchFinalValue();
          }
          if (isEmpty(extension)) {
            extension = (String) nexusRegistryMavenConfig.getExtension().fetchFinalValue();
          }
          break;
        case NexusConstant.RAW:
          NexusRegistryRawConfig nexusRegistryRawConfig =
              (NexusRegistryRawConfig) nexusRegistryArtifactConfig.getNexusRegistryConfigSpec();
          if (isEmpty(group)) {
            group = (String) nexusRegistryRawConfig.getGroup().fetchFinalValue();
          }
          break;
        default:
          throw new NotFoundException(String.format(
              "Repository Format [%s] is not supported", nexusRegistryArtifactConfig.getRepositoryFormat().getValue()));
      }

      if (isEmpty(repositoryName)) {
        repositoryName = (String) nexusRegistryArtifactConfig.getRepository().fetchFinalValue();
      }
      if (isEmpty(repositoryFormat)) {
        repositoryFormat = (String) nexusRegistryArtifactConfig.getRepositoryFormat().fetchFinalValue();
      }

      if (isEmpty(nexusConnectorIdentifier)) {
        nexusConnectorIdentifier = nexusRegistryArtifactConfig.getConnectorRef().getValue();
      }
    }
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(nexusConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    artifactPath = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        runtimeInputYaml, artifactPath, fqnPath, gitEntityBasicInfo, serviceRef);
    return nexusResourceService.getBuildDetails(connectorRef, repositoryName, repositoryPort, artifactPath,
        repositoryFormat, artifactRepositoryUrl, orgIdentifier, projectIdentifier, groupId, artifactId, extension,
        classifier, packageName, group);
  }
  public GARResponseDTO getBuildDetailsV2GAR(String gcpConnectorIdentifier, String region, String repositoryName,
      String project, String pkg, String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String version, String versionRegex, String fqnPath, String runtimeInputYaml,
      String serviceRef, GitEntityFindInfoDTO gitEntityBasicInfo) {
    if (StringUtils.isNotBlank(serviceRef)) {
      final ArtifactConfig artifactSpecFromService =
          locateArtifactInService(accountId, orgIdentifier, projectIdentifier, serviceRef, fqnPath);

      GoogleArtifactRegistryConfig googleArtifactRegistryConfig =
          (GoogleArtifactRegistryConfig) artifactSpecFromService;

      if (StringUtils.isBlank(gcpConnectorIdentifier)) {
        gcpConnectorIdentifier = (String) googleArtifactRegistryConfig.getConnectorRef().fetchFinalValue();
      }

      if (StringUtils.isBlank(region)) {
        region = (String) googleArtifactRegistryConfig.getRegion().fetchFinalValue();
      }

      if (StringUtils.isBlank(repositoryName)) {
        repositoryName = (String) googleArtifactRegistryConfig.getRepositoryName().fetchFinalValue();
      }

      if (StringUtils.isBlank(project)) {
        project = (String) googleArtifactRegistryConfig.getProject().fetchFinalValue();
      }

      if (StringUtils.isBlank(pkg)) {
        pkg = (String) googleArtifactRegistryConfig.getPkg().fetchFinalValue();
      }
    }

    // Getting the resolvedConnectorRef
    String resolvedConnectorRef = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        runtimeInputYaml, gcpConnectorIdentifier, fqnPath, gitEntityBasicInfo, serviceRef);

    // Getting the resolvedRegion
    String resolvedRegion = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        runtimeInputYaml, region, fqnPath, gitEntityBasicInfo, serviceRef);

    // Getting the resolvedRepositoryName
    String resolvedRepositoryName = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, runtimeInputYaml, repositoryName, fqnPath, gitEntityBasicInfo, serviceRef);

    // Getting the resolvedProject
    String resolvedProject = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        runtimeInputYaml, project, fqnPath, gitEntityBasicInfo, serviceRef);

    // Getting the resolvedPackage
    String resolvedPackage = getResolvedImagePath(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        runtimeInputYaml, pkg, fqnPath, gitEntityBasicInfo, serviceRef);

    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(resolvedConnectorRef, accountId, orgIdentifier, projectIdentifier);

    return garResourceService.getBuildDetails(connectorRef, resolvedRegion, resolvedRepositoryName, resolvedProject,
        resolvedPackage, version, versionRegex, orgIdentifier, projectIdentifier);
  }
}
