package io.harness.cdng.pipeline.executions.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactTypes;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(ArtifactTypes.DOCKER_ARTIFACT)
public class DockerArtifactSummary implements ArtifactSummary {
  String imagePath;
  String tag;

  @Override
  public String getType() {
    return ArtifactTypes.DOCKER_ARTIFACT;
  }
}
