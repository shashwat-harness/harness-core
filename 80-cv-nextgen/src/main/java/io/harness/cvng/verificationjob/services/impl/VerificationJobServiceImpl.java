package io.harness.cvng.verificationjob.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;

public class VerificationJobServiceImpl implements VerificationJobService {
  @Inject private HPersistence hPersistence;

  @Override
  public VerificationJobDTO get(String accountId, String identifier) {
    VerificationJob verificationJob = findVerificationJob(accountId, identifier);
    if (verificationJob == null) {
      return null;
    }
    return verificationJob.getVerificationJobDTO();
  }

  @Override
  public void upsert(String accountId, VerificationJobDTO verificationJobDTO) {
    VerificationJob verificationJob = verificationJobDTO.getVerificationJob();
    verificationJob.setAccountId(accountId);
    VerificationJob stored = findVerificationJob(accountId, verificationJobDTO.getIdentifier());
    if (stored != null) {
      verificationJob.setUuid(stored.getUuid());
    }
    verificationJob.validate();
    // TODO: Keeping it simple for now. find a better way to save if more fields are added to verification Job. This can
    // potentially override them.
    hPersistence.save(verificationJob);
  }

  private VerificationJob findVerificationJob(String accountId, String identifier) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(identifier);
    return hPersistence.createQuery(VerificationJob.class)
        .filter(VerificationJobKeys.accountId, accountId)
        .filter(VerificationJobKeys.identifier, identifier)
        .get();
  }

  @Override
  public void delete(String accountId, String identifier) {
    hPersistence.delete(hPersistence.createQuery(VerificationJob.class)
                            .filter(VerificationJobKeys.accountId, accountId)
                            .filter(VerificationJobKeys.identifier, identifier));
  }
}
