/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote.licenserestriction;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;

import com.google.inject.Inject;

@OwnedBy(PL)
public class ServiceAccountRestrictionUsageImpl
    implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject ServiceAccountService serviceAccountService;

  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return serviceAccountService.countServiceAccounts(accountIdentifier);
  }
}