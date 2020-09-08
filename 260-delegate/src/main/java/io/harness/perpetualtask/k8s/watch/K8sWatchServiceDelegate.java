package io.harness.perpetualtask.k8s.watch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.informer.SharedInformerFactoryFactory;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import io.harness.serializer.KryoSerializer;
import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Store;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class K8sWatchServiceDelegate {
  private static final Map<String, Class<?>> KNOWN_WORKLOAD_TYPES = ImmutableMap.<String, Class<?>>builder()
                                                                        .put("Deployment", V1Deployment.class)
                                                                        .put("ReplicaSet", V1ReplicaSet.class)
                                                                        .put("DaemonSet", V1DaemonSet.class)
                                                                        .put("StatefulSet", V1StatefulSet.class)
                                                                        .put("Job", V1Job.class)
                                                                        .put("CronJob", V1beta1CronJob.class)
                                                                        .build();

  private final WatcherFactory watcherFactory;
  private final SharedInformerFactoryFactory sharedInformerFactoryFactory;
  private final ApiClientFactory apiClientFactory;

  private final Map<String, WatcherGroup> watchMap; // <id, Watch>
  private final KryoSerializer kryoSerializer; // <id, Watch>
  private final ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Inject
  public K8sWatchServiceDelegate(WatcherFactory watcherFactory,
      SharedInformerFactoryFactory sharedInformerFactoryFactory, ApiClientFactory apiClientFactory,
      KryoSerializer kryoSerializer, ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper) {
    this.watcherFactory = watcherFactory;
    this.sharedInformerFactoryFactory = sharedInformerFactoryFactory;
    this.apiClientFactory = apiClientFactory;
    this.watchMap = new ConcurrentHashMap<>();
    this.kryoSerializer = kryoSerializer;
    this.containerDeploymentDelegateHelper = containerDeploymentDelegateHelper;
  }

  @Value
  @Builder
  static class WatcherGroup implements Closeable {
    String watchId;
    SharedInformerFactory sharedInformerFactory;

    @Override
    public void close() {
      logger.info("Closing AllRegisteredInformers for watch {}", watchId);
      sharedInformerFactory.stopAllRegisteredInformers();
    }
  }

  Iterable<String> watchIds() {
    return watchMap.keySet();
  }

  public String create(K8sWatchTaskParams params) {
    String watchId = params.getClusterId();
    watchMap.computeIfAbsent(watchId, id -> {
      logger.info("Creating watch with id: {}", id);

      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) kryoSerializer.asObject(params.getK8SClusterConfig().toByteArray());
      KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);

      ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig);
      DefaultK8sMetricsClient k8sMetricsClient = new DefaultK8sMetricsClient(apiClient);

      String kubeSystemUid = getKubeSystemUid(k8sMetricsClient);
      ClusterDetails clusterDetails = ClusterDetails.builder()
                                          .cloudProviderId(params.getCloudProviderId())
                                          .clusterId(params.getClusterId())
                                          .clusterName(params.getClusterName())
                                          .kubeSystemUid(kubeSystemUid)
                                          .build();

      SharedInformerFactory sharedInformerFactory =
          sharedInformerFactoryFactory.createSharedInformerFactory(apiClient, clusterDetails);

      PVCFetcher pvcFetcher = watcherFactory.createPVCFetcher(apiClient, sharedInformerFactory);
      SharedInformer<V1PersistentVolumeClaim> pvcInformer =
          sharedInformerFactory.getExistingSharedIndexInformer(V1PersistentVolumeClaim.class);
      pvcInformer.run();

      NamespaceFetcher namespaceFetcher = watcherFactory.createNamespaceFetcher(apiClient, sharedInformerFactory);
      SharedInformer<V1Namespace> namespaceInformer =
          sharedInformerFactory.getExistingSharedIndexInformer(V1Namespace.class);
      namespaceInformer.run();

      Map<String, Store<?>> stores = KNOWN_WORKLOAD_TYPES.entrySet().stream().collect(Collectors.toMap(
          Map.Entry::getKey, e -> sharedInformerFactory.getExistingSharedIndexInformer(e.getValue()).getIndexer()));

      K8sControllerFetcher controllerFetcher = new K8sControllerFetcher(stores);

      watcherFactory.createNodeWatcher(apiClient, clusterDetails, sharedInformerFactory);
      watcherFactory.createPVWatcher(apiClient, clusterDetails, sharedInformerFactory);

      blockingWaitForFetchersToSync(ImmutableList.of(pvcInformer, namespaceInformer));
      watcherFactory.createPodWatcher(
          apiClient, clusterDetails, controllerFetcher, sharedInformerFactory, pvcFetcher, namespaceFetcher);

      logger.info("Starting AllRegisteredInformers for watch {}", id);
      sharedInformerFactory.startAllRegisteredInformers();

      return WatcherGroup.builder().watchId(id).sharedInformerFactory(sharedInformerFactory).build();
    });

    return watchId;
  }

  @SneakyThrows
  private void blockingWaitForFetchersToSync(ImmutableList<SharedInformer<?>> informers) {
    int cnt = 0;
    while (!informers.stream().map(SharedInformer::hasSynced).filter(i -> !i).findFirst().orElse(Boolean.TRUE)
        && ++cnt <= 25) {
      logger.info("Waiting for InformerFetchers to sync... {}", cnt);
      Thread.sleep(300);
    }
  }

  public static String getKubeSystemUid(DefaultK8sMetricsClient client) {
    try {
      return client.readNamespace("kube-system", null, null, null).getMetadata().getUid();
    } catch (Exception e) {
      logger.warn("Error getting kube-system namespace uid", e);
      return "kube-system";
    }
  }

  public void delete(String watchId) {
    watchMap.computeIfPresent(watchId, (id, watcherGroup) -> {
      logger.info("Deleting watch with id: {}", watchId);
      watcherGroup.close();
      return null;
    });
  }
}
