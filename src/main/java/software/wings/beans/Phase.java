package software.wings.beans;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(value = "phases", noClassnameStored = true)
public class Phase {
  @Id private ObjectId id;

  private String name;
  private String description;
  private String compName;
  private String envName;
  private Map<String, List<String>> hostInstances = new HashMap<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCompName() {
    return compName;
  }

  public void setCompName(String compName) {
    this.compName = compName;
  }

  public Map<String, List<String>> getHostInstances() {
    return hostInstances;
  }

  public void setHostInstances(Map<String, List<String>> hostInstances) {
    this.hostInstances = hostInstances;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }
}
