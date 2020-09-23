package io.harness.engine.executions.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.PlanExecution;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PlanExecutionServiceImplTest extends OrchestrationTest {
  @Inject PlanExecutionService planExecutionService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String planExecutionId = generateUuid();
    PlanExecution savedExecution = planExecutionService.save(PlanExecution.builder().uuid(planExecutionId).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);
    assertThat(savedExecution.getCreatedAt()).isNotNull();
    assertThat(savedExecution.getVersion()).isEqualTo(0);
  }
}