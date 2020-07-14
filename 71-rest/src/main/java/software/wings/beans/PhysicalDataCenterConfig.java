package software.wings.beans;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.audit.ResourceType;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.List;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER")
public class PhysicalDataCenterConfig extends SettingValue {
  /**
   * Instantiates a new setting value.
   */
  public PhysicalDataCenterConfig() {
    super(SettingVariableTypes.PHYSICAL_DATA_CENTER.name());
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return emptyList();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String type;

    private Builder() {}

    /**
     * A physical data center config builder.
     *
     * @return the builder
     */
    public static Builder aPhysicalDataCenterConfig() {
      return new Builder();
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPhysicalDataCenterConfig().withType(type);
    }

    /**
     * Build physical data center config.
     *
     * @return the physical data center config
     */
    public PhysicalDataCenterConfig build() {
      PhysicalDataCenterConfig physicalDataCenterConfig = new PhysicalDataCenterConfig();
      physicalDataCenterConfig.setType(type);
      return physicalDataCenterConfig;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
    }
  }
}
