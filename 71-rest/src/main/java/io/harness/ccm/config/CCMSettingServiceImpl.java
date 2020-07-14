package io.harness.ccm.config;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

@Slf4j
@Singleton
public class CCMSettingServiceImpl implements CCMSettingService {
  private AccountService accountService;
  private SettingsService settingsService;

  @Inject
  public CCMSettingServiceImpl(AccountService accountService, SettingsService settingsService) {
    this.accountService = accountService;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isCloudCostEnabled(String accountId) {
    Account account = accountService.get(accountId);
    return account.isCloudCostEnabled();
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(String accountId) {
    return accountService.get(accountId).isCeAutoCollectK8sEvents();
  }

  @Override
  public boolean isCloudCostEnabled(SettingAttribute settingAttribute) {
    if (isCloudCostEnabled(settingAttribute.getAccountId())) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (null != ccmConfig) {
        return ccmConfig.isCloudCostEnabled();
      }
    }
    return false;
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(SettingAttribute settingAttribute) {
    if (isCeK8sEventCollectionEnabled(settingAttribute.getAccountId())) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (null != ccmConfig) {
        return settingAttribute.getValue().getType().equals(SettingVariableTypes.KUBERNETES_CLUSTER.name())
            && !ccmConfig.isSkipK8sEventCollection();
      }
    } else {
      return isCloudCostEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public boolean isCloudCostEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      logger.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }
    if (settingAttribute.getValue() instanceof CloudCostAware) {
      return isCloudCostEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public boolean isCeK8sEventCollectionEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      logger.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }

    if (settingAttribute.getValue() instanceof CloudCostAware) {
      return isCeK8sEventCollectionEnabled(settingAttribute);
    }
    return false;
  }

  @Override
  public void maskCCMConfig(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (!account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      value.setCcmConfig(null);
      settingAttribute.setValue((SettingValue) value);
    }
  }
}
