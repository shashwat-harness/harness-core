package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlSchemaUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YamlSchemaUtils.class, IOUtils.class})
public class YamlSchemaHelperTest extends CategoryTest {
  YamlSchemaHelper yamlSchemaHelper;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void initializeSchemaMapAndGetSchema() throws IOException {
    yamlSchemaHelper = new YamlSchemaHelper();
    String schema = getResource("testSchema/testOutputSchema.json");
    mockStatic(YamlSchemaUtils.class);
    mockStatic(IOUtils.class);
    Set<Class<?>> classes = new HashSet<>();
    classes.add(TestClass.ClassWhichContainsInterface.class);
    when(YamlSchemaUtils.getClasses(any(), any())).thenReturn(classes);
    when(IOUtils.resourceToString(any(), any(), any())).thenReturn(schema);

    yamlSchemaHelper.initializeSchemaMaps(null);
    final String schemaForEntityType = yamlSchemaHelper.getSchemaForEntityType(EntityType.CONNECTORS);
    assertThat(schemaForEntityType).isNotNull();
    assertThat(schemaForEntityType).isEqualTo(schema);
  }

  private String getResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, this.getClass().getClassLoader());
  }
}