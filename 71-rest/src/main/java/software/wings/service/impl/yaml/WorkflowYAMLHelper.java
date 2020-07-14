package software.wings.service.impl.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.NameAccess;
import io.harness.persistence.UuidAccess;
import org.jetbrains.annotations.Nullable;
import software.wings.beans.EntityType;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.settings.SettingVariableTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class WorkflowYAMLHelper {
  @Inject EnvironmentService environmentService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject UserGroupService userGroupService;

  public String getWorkflowVariableValueBean(
      String accountId, String envId, String appId, String entityType, String variableValue, boolean skipEmpty) {
    if (entityType == null || (skipEmpty && isEmpty(variableValue)) || matchesVariablePattern(variableValue)) {
      return variableValue;
    }
    EntityType entityTypeEnum = EntityType.valueOf(entityType);
    List<String> values = new ArrayList<>();
    List<String> returnValues = new ArrayList<>();

    String valuesArray[] = variableValue.split(",");
    values = Arrays.asList(valuesArray);

    for (String str : values) {
      UuidAccess uuidAccess = getUuidAccess(accountId, envId, appId, str, entityTypeEnum);
      if (uuidAccess != null) {
        returnValues.add(uuidAccess.getUuid());
      } else {
        return variableValue;
      }
    }
    return String.join(",", returnValues);
  }

  public String getWorkflowVariableValueBean(
      String accountId, String envId, String appId, String entityType, String variableValue) {
    return getWorkflowVariableValueBean(accountId, envId, appId, entityType, variableValue, false);
  }

  public String getWorkflowVariableValueYaml(
      String appId, String entryValue, EntityType entityType, boolean skipEmpty) {
    if (entityType == null || (skipEmpty && isEmpty(entryValue)) || matchesVariablePattern(entryValue)) {
      return entryValue;
    }

    List<String> userGroupValueName = new ArrayList<>();
    if (entityType.equals(EntityType.USER_GROUP)) {
      List<String> values = new ArrayList<>();
      String valuesArray[] = entryValue.split(",");
      values = Arrays.asList(valuesArray);
      for (String value : values) {
        UserGroup userGroup = userGroupService.get(value);
        String name = userGroup.getName();
        if (name != null) {
          userGroupValueName.add(name);
        } else {
          userGroupValueName.add(entryValue);
        }
      }
      return String.join("\n", userGroupValueName);
    }

    NameAccess x = getNameAccess(appId, entryValue, entityType);
    if (x != null) {
      return x.getName();
    } else {
      return entryValue;
    }
  }

  public String getWorkflowVariableValueYaml(String appId, String entryValue, EntityType entityType) {
    return getWorkflowVariableValueYaml(appId, entryValue, entityType, false);
  }

  @Nullable
  private NameAccess getNameAccess(String appId, String entryValue, EntityType entityType) {
    switch (entityType) {
      case ENVIRONMENT:
        return environmentService.get(appId, entryValue, false);
      case SERVICE:
        return serviceResourceService.get(appId, entryValue, false);
      case INFRASTRUCTURE_MAPPING:
        return infraMappingService.get(appId, entryValue);
      case INFRASTRUCTURE_DEFINITION:
        return infrastructureDefinitionService.get(appId, entryValue);
      case CF_AWS_CONFIG_ID:
      case HELM_GIT_CONFIG_ID:
      case SS_SSH_CONNECTION_ATTRIBUTE:
      case SS_WINRM_CONNECTION_ATTRIBUTE:
      case GCP_CONFIG:
      case GIT_CONFIG:
        return settingsService.get(entryValue);
      default:
        return null;
    }
  }

  @Nullable
  private UuidAccess getUuidAccess(
      String accountId, String envId, String appId, String variableValue, EntityType entityType) {
    UuidAccess uuidAccess;
    switch (entityType) {
      case ENVIRONMENT:
        uuidAccess = environmentService.getEnvironmentByName(appId, variableValue, false);
        notNullCheck("Environment [" + variableValue + "] does not exist", uuidAccess, USER);
        break;
      case SERVICE:
        uuidAccess = serviceResourceService.getServiceByName(appId, variableValue, false);
        notNullCheck("Service [" + variableValue + "] does not exist", uuidAccess, USER);
        break;
      case INFRASTRUCTURE_MAPPING:
        uuidAccess = infraMappingService.getInfraMappingByName(appId, envId, variableValue);
        notNullCheck(
            "Service Infrastructure [" + variableValue + "] does not exist for the environment", uuidAccess, USER);
        break;
      case INFRASTRUCTURE_DEFINITION:
        uuidAccess = infrastructureDefinitionService.getInfraDefByName(appId, envId, variableValue);
        notNullCheck(
            "Infrastructure Definition [" + variableValue + "] does not exist for the environment", uuidAccess, USER);
        break;
      case CF_AWS_CONFIG_ID:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.AWS);
        notNullCheck(
            "Aws Cloud Provider [" + variableValue + "] associated to the Cloud Formation State does not exist",
            uuidAccess, USER);
        break;
      case HELM_GIT_CONFIG_ID:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.GIT);
        notNullCheck(
            "Git Connector [" + variableValue + "] associated to the Helm State does not exist", uuidAccess, USER);
        break;
      case SS_SSH_CONNECTION_ATTRIBUTE:
        uuidAccess = settingsService.fetchSettingAttributeByName(
            accountId, variableValue, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES);
        notNullCheck(
            "Ssh connection attribute [" + variableValue + "] associated to the Shell Script State does not exist",
            uuidAccess, USER);
        break;
      case USER_GROUP:
        uuidAccess = userGroupService.fetchUserGroupByName(accountId, variableValue);
        notNullCheck(
            "userGroup [" + variableValue + "] associated to the Approval State does not exist", uuidAccess, USER);
        break;
      case SS_WINRM_CONNECTION_ATTRIBUTE:
        uuidAccess = settingsService.fetchSettingAttributeByName(
            accountId, variableValue, SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES);
        notNullCheck(
            "Winrm connection attribute [" + variableValue + "] associated to the Shell Script State does not exist",
            uuidAccess, USER);
        break;
      case GCP_CONFIG:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.GCP);
        notNullCheck(
            "Google Cloud Provider [" + variableValue + "] associated to Google Cloud Build State does not exist",
            uuidAccess, USER);
        break;
      case GIT_CONFIG:
        uuidAccess = settingsService.fetchSettingAttributeByName(accountId, variableValue, SettingVariableTypes.GIT);
        notNullCheck("Git connector [" + variableValue + "] associated to Google Cloud Build State does not exist",
            uuidAccess, USER);
        break;
      default:
        return null;
    }

    return uuidAccess;
  }
}
