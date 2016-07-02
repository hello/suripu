package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggStatsDAODynamoDB;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.AggStatsUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jyfan on 7/1/16.
 */
public class AggStatsProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggStatsProcessor.class);

    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final PillDataDAODynamoDB pillDataDAODynamoDB;
    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final AggStatsDAODynamoDB aggStatsDAODynamoDB;

    private final int MIN_ALLOWED_LOCAL_HOUR = 13; //Perform computations between 1PM and 6PM
    private final int MAX_ALLOWED_LOCAL_HOUR = MIN_ALLOWED_LOCAL_HOUR + 6;

    public AggStatsProcessor(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                             final PillDataDAODynamoDB pillDataDAODynamoDB,
                             final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                             final AggStatsDAODynamoDB aggStatsDAODynamoDB) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.aggStatsDAODynamoDB = aggStatsDAODynamoDB;
    }

    /*
    Build processor
     */
    public static class Builder {
        private SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
        private PillDataDAODynamoDB pillDataDAODynamoDB;
        private DeviceDataDAODynamoDB deviceDataDAODynamoDB;
        private AggStatsDAODynamoDB aggStatsDAODynamoDB;

        public Builder withSleepStatsDAO(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
            this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
            return this;
        }

        public Builder withPillDataDAO(final PillDataDAODynamoDB pillDataDAODynamoDB) {
            this.pillDataDAODynamoDB = pillDataDAODynamoDB;
            return this;
        }

        public Builder withDeviceDataDAO(final DeviceDataDAODynamoDB deviceDataDAODynamoDB) {
            this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
            return this;
        }

        public Builder withAggStatsDAO(final AggStatsDAODynamoDB aggStatsDAODynamoDB) {
            this.aggStatsDAODynamoDB = aggStatsDAODynamoDB;
            return this;
        }

        public AggStatsProcessor build() {
            checkNotNull(sleepStatsDAODynamoDB, "sleepStatsDAODynamoDB cannot be null");
            checkNotNull(pillDataDAODynamoDB, "pillDataDAODynamoDB cannot be null");
            checkNotNull(deviceDataDAODynamoDB, "deviceDataDAODynamoDB cannot be null");
            checkNotNull(aggStatsDAODynamoDB, "aggStatsDAODynamoDB cannot be null");

            return new AggStatsProcessor(
                    this.sleepStatsDAODynamoDB,
                    this.pillDataDAODynamoDB,
                    this.deviceDataDAODynamoDB,
                    this.aggStatsDAODynamoDB);
        }
    }

    /*
    Functionality
     */
    public Boolean generateAggStats(final DeviceAccountPair deviceAccountPair) {

        //Use user's timezone to figure out date of past full date
        final Long accountId = deviceAccountPair.accountId;
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=skip-compute-agg-stats reason=timezoneoffset-absent account_id={}", accountId);
            return Boolean.FALSE;
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final DateTime utcNow = DateTime.now();
        final DateTime localNow = utcNow.plusMillis(timeZoneOffset);

        //Limit computations to period where data collection for previous day is completed
        final Integer localHour = localNow.getHourOfDay();
        if (localHour < MIN_ALLOWED_LOCAL_HOUR || localHour > MAX_ALLOWED_LOCAL_HOUR) {
            LOGGER.debug("action=skip-compute-agg-stats reason=not-in-compute-hours-range account_id={} local_hour={}", accountId, localHour);
            return Boolean.FALSE;
        }

        final DateTime targetDateLocal = localNow.minusDays(1).withTimeAtStartOfDay(); //Date stored is preceding day

        //Query aggStats, does this exist already? Exit early if so.
        final Optional<AggStats> presentAggStat = aggStatsDAODynamoDB.getSingleStat(accountId, targetDateLocal);
        if (presentAggStat.isPresent()) {
            LOGGER.info("action=skip-compute-agg-stats condition=agg-stats-already-present account_id={} target_date_local={}", accountId, targetDateLocal.toString());
            return Boolean.FALSE;
        }

        final AggStats aggStats = computeAggStatsForDay(accountId, deviceAccountPair, targetDateLocal, timeZoneOffset);
        LOGGER.debug("action=computed-agg-stats account_id={} target_date_local={}", accountId, targetDateLocal.toString());

        //Save aggregate statistics
        final Boolean successInsert = saveAggStat(aggStats, deviceAccountPair, utcNow);
        LOGGER.debug("action=insert-agg-stats success={} account_id={} ", successInsert.toString(), aggStats.accountId);
        return successInsert;
    }
    public AggStats computeAggStatsForDay(final Long accountId, final DeviceAccountPair deviceAccountPair, final DateTime targetDateLocal, final Integer timeZoneOffset) {

        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);

        final DateTime startLocalTime = targetDateLocal.withHourOfDay(AggStats.DAY_START_END_HOUR);
        final DateTime endLocalTime = targetDateLocal.plusDays(1).withHourOfDay(AggStats.DAY_START_END_HOUR);

        final DateTime startUTCTime = startLocalTime.minusMillis(timeZoneOffset);
        final DateTime endUTCTime = endLocalTime.minusMillis(timeZoneOffset);

        //Query deviceData
        final Response<ImmutableList<DeviceData>> deviceDataListResponse = deviceDataDAODynamoDB.getBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, DeviceDataDAODynamoDB.ALL_ATTRIBUTES);
        LOGGER.debug("processor=agg-stats action=queryed-device-data account_id={} targetDateLocal={} status={} len_data={}", accountId, targetDateLocal.toString(), deviceDataListResponse.status.toString(), deviceDataListResponse.data.size());

        //Query pillData
        final ImmutableList<TrackerMotion> pillDataList = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, startLocalTime, endLocalTime);
        LOGGER.debug("processor=agg-stats action=queryed-tracker-motion account_id={} targetDateLocal={} len_data={}", accountId, targetDateLocal.toString(), pillDataList.size());

        //Compute aggregate stats
        final AggStats aggStats = AggStatsUtils.computeAggStats(accountId, targetDateLocal, deviceDataListResponse.data, pillDataList);
        LOGGER.debug("action=computed-agg-stats account_id={} targetDateLocal={}", accountId, targetDateLocal.toString());

        return aggStats;
    }

    public Boolean saveAggStat(final AggStats aggStats, final DeviceAccountPair deviceAccountPair, final DateTime createdTimeStampUTC) {
        return aggStatsDAODynamoDB.saveStat(aggStats, deviceAccountPair, createdTimeStampUTC);
    }

}
