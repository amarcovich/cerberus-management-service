/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.CleanUpRequest;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.nike.cerberus.service.KmsService.SOONEST_A_KMS_KEY_CAN_BE_DELETED;

/**
 * Service to clean up inactive and orphaned KMS keys
 */
@Singleton
public class CleanUpService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_SLEEP_BETWEEN_KMS_CALLS = 10;  // in seconds

    private static final int DEFAULT_KMS_KEY_INACTIVE_AFTER_N_DAYS = 30;

    private final KmsService kmsService;

    private final AwsIamRoleDao awsIamRoleDao;

    private final DateTimeSupplier dateTimeSupplier;

    @Inject
    public CleanUpService(KmsService kmsService,
                          AwsIamRoleDao awsIamRoleDao,
                          DateTimeSupplier dateTimeSupplier) {
        this.kmsService = kmsService;
        this.awsIamRoleDao = awsIamRoleDao;
        this.dateTimeSupplier = dateTimeSupplier;
    }

    public void cleanUp(final CleanUpRequest cleanUpRequest) {
        Integer expirationPeriodInDays = cleanUpRequest.getKmsExpirationPeriodInDays();
        int kmsKeysInactiveAfterNDays = (expirationPeriodInDays == null) ? DEFAULT_KMS_KEY_INACTIVE_AFTER_N_DAYS : expirationPeriodInDays;

        cleanUpInactiveAndOrphanedKmsKeys(kmsKeysInactiveAfterNDays);
        cleanUpOrphanedIamRoles();
    }

    /**
     * Delete all AWS KMS keys and DB records for KMS keys that have not been used recently
     * or are no longer associated with an SDB.
     */
    protected void cleanUpInactiveAndOrphanedKmsKeys(final int kmsKeysInactiveAfterNDays) {

        cleanUpInactiveAndOrphanedKmsKeys(kmsKeysInactiveAfterNDays, DEFAULT_SLEEP_BETWEEN_KMS_CALLS);
    }

    /**
     * Delete all AWS KMS keys and DB records for KMS keys that have not been used recently
     * or are no longer associated with an SDB.
     * @param kmsKeysInactiveAfterNDays - Consider KMS keys to be inactive after 'n' number of days
     * @param sleepInSeconds - Sleep for 'n' seconds between AWS calls, to keep from exceeding the API limit
     */
    protected void cleanUpInactiveAndOrphanedKmsKeys(final int kmsKeysInactiveAfterNDays, final int sleepInSeconds) {

        // get orphaned and inactive kms keys (not used in 'n' days)
        final OffsetDateTime inactiveDateTime = dateTimeSupplier.get().minusDays(kmsKeysInactiveAfterNDays);
        final List<AwsIamRoleKmsKeyRecord> inactiveAndOrphanedKmsKeys = awsIamRoleDao.getInactiveOrOrphanedKmsKeys(inactiveDateTime);

        if (inactiveAndOrphanedKmsKeys.isEmpty()) {
            logger.info("No keys to clean up.");
        } else {
            // delete inactive and orphaned kms key records from DB
            logger.info("Cleaning up orphaned or inactive KMS keys...");
            inactiveAndOrphanedKmsKeys.forEach(kmsKeyRecord -> {
                final String kmsKeyArn = kmsKeyRecord.getAwsKmsKeyId();
                final String kmsKeyRegion = kmsKeyRecord.getAwsRegion();
                try {
                    logger.info("Deleting orphaned or inactive KMS key: id={}, region={}, lastValidated={}",
                            kmsKeyArn, kmsKeyRegion, kmsKeyRecord.getLastValidatedTs());
                    kmsService.validatePolicyAllowsCMSToDeleteCMK(kmsKeyArn, kmsKeyRegion);
                    kmsService.scheduleKmsKeyDeletion(kmsKeyArn, kmsKeyRegion, SOONEST_A_KMS_KEY_CAN_BE_DELETED);
                    kmsService.deleteKmsKeyById(kmsKeyRecord.getId());
                    TimeUnit.SECONDS.sleep(sleepInSeconds);
                } catch (InterruptedException ie) {
                    logger.error("Timeout between KMS key deletion was interrupted", ie);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("There was a problem deleting KMS key with id: {}, region: {}",
                            kmsKeyRecord.getAwsIamRoleId(),
                            kmsKeyRegion,
                            e);
                }
            });
        }
    }

    /**
     * Delete all IAM role records that are no longer associated with an SDB.
     */
    @Transactional
    protected void cleanUpOrphanedIamRoles() {

        // get orphaned iam role ids
        final List<AwsIamRoleRecord> orphanedIamRoleIds = awsIamRoleDao.getOrphanedIamRoles();

        // delete orphaned iam role records from DB
        orphanedIamRoleIds.forEach(awsIamRoleRecord -> {
            try {
                logger.info("Deleting orphaned IAM role: ARN={}, lastUpdated={}",
                        awsIamRoleRecord.getAwsIamRoleArn(),
                        awsIamRoleRecord.getLastUpdatedTs());
                awsIamRoleDao.deleteIamRoleById(awsIamRoleRecord.getId());
            } catch(Exception e) {
                logger.error("There was a problem deleting orphaned IAM role with ARN: {}",
                    awsIamRoleRecord.getAwsIamRoleArn(),
                    e);
            }
        });
    }
}

