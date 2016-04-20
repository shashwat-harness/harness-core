package software.wings.waitNotify;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Rishi
 */
@Embedded
@Entity(value = "notifyResponses", noClassnameStored = true)
public class NotifyResponse<T extends Serializable> extends Base {
  @Serialized private T response;

  @Indexed private Date expiryTs;

  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  public NotifyResponse() {}

  public NotifyResponse(String correlationId, T response) {
    setUuid(correlationId);
    setResponse(response);
  }

  public T getResponse() {
    return response;
  }

  public void setResponse(T response) {
    this.response = response;
  }

  public Date getExpiryTs() {
    return expiryTs;
  }

  public void setExpiryTs(Date expiryTs) {
    this.expiryTs = expiryTs;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }
}
