package software.wings.beans;

public class FileUploadSource extends ArtifactSource {
  public FileUploadSource() {
    super(SourceType.FILE_UPLOAD);
  }

  @Override
  public ArtifactFile collect(Object[] params) {
    return null;
  }

  public static class Builder {
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    public Builder but() {
      return aFileUploadSource().withSourceName(sourceName).withArtifactType(artifactType);
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public static Builder aFileUploadSource() {
      return new Builder();
    }

    /**
     * creates a FileUploadSource Object.
     * @return FileUploadSource object.
     */
    public FileUploadSource build() {
      FileUploadSource fileUploadSource = new FileUploadSource();
      fileUploadSource.setSourceName(sourceName);
      fileUploadSource.setArtifactType(artifactType);
      return fileUploadSource;
    }
  }
}
