package io.harness.delegate.task.k8s;

public enum K8sTaskType {
  DEPLOYMENT_ROLLING,
  DEPLOYMENT_ROLLING_ROLLBACK,
  SCALE,
  CANARY_DEPLOY,
  BLUE_GREEN_DEPLOY,
  INSTANCE_SYNC,
  DELETE,
  TRAFFIC_SPLIT,
  APPLY,
  VERSION
}
