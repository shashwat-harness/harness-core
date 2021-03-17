package io.harness.accesscontrol;

import io.harness.accesscontrol.principals.PrincipalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "PrincipalKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HPrincipal implements Principal {
  @NotEmpty String principalIdentifier;
  @NotNull PrincipalType principalType;
}
