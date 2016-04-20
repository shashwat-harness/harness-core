/**
 *
 */
package software.wings.waitNotify;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Rishi
 */
@Entity(value = "waitInstanceErrors", noClassnameStored = true)
public class WaitInstanceError extends Base {
  private String waitInstanceId;

  @Serialized private Map<String, Serializable> responseMap;

  private String errorStackTrace;

  public String getWaitInstanceId() {
    return waitInstanceId;
  }

  public void setWaitInstanceId(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  public Map<String, Serializable> getResponseMap() {
    return responseMap;
  }

  public void setResponseMap(Map<String, Serializable> responseMap) {
    this.responseMap = responseMap;
  }

  public String getErrorStackTrace() {
    return errorStackTrace;
  }

  public void setErrorStackTrace(String errorStackTrace) {
    this.errorStackTrace = errorStackTrace;
  }
}
