package com.hello.suripu.core.models.device.v2;


import com.amazonaws.AmazonServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.analytics.AnalyticsTracker;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PairingInfo;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.core.sense.metadata.SenseMetadata;
import com.hello.suripu.core.sense.metadata.SenseMetadataDAO;
import com.hello.suripu.core.sense.voice.VoiceMetadata;
import com.hello.suripu.core.sense.voice.VoiceMetadataDAO;
import com.hello.suripu.core.util.PillColorUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceProcessor.class);

    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;
    private final PillDataDAODynamoDB pillDataDAODynamoDB;
    private final WifiInfoDAO wifiInfoDAO;
    private final AnalyticsTracker analyticsTracker;
    private final SenseMetadataDAO senseMetadataDAO;
    private final VoiceMetadataDAO voiceMetadataDAO;
    private final KeyStore pillKeyStore;

    private final static Integer MIN_ACCOUNT_AGE_FOR_LOW_BATTERY_WARNING = 28; // days
    private final static Integer BATTERY_LEVEL_LOW_BATTERY_WARNING = 15;
    public static final Integer RECENTLY_PAIRED_PILL_UNSEEN_THRESHOLD = 80; // allow 80 mins for first heartbeat to be uploaded

    private DeviceProcessor(final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                            final SensorsViewsDynamoDB sensorsViewsDynamoDB,
                            final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB,
                            final PillDataDAODynamoDB pillDataDAODynamoDB,
                            final WifiInfoDAO wifiInfoDAO,
                            final AnalyticsTracker analyticsTracker,
                            final SenseMetadataDAO senseMetadataDAO,
                            final VoiceMetadataDAO voiceMetadataDAO,
                            final KeyStore pillKeyStore) {
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
        this.wifiInfoDAO = wifiInfoDAO;
        this.analyticsTracker = analyticsTracker;
        this.senseMetadataDAO = senseMetadataDAO;
        this.voiceMetadataDAO = voiceMetadataDAO;
        this.pillKeyStore = pillKeyStore;
    }

    /**
     * Retrieve pairing info which contains sense ID and number of senses paired
     *
     * @param accountId internal account ID
     * @return optional pairing info
     */
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

    /**
     * Unpair a pill from an account
     *
     * @param accountId internal account ID
     * @param pillId external pill ID
     */
    public void unregisterPill(final Long accountId, final String pillId) {
        final Integer numRows = deviceDAO.deletePillPairing(pillId, accountId);
        if(numRows == 0) {
            LOGGER.warn("Did not find active pill to unregister");
        }

        final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
        if(sensePairedWithAccount.isEmpty()){
            LOGGER.error("No sense paired with account {}", accountId);
            return;
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


    /**
     * Unpair a sense from an account
     *
     * @param accountId internal account ID
     * @param senseId external sense ID
     */
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


    /**
     * Factory reset which means unpair all pills from input account, and unpair all account associated with input sense
     *
     * @param accountId internal account ID
     * @param senseId external sense ID
     */
    public void factoryReset(final Long accountId, final String senseId) {
        final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);
        try {
            deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
                @Override
                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accountId);
                    LOGGER.info("Factory reset - delete {} Pills linked to account {}", pillDeleted, accountId);

                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
                    LOGGER.info("Factory reset - delete {} accounts linked to Sense {}", accountUnlinked, senseId);

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
            LOGGER.error("error=factory-reset-failed sense_id=%s message={}", senseId, sqlExp.getMessage());
            throw new RuntimeException(sqlExp);
        }
    }


    /**
     * Update / insert wifi info for a sense
     *
     * @param accountId internal account ID
     * @param wifiInfo container of wifi specs
     */
    public Boolean upsertWifiInfo(final Long accountId, final WifiInfo wifiInfo) throws InvalidWifiInfoException {
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String mostRecentlyPairedSenseId = deviceAccountPairOptional.get().externalDeviceId;
            if (mostRecentlyPairedSenseId.equals(wifiInfo.senseId)) {
                return wifiInfoDAO.put(wifiInfo);
            }
            LOGGER.debug("Account {} attempted to update wifi info for sense {} but the most recently paired sense is {}", accountId, wifiInfo.senseId, mostRecentlyPairedSenseId);
            throw new InvalidWifiInfoException(String.format("Mismatch account id from token and input object"));
        }
        throw new InvalidWifiInfoException(String.format("No associated account found for sense %s", wifiInfo.senseId));
    }


    /**
     * Get all senses and pills for an account
     *
     * @param deviceQueryInfo all the info needed to do queries
     * @return a Devices object which contains list of all associated senses and pills
     */
    public Devices getAllDevices(final DeviceQueryInfo deviceQueryInfo) {
        final List<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(deviceQueryInfo.accountId);

        // We only want to return the most recently paired sense
        final Optional<DeviceAccountPair> senseAccountPair = deviceDAO.getMostRecentSensePairByAccountId(deviceQueryInfo.accountId);
        final List<DeviceAccountPair> senseAccountPairs = new ArrayList<>();
        if(senseAccountPair.isPresent()) {
            senseAccountPairs.add(senseAccountPair.get());
        }

        final Map<String, Optional<WifiInfo>> wifiInfoMap = retrieveWifiInfoMap(senseAccountPairs);
        final Optional<Pill.Color> pillColorOptional = retrievePillColor(deviceQueryInfo.accountId, senseAccountPairs);

        final List<Sense> senses = getSenses(senseAccountPairs, wifiInfoMap);

        if(deviceQueryInfo.account.isPresent()) {
            final List<Pill> pills = getPills(pillAccountPairs, pillColorOptional, deviceQueryInfo.account.get(), DateTime.now(DateTimeZone.UTC));
            return new Devices(senses, pills);
        }

        final List<Pill> pills = getPills(pillAccountPairs, pillColorOptional, DateTime.now(DateTimeZone.UTC));
        return new Devices(senses, pills);
    }

    private List<Sense> getSenses(final List<DeviceAccountPair> senseAccountPairs, final Map<String, Optional<WifiInfo>> wifiInfoMap) {
        final List<Sense> senses = Lists.newArrayList();
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = retrieveSenseStatus(senseAccountPair);
            final Optional<WifiInfo> wifiInfoOptional = wifiInfoMap.get(senseAccountPair.externalDeviceId);
            final Optional<SenseMetadata> metadataOptional = Optional.fromNullable(senseMetadataDAO.get(senseAccountPair.externalDeviceId));
            // TODO: get metadata from keystore if it fails?
            // should it be allowed to fail???
            final SenseMetadata defaultMetadata = SenseMetadata.unknown(senseAccountPair.externalDeviceId);
            final SenseMetadata metadata = metadataOptional.or(defaultMetadata);

            final Sense sense = Sense.create(
                    senseAccountPair,
                    senseStatusOptional,
                    wifiInfoOptional,
                    metadata);

            senses.add(sense);
        }
        return senses;
    }

    private static Pill.BatteryType fromSN(String metadata) {
        if(metadata.startsWith("90500007A")) {
            // og pill
            return Pill.BatteryType.REMOVABLE;
        } else if(metadata.startsWith("905000071")) {
            // 2nd gen pill
            return Pill.BatteryType.SEALED;
        }

        return Pill.BatteryType.UNKNOWN;
    }

    private List<Pill> getPills(final List<DeviceAccountPair> pillAccountPairs, final Optional<Pill.Color> pillColorOptional, final DateTime now) {
        final List<Pill> pills = Lists.newArrayList();

        for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
            final Optional<PillHeartBeat> pillStatusOptional = retrievePillHeartBeat(pillAccountPair, now);
            final Optional<DeviceKeyStoreRecord> recordOptional = pillKeyStore.getKeyStoreRecord(pillAccountPair.externalDeviceId);
            final Pill.BatteryType batteryType = (recordOptional.isPresent()) ? fromSN(recordOptional.get().metadata) : Pill.BatteryType.UNKNOWN;

            // choose between heartbeat created or pairing created time for pill's last seen
            final Pill pill;
            if (usePillPairedTimeAsLastSeen(pillStatusOptional, pillAccountPair.created, now)) {
                LOGGER.debug("action=pill-last-seen-from-paired-time pill_id={} account_id={}", pillAccountPair.externalDeviceId, pillAccountPair.accountId);
                pill = Pill.createRecentlyPaired(pillAccountPair, pillColorOptional, batteryType);
            } else {
                pill = Pill.create(pillAccountPair, pillStatusOptional, pillColorOptional, batteryType);
            }
            pills.add(pill);
        }

        return pills;
    }

    public static Boolean usePillPairedTimeAsLastSeen(final Optional<PillHeartBeat> pillStatusOptional, final DateTime pairedDateTime, final DateTime now) {
        final DateTime lastHeartBeatThreshold = now.minusMinutes(RECENTLY_PAIRED_PILL_UNSEEN_THRESHOLD);

        if (pillStatusOptional.isPresent()) {
            if (pillStatusOptional.get().createdAtUTC.isBefore(pairedDateTime)) {
                // if paired time is too long ago, and an old heart beat exist, return heartbeat time
                // else if paired time is after threshold and heartbeat, return paired time
                return pairedDateTime.isAfter(lastHeartBeatThreshold);
            }
        } else if (pairedDateTime.isAfter(lastHeartBeatThreshold)) {
            // newly-paired pill, no previous heartbeat
            return true;
        }
        return false;
    }

    private List<Pill> getPills(final List<DeviceAccountPair> pillAccountPairs, final Optional<Pill.Color> pillColorOptional, final Account account, final DateTime referenceTime) {
        final List<Pill> pills = getPills(pillAccountPairs, pillColorOptional, referenceTime);
        final Days days = Days.daysBetween(referenceTime,account.created);
        final int accountCreatedInDays = Math.abs(days.getDays());
        if(accountCreatedInDays > MIN_ACCOUNT_AGE_FOR_LOW_BATTERY_WARNING ) {
            return pills;
        }


        // Special case: we want to hide the low warning battery for new accounts
        return DeviceProcessor.replaceBatteryWarning(pills, account, analyticsTracker);
    }

    @VisibleForTesting
    public Optional<DeviceStatus> retrieveSenseStatus(final DeviceAccountPair senseAccountPair) {
        return sensorsViewsDynamoDB.senseStatus(senseAccountPair.externalDeviceId, senseAccountPair.accountId, senseAccountPair.internalDeviceId);
    }


    @VisibleForTesting
    public Optional<PillHeartBeat> retrievePillHeartBeat(final DeviceAccountPair pillAccountPair, final DateTime now) {
        // First attempt: get it from heartbeat
        final Optional<PillHeartBeat> pillHeartBeatOptional = this.pillHeartBeatDAODynamoDB.get(pillAccountPair.externalDeviceId);

        if (pillHeartBeatOptional.isPresent()) {
            return pillHeartBeatOptional;
        }

        LOGGER.warn("No pill heartbeat for pill {} in dynamoDB", pillAccountPair.externalDeviceId);

        final Optional<TrackerMotion> trackerMotionOptional = this.pillDataDAODynamoDB.getMostRecent(
                pillAccountPair.externalDeviceId,
                pillAccountPair.accountId,
                now);

        if(trackerMotionOptional.isPresent()) {
            return Optional.of(PillHeartBeat.fromTrackerMotion(trackerMotionOptional.get()));
        }

        LOGGER.warn("No pill heartbeat for pill {} in trackerMotionDAO", pillAccountPair.externalDeviceId);
        return Optional.absent();
    }

    private Optional<PillHeartBeat> retrievePillHeartBeat(final DeviceAccountPair pillAccountPair) {
        return retrievePillHeartBeat(pillAccountPair, DateTime.now(DateTimeZone.UTC));
    }

    @VisibleForTesting
    public Optional<Pill.Color> retrievePillColor(final Long accountId, final List<DeviceAccountPair> senseAccountPairs) {
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            if (!accountId.equals(senseAccountPair.accountId)) {
                continue;
            }
            final Optional<UserInfo> userInfoOptional = mergedUserInfoDynamoDB.getInfo(senseAccountPair.externalDeviceId, senseAccountPair.accountId);
            if (!userInfoOptional.isPresent()) {
                return Optional.absent();
            }
            final Optional<OutputProtos.SyncResponse.PillSettings> pillSettingsOptional =  userInfoOptional.get().pillColor;
            if (!pillSettingsOptional.isPresent()){
                return Optional.absent();
            }
            return Optional.of(PillColorUtil.displayPillColor(pillSettingsOptional.get().getPillColor()));
        }
        return Optional.absent();
    }


    @VisibleForTesting
    public Map<String, Optional<WifiInfo>> retrieveWifiInfoMap(final List<DeviceAccountPair> senseAccountPairs) {
        final List<String> senseIds = Lists.newArrayList();
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            senseIds.add(senseAccountPair.externalDeviceId);
        }
        return wifiInfoDAO.getBatchStrict(senseIds);
    }

    public static class Builder {
        private DeviceDAO deviceDAO;
        private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
        private SensorsViewsDynamoDB sensorsViewsDynamoDB;
        private PillDataDAODynamoDB pillDataDAODynamoDB;
        private WifiInfoDAO wifiInfoDAO;
        private SenseColorDAO senseColorDAO;
        private PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;
        private AnalyticsTracker analyticsTracker;
        private SenseMetadataDAO senseMetadataDAO;
        private VoiceMetadataDAO voiceMetadataDAO;
        private KeyStore pillKeyStore;

        public Builder withDeviceDAO(final DeviceDAO deviceDAO) {
            this.deviceDAO = deviceDAO;
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

        public Builder withPillDataDAODynamoDB(final PillDataDAODynamoDB pillDataDAODynamoDB) {
            this.pillDataDAODynamoDB = pillDataDAODynamoDB;
            return this;
        }

        public Builder withWifiInfoDAO(final WifiInfoDAO wifiInfoDAO) {
            this.wifiInfoDAO = wifiInfoDAO;
            return this;
        }

        public Builder withPillHeartbeatDAO(final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB) {
            this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
            return this;
        }

        public Builder withAnalyticsTracker(final AnalyticsTracker analyticsTracker) {
            this.analyticsTracker = analyticsTracker;
            return this;
        }

        public Builder withSenseMetadataDAO(final SenseMetadataDAO senseMetadataDAO) {
            this.senseMetadataDAO = senseMetadataDAO;
            return this;
        }

        public Builder withVoiceMetadataDAO(final VoiceMetadataDAO voiceMetadataDAO) {
            this.voiceMetadataDAO = voiceMetadataDAO;
            return this;
        }

        public Builder withKeyStore(final KeyStore pillKeyStore) {
            this.pillKeyStore = pillKeyStore;
            return this;
        }
        
        public DeviceProcessor build() {
            checkNotNull(analyticsTracker, "analytics tracker can not be null");

            return new DeviceProcessor(deviceDAO, mergedUserInfoDynamoDB,
                    sensorsViewsDynamoDB, pillHeartBeatDAODynamoDB,
                    pillDataDAODynamoDB, wifiInfoDAO, analyticsTracker,
                    senseMetadataDAO, voiceMetadataDAO, pillKeyStore
            );
        }
    }

    public static class InvalidWifiInfoException extends Exception {
        public InvalidWifiInfoException(final String message) {
            super(message);
        }
    }


    public static List<Pill> replaceBatteryWarning(final List<Pill> pills, final Account account, final AnalyticsTracker tracker) {
        final List<Pill> pillsWithBatteryWarningHidden = new ArrayList<>();

        for(Pill pill : pills) {
            if(pill.batteryLevelOptional.isPresent()) {

                if(pill.batteryLevelOptional.get() <= BATTERY_LEVEL_LOW_BATTERY_WARNING) {
                    LOGGER.warn("message=low-battery-new-account account_id={}", account.id.get());
                    pill = Pill.withState(pill, Pill.State.NORMAL);
                    tracker.trackLowBattery(pill, account);
                }
            }
            pillsWithBatteryWarningHidden.add(pill);
        }

        return pillsWithBatteryWarningHidden;
    }

    public Optional<Long> primaryAccount(final String senseId) {
        final Optional<Long> primaryOverride = voiceMetadataDAO.getPrimaryAccount(senseId);
        if(primaryOverride.isPresent()) {
            return primaryOverride;
        }

        final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseId);

        if(pairs.isEmpty()) {
            LOGGER.error("error=no-account-paired sense_id={}", senseId);
            return Optional.absent();
        }

        if(pairs.size() == 1) {
            return Optional.of(pairs.get(0).accountId);
        }

        final Ordering<DeviceAccountPair> byPairedDateOrdering = new Ordering<DeviceAccountPair>() {
            public int compare(DeviceAccountPair left, DeviceAccountPair right) {
                return Longs.compare(left.created.getMillis(), right.created.getMillis());
            }
        };

        return Optional.of(byPairedDateOrdering.sortedCopy(pairs).get(0).accountId);
    }

    public VoiceMetadata voiceMetadata(final String senseId, final Long accountId) {
        final Optional<Long> primaryAccount = primaryAccount(senseId);
        final VoiceMetadata voiceMetadata = voiceMetadataDAO.get(senseId, accountId, primaryAccount.orNull());
        return voiceMetadata;
    }

    public boolean isPairedTo(final Long accountId, final String senseId) {
        final Optional<Long> internalId = deviceDAO.getIdForAccountIdDeviceId(accountId, senseId);
        return internalId.isPresent();
    }
}
