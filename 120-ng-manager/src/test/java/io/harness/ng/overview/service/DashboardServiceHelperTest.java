/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.service;

import static io.harness.rule.OwnerRule.ABHISHEK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.models.ActiveServiceInstanceInfoWithEnvType;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.overview.dto.InstanceGroupedByEnvironmentList;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DashboardServiceHelperTest {
  private static final String ENV_1 = "env1";
  private static final String ENV_2 = "env2";
  private static final String INFRA_1 = "infra1";
  private static final String INFRA_2 = "infra2";
  private static final String DISPLAY_NAME_1 = "displayName1";
  private static final String DISPLAY_NAME_2 = "displayName2";

  private Map<String, String> envIdToNameMap;
  private Map<String, String> infraIdToNameMap;
  private Map<String, Map<EnvironmentType, Map<String, Map<String, Pair<Integer, Long>>>>> instanceCountMap;

  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact> instanceGroupedByArtifactList1;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact> instanceGroupedByArtifactList2;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact> instanceGroupedByArtifactList3;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact> instanceGroupedByArtifactList4;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByInfrastructureList1;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByInfrastructureList2;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByInfrastructureList3;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByClusterList1;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByClusterList2;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByClusterList3;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType> instanceGroupedByEnvironmentTypeList1;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType> instanceGroupedByEnvironmentTypeList2;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType>
      instanceGroupedByEnvironmentTypeListGitOps1;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType>
      instanceGroupedByEnvironmentTypeListGitOps2;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment> instanceGroupedByEnvironmentList;
  private List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment> instanceGroupedByEnvironmentListGitOps;

  @Before
  public void setup() {
    instanceGroupedByArtifactList1 = new ArrayList<>();
    instanceGroupedByArtifactList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact.builder()
                                           .artifact(DISPLAY_NAME_2)
                                           .lastDeployedAt(2l)
                                           .count(1)
                                           .build());
    instanceGroupedByArtifactList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact.builder()
                                           .artifact(DISPLAY_NAME_1)
                                           .lastDeployedAt(1l)
                                           .count(1)
                                           .build());
    instanceGroupedByArtifactList2 = new ArrayList<>();
    instanceGroupedByArtifactList2.add(InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact.builder()
                                           .artifact(DISPLAY_NAME_1)
                                           .lastDeployedAt(3l)
                                           .count(1)
                                           .build());
    instanceGroupedByArtifactList3 = new ArrayList<>();
    instanceGroupedByArtifactList3.add(InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact.builder()
                                           .artifact(DISPLAY_NAME_1)
                                           .lastDeployedAt(4l)
                                           .count(1)
                                           .build());
    instanceGroupedByArtifactList4 = new ArrayList<>();
    instanceGroupedByArtifactList4.add(InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact.builder()
                                           .artifact(DISPLAY_NAME_1)
                                           .lastDeployedAt(5l)
                                           .count(1)
                                           .build());

    instanceGroupedByInfrastructureList1 = new ArrayList<>();
    instanceGroupedByInfrastructureList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                                 .infrastructureId(INFRA_2)
                                                 .infrastructureName(INFRA_2)
                                                 .lastDeployedAt(3l)
                                                 .instanceGroupedByArtifactList(instanceGroupedByArtifactList2)
                                                 .build());
    instanceGroupedByInfrastructureList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                                 .infrastructureId(INFRA_1)
                                                 .infrastructureName(INFRA_1)
                                                 .lastDeployedAt(2l)
                                                 .instanceGroupedByArtifactList(instanceGroupedByArtifactList1)
                                                 .build());
    instanceGroupedByInfrastructureList2 = new ArrayList<>();
    instanceGroupedByInfrastructureList2.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                                 .infrastructureId(INFRA_1)
                                                 .infrastructureName(INFRA_1)
                                                 .lastDeployedAt(4l)
                                                 .instanceGroupedByArtifactList(instanceGroupedByArtifactList3)
                                                 .build());
    instanceGroupedByInfrastructureList3 = new ArrayList<>();
    instanceGroupedByInfrastructureList3.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                                 .infrastructureId(INFRA_1)
                                                 .infrastructureName(INFRA_1)
                                                 .lastDeployedAt(5l)
                                                 .instanceGroupedByArtifactList(instanceGroupedByArtifactList4)
                                                 .build());

    instanceGroupedByClusterList1 = new ArrayList<>();
    instanceGroupedByClusterList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                          .clusterId(INFRA_2)
                                          .agentId(INFRA_2)
                                          .lastDeployedAt(3l)
                                          .instanceGroupedByArtifactList(instanceGroupedByArtifactList2)
                                          .build());
    instanceGroupedByClusterList1.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                          .clusterId(INFRA_1)
                                          .agentId(INFRA_1)
                                          .lastDeployedAt(2l)
                                          .instanceGroupedByArtifactList(instanceGroupedByArtifactList1)
                                          .build());
    instanceGroupedByClusterList2 = new ArrayList<>();
    instanceGroupedByClusterList2.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                          .clusterId(INFRA_1)
                                          .agentId(INFRA_1)
                                          .lastDeployedAt(4l)
                                          .instanceGroupedByArtifactList(instanceGroupedByArtifactList3)
                                          .build());
    instanceGroupedByClusterList3 = new ArrayList<>();
    instanceGroupedByClusterList3.add(InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure.builder()
                                          .clusterId(INFRA_1)
                                          .agentId(INFRA_1)
                                          .lastDeployedAt(5l)
                                          .instanceGroupedByArtifactList(instanceGroupedByArtifactList4)
                                          .build());

    instanceGroupedByEnvironmentTypeList1 = new ArrayList<>();
    instanceGroupedByEnvironmentTypeList1.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.Production)
            .lastDeployedAt(4l)
            .instanceGroupedByInfrastructureList(instanceGroupedByInfrastructureList2)
            .build());
    instanceGroupedByEnvironmentTypeList1.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.PreProduction)
            .lastDeployedAt(3l)
            .instanceGroupedByInfrastructureList(instanceGroupedByInfrastructureList1)
            .build());
    instanceGroupedByEnvironmentTypeList2 = new ArrayList<>();
    instanceGroupedByEnvironmentTypeList2.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.Production)
            .lastDeployedAt(5l)
            .instanceGroupedByInfrastructureList(instanceGroupedByInfrastructureList3)
            .build());

    instanceGroupedByEnvironmentTypeListGitOps1 = new ArrayList<>();
    instanceGroupedByEnvironmentTypeListGitOps1.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.Production)
            .lastDeployedAt(4l)
            .instanceGroupedByInfrastructureList(instanceGroupedByClusterList2)
            .build());
    instanceGroupedByEnvironmentTypeListGitOps1.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.PreProduction)
            .lastDeployedAt(3l)
            .instanceGroupedByInfrastructureList(instanceGroupedByClusterList1)
            .build());
    instanceGroupedByEnvironmentTypeListGitOps2 = new ArrayList<>();
    instanceGroupedByEnvironmentTypeListGitOps2.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType.builder()
            .environmentType(EnvironmentType.Production)
            .lastDeployedAt(5l)
            .instanceGroupedByInfrastructureList(instanceGroupedByClusterList3)
            .build());

    instanceGroupedByEnvironmentList = new ArrayList<>();
    instanceGroupedByEnvironmentList.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment.builder()
            .envId(ENV_2)
            .instanceGroupedByEnvironmentTypeList(instanceGroupedByEnvironmentTypeList2)
            .envName(ENV_2)
            .lastDeployedAt(5l)
            .build());
    instanceGroupedByEnvironmentList.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment.builder()
            .envId(ENV_1)
            .instanceGroupedByEnvironmentTypeList(instanceGroupedByEnvironmentTypeList1)
            .envName(ENV_1)
            .lastDeployedAt(4l)
            .build());

    instanceGroupedByEnvironmentListGitOps = new ArrayList<>();
    instanceGroupedByEnvironmentListGitOps.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment.builder()
            .envId(ENV_2)
            .instanceGroupedByEnvironmentTypeList(instanceGroupedByEnvironmentTypeListGitOps2)
            .envName(ENV_2)
            .lastDeployedAt(5l)
            .build());
    instanceGroupedByEnvironmentListGitOps.add(
        InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment.builder()
            .envId(ENV_1)
            .instanceGroupedByEnvironmentTypeList(instanceGroupedByEnvironmentTypeListGitOps1)
            .envName(ENV_1)
            .lastDeployedAt(4l)
            .build());

    envIdToNameMap = new HashMap<>();
    envIdToNameMap.put(ENV_1, ENV_1);
    envIdToNameMap.put(ENV_2, ENV_2);

    infraIdToNameMap = new HashMap<>();
    infraIdToNameMap.put(INFRA_1, INFRA_1);
    infraIdToNameMap.put(INFRA_2, INFRA_2);

    instanceCountMap = getInstanceCountMap();
  }

  private List<ActiveServiceInstanceInfoWithEnvType> getActiveServiceInstanceInfoWithEnvTypeListNonGitOps() {
    List<ActiveServiceInstanceInfoWithEnvType> activeServiceInstanceInfoWithEnvTypeList = new ArrayList<>();
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, INFRA_1, INFRA_1, null, null, 1l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, INFRA_1, INFRA_1, null, null, 2l, DISPLAY_NAME_2, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, INFRA_2, INFRA_2, null, null, 3l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.Production, INFRA_1, INFRA_1, null, null, 4l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_2, ENV_2, EnvironmentType.Production, INFRA_1, INFRA_1, null, null, 5l, DISPLAY_NAME_1, 1));
    return activeServiceInstanceInfoWithEnvTypeList;
  }

  private List<ActiveServiceInstanceInfoWithEnvType> getActiveServiceInstanceInfoWithEnvTypeListGitOps() {
    List<ActiveServiceInstanceInfoWithEnvType> activeServiceInstanceInfoWithEnvTypeList = new ArrayList<>();
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, null, null, INFRA_1, INFRA_1, 1l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, null, null, INFRA_1, INFRA_1, 2l, DISPLAY_NAME_2, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.PreProduction, null, null, INFRA_2, INFRA_2, 3l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_1, ENV_1, EnvironmentType.Production, null, null, INFRA_1, INFRA_1, 4l, DISPLAY_NAME_1, 1));
    activeServiceInstanceInfoWithEnvTypeList.add(new ActiveServiceInstanceInfoWithEnvType(
        ENV_2, ENV_2, EnvironmentType.Production, null, null, INFRA_1, INFRA_1, 5l, DISPLAY_NAME_1, 1));
    return activeServiceInstanceInfoWithEnvTypeList;
  }

  private Map<String, Map<EnvironmentType, Map<String, Map<String, Pair<Integer, Long>>>>> getInstanceCountMap() {
    Map<String, Pair<Integer, Long>> buildToCountMap1 = new HashMap<>();
    buildToCountMap1.put(DISPLAY_NAME_1, MutablePair.of(1, 1l));
    buildToCountMap1.put(DISPLAY_NAME_2, MutablePair.of(1, 2l));
    Map<String, Pair<Integer, Long>> buildToCountMap2 = new HashMap<>();
    buildToCountMap2.put(DISPLAY_NAME_1, MutablePair.of(1, 3l));
    Map<String, Pair<Integer, Long>> buildToCountMap3 = new HashMap<>();
    buildToCountMap3.put(DISPLAY_NAME_1, MutablePair.of(1, 4l));
    Map<String, Pair<Integer, Long>> buildToCountMap4 = new HashMap<>();
    buildToCountMap4.put(DISPLAY_NAME_1, MutablePair.of(1, 5l));

    Map<String, Map<String, Pair<Integer, Long>>> infraToBuildMap1 = new HashMap<>();
    infraToBuildMap1.put(INFRA_1, buildToCountMap1);
    infraToBuildMap1.put(INFRA_2, buildToCountMap2);
    Map<String, Map<String, Pair<Integer, Long>>> infraToBuildMap2 = new HashMap<>();
    infraToBuildMap2.put(INFRA_1, buildToCountMap3);
    Map<String, Map<String, Pair<Integer, Long>>> infraToBuildMap3 = new HashMap<>();
    infraToBuildMap3.put(INFRA_1, buildToCountMap4);

    Map<EnvironmentType, Map<String, Map<String, Pair<Integer, Long>>>> environmentTypeToInfraMap1 = new HashMap<>();
    environmentTypeToInfraMap1.put(EnvironmentType.PreProduction, infraToBuildMap1);
    environmentTypeToInfraMap1.put(EnvironmentType.Production, infraToBuildMap2);
    Map<EnvironmentType, Map<String, Map<String, Pair<Integer, Long>>>> environmentTypeToInfraMap2 = new HashMap<>();
    environmentTypeToInfraMap2.put(EnvironmentType.Production, infraToBuildMap3);

    Map<String, Map<EnvironmentType, Map<String, Map<String, Pair<Integer, Long>>>>> environmentToTypeMap =
        new HashMap<>();
    environmentToTypeMap.put(ENV_1, environmentTypeToInfraMap1);
    environmentToTypeMap.put(ENV_2, environmentTypeToInfraMap2);

    return environmentToTypeMap;
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_getInstanceGroupedByEnvironmentListHelper_NonGitOps() {
    InstanceGroupedByEnvironmentList instanceGroupedByEnvironmentList1 =
        DashboardServiceHelper.getInstanceGroupedByEnvironmentListHelper(
            getActiveServiceInstanceInfoWithEnvTypeListNonGitOps(), false);
    InstanceGroupedByEnvironmentList instanceGroupedByEnvironmentList2 =
        InstanceGroupedByEnvironmentList.builder()
            .instanceGroupedByEnvironmentList(instanceGroupedByEnvironmentList)
            .build();
    assertThat(instanceGroupedByEnvironmentList1).isEqualTo(instanceGroupedByEnvironmentList2);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_getInstanceGroupedByEnvironmentListHelper_GitOps() {
    InstanceGroupedByEnvironmentList instanceGroupedByEnvironmentList1 =
        DashboardServiceHelper.getInstanceGroupedByEnvironmentListHelper(
            getActiveServiceInstanceInfoWithEnvTypeListGitOps(), true);
    InstanceGroupedByEnvironmentList instanceGroupedByEnvironmentList2 =
        InstanceGroupedByEnvironmentList.builder()
            .instanceGroupedByEnvironmentList(instanceGroupedByEnvironmentListGitOps)
            .build();
    assertThat(instanceGroupedByEnvironmentList1).isEqualTo(instanceGroupedByEnvironmentList2);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupByEnvironment_NonGitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment> instanceGroupedByEnvironmentList1 =
        DashboardServiceHelper.groupByEnvironment(instanceCountMap, infraIdToNameMap, envIdToNameMap, false);
    assertThat(instanceGroupedByEnvironmentList1).isEqualTo(instanceGroupedByEnvironmentList);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupByEnvironment_GitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironment> instanceGroupedByEnvironmentList1 =
        DashboardServiceHelper.groupByEnvironment(instanceCountMap, infraIdToNameMap, envIdToNameMap, true);
    assertThat(instanceGroupedByEnvironmentList1).isEqualTo(instanceGroupedByEnvironmentListGitOps);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupedByEnvironmentTypes_NonGitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType> instanceGroupedByEnvironmentTypeListResult =
        DashboardServiceHelper.groupedByEnvironmentTypes(instanceCountMap.get(ENV_1), infraIdToNameMap, false);
    assertThat(instanceGroupedByEnvironmentTypeListResult).isEqualTo(instanceGroupedByEnvironmentTypeList1);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupedByEnvironmentTypes_GitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByEnvironmentType> instanceGroupedByEnvironmentTypeListResult =
        DashboardServiceHelper.groupedByEnvironmentTypes(instanceCountMap.get(ENV_1), infraIdToNameMap, true);
    assertThat(instanceGroupedByEnvironmentTypeListResult).isEqualTo(instanceGroupedByEnvironmentTypeListGitOps1);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupedByInfrastructure_NonGitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByInfrastructureResult =
        DashboardServiceHelper.groupedByInfrastructures(
            instanceCountMap.get(ENV_1).get(EnvironmentType.PreProduction), infraIdToNameMap, false);
    assertThat(instanceGroupedByInfrastructureResult).isEqualTo(instanceGroupedByInfrastructureList1);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupedByInfrastructure_GitOps() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByInfrastructure> instanceGroupedByInfrastructureResult =
        DashboardServiceHelper.groupedByInfrastructures(
            instanceCountMap.get(ENV_1).get(EnvironmentType.PreProduction), infraIdToNameMap, true);
    assertThat(instanceGroupedByInfrastructureResult).isEqualTo(instanceGroupedByClusterList1);
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void test_groupedByArtifacts() {
    List<InstanceGroupedByEnvironmentList.InstanceGroupedByArtifact> instanceGroupedByArtifactListResult =
        DashboardServiceHelper.groupedByArtifacts(
            instanceCountMap.get(ENV_1).get(EnvironmentType.PreProduction).get(INFRA_1));
    assertThat(instanceGroupedByArtifactListResult).isEqualTo(instanceGroupedByArtifactList1);
  }
}