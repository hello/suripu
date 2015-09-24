package com.hello.suripu.core.models.device.v2;


import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PairingInfo;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.processors.FeatureFlippedProcessor;
import com.hello.suripu.core.util.PillColorUtil;
import com.librato.rollout.RolloutClient;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class DeviceProcessor extends FeatureFlippedProcessor {

    @Inject
    RolloutClient feature;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceProcessor.class);

    public final DeviceDAO deviceDAO;
    public final DeviceDataDAO deviceDataDAO;
    public final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    public final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    public final PillHeartBeatDAO pillHeartBeatDAO;
    public final TrackerMotionDAO trackerMotionDAO;
    public final WifiInfoDAO wifiInfoDAO;
    public final SenseColorDAO senseColorDAO;


    private DeviceProcessor(final DeviceDAO deviceDAO, final DeviceDataDAO deviceDataDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final PillHeartBeatDAO pillHeartBeatDAO, final TrackerMotionDAO trackerMotionDAO, final WifiInfoDAO wifiInfoDAO, final SenseColorDAO senseColorDAO) {
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.wifiInfoDAO = wifiInfoDAO;
        this.senseColorDAO = senseColorDAO;
    }

    public Optional<PairingInfo> getPairingInfo(final Long accountId) {
        final Optional<DeviceAccountPair> senseAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!senseAccountPairOptional.isPresent()) {
            LOGGER.warn("No sense paired for account = {}", accountId);
            return Optional.absent();
        }
        final DeviceAccountPair senseAccountPair = senseAccountPairOptional.get();
        final ImmutableList<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseAccountPair.externalDeviceId);
        return Optional.of(PairingInfo.create(senseAccountPair.externalDeviceId, pairs.size()));
    }

    public void unregisterPill(final Long accountId, final String pillId) {
        final Integer numRows = deviceDAO.deletePillPairing(pillId, accountId);
        if(numRows == 0) {
            LOGGER.warn("Did not find active pill to unregister");
        }

        final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
        if(sensePairedWithAccount.isEmpty()){
            LOGGER.error("No sense paired with account {}", accountId);
        }

        final String senseId = sensePairedWithAccount.get(0).externalDeviceId;

        try {
            this.mergedUserInfoDynamoDB.deletePillColor(senseId, accountId, pillId);
        }catch (Exception ex){
            LOGGER.error("Delete pill {}'s color from user info table for sense {} and account {} failed: {}",
                    pillId,
                    senseId,
                    accountId,
                    ex.getMessage());
        }
    }

    public void unregisterSense(final Long accountId, final String senseId) {
        final Integer numRows = deviceDAO.deleteSensePairing(senseId, accountId);
        final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);

        if(numRows == 0) {
            LOGGER.warn("Did not find active sense to unregister");
        }

        if(!alarmInfoOptional.isPresent()){
            LOGGER.warn("Cannot find device {} account {} pair in merge info table.", senseId, accountId);
        }
    }

    public void factoryReset(final Long accountId, final String senseId) {
        final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);
        try {
            deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
                @Override
                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accountId);
                    LOGGER.info("Factory reset delete {} Pills linked to account {}", pillDeleted, accountId);

                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
                    LOGGER.info("Factory reset delete {} accounts linked to Sense {}", accountUnlinked, accountId);

                    for (final UserInfo userInfo : pairedUsers) {
                        try {
                            mergedUserInfoDynamoDB.unlinkAccountToDevice(userInfo.accountId, userInfo.deviceId);
                        } catch (AmazonServiceException awsEx) {
                            LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
                                    userInfo.accountId,
                                    userInfo.deviceId,
                                    awsEx.getErrorMessage());
                        }
                    }

                    return null;
                }
            });
        }catch (UnableToExecuteStatementException sqlExp){
            LOGGER.error("Failed to factory reset Sense {}, error {}", senseId, sqlExp.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public void updateWifiInfo(final Long accountId, final WifiInfo wifiInfo) {
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String mostRecentlyPairedSenseId = deviceAccountPairOptional.get().externalDeviceId;
            if (mostRecentlyPairedSenseId.equals(wifiInfo.senseId)) {
                wifiInfoDAO.put(wifiInfo);
            }
            else {
                LOGGER.debug("Account {} attempted to update wifi info for not sense {} but the most recently paired sense is {}", accountId, wifiInfo.senseId, mostRecentlyPairedSenseId);
            }
        }
    }

    public static class Builder {
        private DeviceDAO deviceDAO;
        private DeviceDataDAO deviceDataDAO;
        private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
        private SensorsViewsDynamoDB sensorsViewsDynamoDB;
        private PillHeartBeatDAO pillHeartBeatDAO;
        private TrackerMotionDAO trackerMotionDAO;
        private WifiInfoDAO wifiInfoDAO;
        private SenseColorDAO senseColorDAO;

        public Builder withDeviceDAO(final DeviceDAO deviceDAO) {
            this.deviceDAO = deviceDAO;
            return this;
        }

        public Builder withDeviceDataDAO(final DeviceDataDAO deviceDataDAO) {
            this.deviceDataDAO = deviceDataDAO;
            return this;
        }

        public Builder withMergedUserInfoDynamoDB(final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
            this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
            return this;
        }

        public Builder withSensorsViewDynamoDB(final SensorsViewsDynamoDB sensorsViewDynamoDB) {
            this.sensorsViewsDynamoDB = sensorsViewDynamoDB;
            return this;
        }

        public Builder withPillHeartbeatDAO(final PillHeartBeatDAO pillHeartbeatDAO) {
            this.pillHeartBeatDAO = pillHeartbeatDAO;
            return this;
        }

        public Builder withTrackerMotionDAO(final TrackerMotionDAO trackerMotionDAO) {
            this.trackerMotionDAO = trackerMotionDAO;
            return this;
        }

        public Builder withWifiInfoDAO(final WifiInfoDAO wifiInfoDAO) {
            this.wifiInfoDAO = wifiInfoDAO;
            return this;
        }

        public Builder withSenseColorDAO(final SenseColorDAO senseColorDAO) {
            this.senseColorDAO = senseColorDAO;
            return this;
        }

        public DeviceProcessor build() {
            return new DeviceProcessor(deviceDAO, deviceDataDAO, mergedUserInfoDynamoDB, sensorsViewsDynamoDB, pillHeartBeatDAO, trackerMotionDAO, wifiInfoDAO, senseColorDAO);
        }
    }
    public Devices getAllDevices(final Long accountId) {
        final List<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
        final List<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
        final Map<String, Optional<WifiInfo>> wifiInfoMap = retrieveWifiInfoMap(senseAccountPairs);
        final Optional<Pill.Color> pillColorOptional = retrievePillColor(accountId, senseAccountPairs);

        return new Devices(getSenses(senseAccountPairs, wifiInfoMap), getPills(pillAccountPairs, pillColorOptional));
    }

    public List<Sense> getSenses(final List<DeviceAccountPair> senseAccountPairs, final Map<String, Optional<WifiInfo>> wifiInfoMap) {
        final List<Sense> senses = Lists.newArrayList();

        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = retrieveSenseStatus(senseAccountPair);
            final Optional<WifiInfo> wifiInfoOptional = wifiInfoMap.get(senseAccountPair.externalDeviceId);
            final Optional<Sense.Color> senseColorOptional = senseColorDAO.get(senseAccountPair.externalDeviceId);
            senses.add(Sense.create(senseAccountPair, senseStatusOptional, senseColorOptional, wifiInfoOptional));
        }
        return senses;
    }

    public List<Pill> getPills(final List<DeviceAccountPair> pillAccountPairs, final Optional<Pill.Color> pillColorOptional) {
        final List<Pill> pills = Lists.newArrayList();
        for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
            final Optional<DeviceStatus> pillStatusOptional = retrievePillStatus(pillAccountPair);
            pills.add(Pill.create(pillAccountPair, pillStatusOptional, pillColorOptional));
        }
        return pills;
    }

    private Optional<DeviceStatus> retrieveSenseStatus(final DeviceAccountPair senseAccountPair) {
        // First attempt: get it from last seen record in dynamo db
        if (this.isSenseLastSeenDynamoDBReadEnabled(senseAccountPair.accountId)) {
            return sensorsViewsDynamoDB.senseStatus(senseAccountPair.externalDeviceId, senseAccountPair.accountId, senseAccountPair.internalDeviceId);
        }

        if (this.isSensorsDBUnavailable(senseAccountPair.accountId)) {
            return Optional.absent();
        }
        // Second attempt: get it from sensor db with assumption that such sense has been active since an hour ago
        Optional<DeviceStatus> senseStatusOptional = deviceDataDAO.senseStatusLastHour(senseAccountPair.internalDeviceId);

        // Third attempt: get if from sensor db with assumption that such sense has been active since a week ago
        if (!senseStatusOptional.isPresent()) {
            senseStatusOptional = deviceDataDAO.senseStatusLastWeek(senseAccountPair.internalDeviceId);
        }
        return senseStatusOptional;
    }

    private Optional<DeviceStatus> retrievePillStatus(final DeviceAccountPair pillAccountPair) {
        // First attempt: get it from heartbeat
        Optional<DeviceStatus> pillStatusOptional = this.pillHeartBeatDAO.getPillStatus(pillAccountPair.internalDeviceId);

        // Second attempt: get it from tracker motion
        if (!pillStatusOptional.isPresent()) {
            pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId, pillAccountPair.accountId);
        }
        return pillStatusOptional;
    }

    private Optional<Pill.Color> retrievePillColor(final Long accountId, final List<DeviceAccountPair> senseAccountPairs) {
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            if (!accountId.equals(senseAccountPair.accountId)) {
                continue;
            }
            final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseAccountPair.externalDeviceId);
            if (userInfoList.isEmpty()) {
                return Optional.absent();
            }
            final Optional<OutputProtos.SyncResponse.PillSettings> pillSettingsOptional =  userInfoList.get(0).pillColor;
            if (!pillSettingsOptional.isPresent()){
                return Optional.absent();
            }
            return Optional.of(PillColorUtil.displayPillColor(pillSettingsOptional.get().getPillColor()));
        }
        return Optional.absent();
    }

    private Map<String, Optional<WifiInfo>> retrieveWifiInfoMap(final List<DeviceAccountPair> senseAccountPairs) {
        final List<String> senseIds = Lists.newArrayList();
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            senseIds.add(senseAccountPair.externalDeviceId);
        }
        return wifiInfoDAO.getBatchStrict(senseIds);
    }

}
