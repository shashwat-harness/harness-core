package io.harness.delegate.task.shell;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static java.util.Collections.emptyList;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShellScriptApprovalTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;

  private ScriptType scriptType;
  private final String outputVars;
  @Expression(ALLOW_SECRETS) private final String script;
  private String workingDirectory;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return emptyList();
  }
}
