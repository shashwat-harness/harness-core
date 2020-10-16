package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "timeSeriesGroupValues"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "TimeSeriesRecordKeys")
@Entity(value = "timeSeriesRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesRecord implements CreatedAtAware, AccountAccess, PersistentEntity, Comparable<TimeSeriesRecord> {
  @Id private String uuid;
  @FdIndex private String accountId;
  @FdIndex private String cvConfigId;
  @FdIndex private String verificationTaskId;
  @FdIndex private String host;
  @FdIndex private String metricName;
  private double riskScore;
  private Instant bucketStartTime;

  private long createdAt;
  @Default private Set<TimeSeriesGroupValue> timeSeriesGroupValues = new HashSet<>();

  @JsonIgnore
  @SchemaIgnore
  @Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(180).toInstant());

  @Override
  public int compareTo(@NotNull TimeSeriesRecord o) {
    if (!bucketStartTime.equals(o.bucketStartTime)) {
      return bucketStartTime.compareTo(o.bucketStartTime);
    }
    return metricName.compareTo(o.metricName);
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TimeSeriesValueKeys")
  @EqualsAndHashCode(of = {"groupName", "timeStamp"})
  public static class TimeSeriesGroupValue implements Comparable<TimeSeriesGroupValue> {
    private String groupName;
    private Instant timeStamp;
    private double metricValue;
    private double riskScore;

    @Override
    public int compareTo(TimeSeriesGroupValue other) {
      return timeStamp.compareTo(other.getTimeStamp());
    }
  }
}
