package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.CE_BATCH)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBatchLastProcessedData implements QLObject {
  long lastProcessedTime;
}
