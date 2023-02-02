/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.BAD;
import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.GOOD;
import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.NO_DATA;
import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.SKIP_DATA;
import static io.harness.rule.OwnerRule.VARSHA_LALWANI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.CVNGTestConstants;
import io.harness.cvng.downtime.beans.EntityUnavailabilityStatusesDTO;
import io.harness.cvng.downtime.services.api.DowntimeService;
import io.harness.cvng.downtime.services.api.EntityUnavailabilityStatusesService;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordParam;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.SLIDataUnavailabilityFilterService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SLIDataUnavailabilityFilterServiceImplTest extends CvNextGenTestBase {
  @Inject SLIDataUnavailabilityFilterService sliDataUnavailabilityFilterService;

  @Mock EntityUnavailabilityStatusesService entityUnavailabilityStatusesService;

  @Mock DowntimeService downtimeService;

  Clock clock;

  BuilderFactory builderFactory;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    clock = CVNGTestConstants.FIXED_TIME_FOR_TESTS;
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testFilter() {
    Instant startTime = clock.instant();
    List<SLIState> sliStates = Arrays.asList(BAD, GOOD, SKIP_DATA, NO_DATA, GOOD, GOOD, BAD, BAD, BAD, SKIP_DATA);
    List<SLIRecordParam> sliRecordParams = getSLIRecordParam(startTime, sliStates);
    EntityUnavailabilityStatusesDTO downtimeEntityUnavailabilityStatusesDTO =
        builderFactory.getDowntimeEntityUnavailabilityStatusesDTO();
    EntityUnavailabilityStatusesDTO sloEntityUnavailabilityStatusesDTO =
        builderFactory.getSLOEntityUnavailabilityStatusesDTO();
    List<EntityUnavailabilityStatusesDTO> statusesDTOS =
        List.of(downtimeEntityUnavailabilityStatusesDTO, sloEntityUnavailabilityStatusesDTO);
    doReturn(statusesDTOS)
        .when(entityUnavailabilityStatusesService)
        .getAllInstances(builderFactory.getProjectParams(), startTime.getEpochSecond(),
            startTime.getEpochSecond() + Duration.ofMinutes(10).toSeconds());
    doReturn(Collections.singletonList(downtimeEntityUnavailabilityStatusesDTO))
        .when(downtimeService)
        .filterDowntimeInstancesOnMonitoredService(builderFactory.getProjectParams(), statusesDTOS, "msIdentifier");
    List<SLIRecordParam> updatedSliRecordParams =
        sliDataUnavailabilityFilterService.filterSLIRecordsToSkip(sliRecordParams, builderFactory.getProjectParams(),
            startTime.getEpochSecond(), startTime.getEpochSecond() + Duration.ofMinutes(10).toSeconds(), "msIdentifier",
            sloEntityUnavailabilityStatusesDTO.getEntityId());
    assertThat(updatedSliRecordParams.size()).isEqualTo(10);
    assertThat(updatedSliRecordParams.get(9).getSliState()).isEqualTo(SKIP_DATA);
  }

  private List<SLIRecordParam> getSLIRecordParam(Instant startTime, List<SLIState> sliStates) {
    List<SLIRecordParam> sliRecordParams = new ArrayList<>();
    for (int i = 0; i < sliStates.size(); i++) {
      SLIState sliState = sliStates.get(i);
      sliRecordParams.add(
          SLIRecordParam.builder().sliState(sliState).timeStamp(startTime.plus(Duration.ofMinutes(i))).build());
    }
    return sliRecordParams;
  }
}