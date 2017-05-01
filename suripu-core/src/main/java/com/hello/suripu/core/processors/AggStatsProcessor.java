package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggStatsDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.AggStatsInputs;
import com.hello.suripu.core.models.Insights.SumCountData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.AggStatsComputer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
    private final SenseColorDAO senseColorDAO;
    private final CalibrationDAO calibrationDAO;

    private final AggStatsDAODynamoDB aggStatsDAODynamoDB;

//    Start computation 1 hr after end of aggStats day. (Compute between 1PM and 7PM)
    private final int MIN_ALLOWED_LOCAL_HOUR = AggStats.DAY_START_END_HOUR + 1; //Perform computations between 1PM and 7PM
    private final int NUM_HOURS_WORKER_ON = 6;
    private final int MAX_ALLOWED_LOCAL_HOUR = MIN_ALLOWED_LOCAL_HOUR + NUM_HOURS_WORKER_ON;

    //Anomaly thresholds
    private final int MIN_REASONABLE_TEMP = 10; //50 F
    private final int MAX_REASONABLE_TEMP = 35; //95 F
    private final int MIN_REASONABLE_HUM = 5;
    private final int MAX_REASONABLE_HUM = 95;
    private final int MAX_REASONABLE_PARTICULATE = 90;
    private final int MAX_REASONABLE_LUX = 30000; //full direct sun

    private AggStatsProcessor(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                             final PillDataDAODynamoDB pillDataDAODynamoDB,
                             final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                             final SenseColorDAO senseColorDAO,
                             final CalibrationDAO calibrationDAO,
                             final AggStatsDAODynamoDB aggStatsDAODynamoDB) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.calibrationDAO = calibrationDAO;
        this.aggStatsDAODynamoDB = aggStatsDAODynamoDB;
    }

    /*
    Build processor
     */
    public static class Builder {
        private SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
        private PillDataDAODynamoDB pillDataDAODynamoDB;
        private DeviceDataDAODynamoDB deviceDataDAODynamoDB;
        private SenseColorDAO senseColorDAO;
        private CalibrationDAO calibrationDAO;
        private AggStatsDAODynamoDB aggStatsDAODynamoDB;

        public Builder withSleepStatsDAODynamoDB(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
            this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
            return this;
        }

        public Builder withPillDataDAODynamoDB(final PillDataDAODynamoDB pillDataDAODynamoDB) {
            this.pillDataDAODynamoDB = pillDataDAODynamoDB;
            return this;
        }

        public Builder withDeviceDataDAODynamoDB(final DeviceDataDAODynamoDB deviceDataDAODynamoDB) {
            this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
            return this;
        }

        public Builder withSenseColorDAO(final SenseColorDAO senseColorDAO) {
            this.senseColorDAO = senseColorDAO;
            return this;
        }

        public Builder withCalibrationDAO(final CalibrationDAO calibrationDAO) {
            this.calibrationDAO = calibrationDAO;
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
            checkNotNull(senseColorDAO, "senseColorDAO cannot be null");
            checkNotNull(calibrationDAO, "calibrationDAO cannot be null");
            checkNotNull(aggStatsDAODynamoDB, "aggStatsDAODynamoDB cannot be null");

            return new AggStatsProcessor(
                    this.sleepStatsDAODynamoDB,
                    this.pillDataDAODynamoDB,
                    this.deviceDataDAODynamoDB,
                    this.senseColorDAO,
                    this.calibrationDAO,
                    this.aggStatsDAODynamoDB);
        }
    }

    /*
    Functionality
     */
    public Boolean generateCurrentAggStats(final DeviceAccountPair deviceAccountPair) {

        //Use user's timezone to figure out date of past full date
        final Long accountId = deviceAccountPair.accountId;
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("method=current-aggstat action=skip-compute-agg-stats reason=timezoneoffset-absent account_id={}", accountId);
            return Boolean.FALSE;
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final DateTime utcNow = DateTime.now(DateTimeZone.UTC);
        final DateTime localNow = utcNow.plusMillis(timeZoneOffset);

        //Limit computations to period where data collection for previous day is completed
        final Boolean inAllowedComputeTime = checkAllowedComputeTime(localNow);
        if (!inAllowedComputeTime) {
            LOGGER.debug("action=skip-compute-agg-stats reason=not-in-compute-hours-range account_id={} local_hour={}", accountId, localNow.getHourOfDay());
            return Boolean.FALSE;
        }

        final DateTime targetDateLocal = localNow.minusDays(1).withTimeAtStartOfDay(); //Date stored is preceding day

        //Query aggStats, does this exist already? Exit early if so.
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);
        final Optional<AggStats> presentAggStat = aggStatsDAODynamoDB.getSingleStat(accountId, deviceId, targetDateLocal);
        if (presentAggStat.isPresent()) {
            LOGGER.debug("action=skip-compute-agg-stats condition=agg-stats-already-present account_id={} target_date_local={}", accountId, targetDateLocal.toString());
            return Boolean.FALSE;
        }

        final Optional<AggStats> aggStats = computeAggStatsForDay(accountId, deviceId, targetDateLocal, timeZoneOffset);
        LOGGER.debug("action=computed-agg-stats account_id={} target_date_local={} present={}", accountId, targetDateLocal.toString(), aggStats.isPresent());
        if (!aggStats.isPresent()) {
            LOGGER.info("action=do-nothing reason=agg-stats-absent account_id={}", accountId);
            return Boolean.FALSE;
        }

        //Save aggregate statistics
        final Boolean successInsert = saveAggStat(aggStats.get());
        if (!successInsert) {
            LOGGER.warn("action=insert-agg-stats success={} account_id={} ", successInsert, aggStats.get().accountId);
        }

        return successInsert;
    }

    private Boolean checkAllowedComputeTime(final DateTime localTime) {
        final Integer localHour = localTime.getHourOfDay();
        if (localHour < MIN_ALLOWED_LOCAL_HOUR || localHour > MAX_ALLOWED_LOCAL_HOUR) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    //Method for backfilling - called by admin endpoint
    public Boolean generatePastAggStat(final DeviceAccountPair deviceAccountPair, final DateTime targetDateLocal, final Integer timeZoneOffset, final Boolean overwrite) {

        final Long accountId = deviceAccountPair.accountId;

        final DateTime utcNow = DateTime.now(DateTimeZone.UTC);
        final DateTime localNow = utcNow.plusMillis(timeZoneOffset);
        final DateTime latestAllowedDate = localNow.minusDays(1).withTimeAtStartOfDay();

        if (targetDateLocal.isAfter(latestAllowedDate)) {
            LOGGER.error("method=past-aggstat action=skip-compute-agg-stats reason=target-date-past-latest-allowed account_id={}", accountId);
            return Boolean.FALSE;
        }

        targetDateLocal.withTimeAtStartOfDay();
        //Query aggStats, does this exist already? Exit early if we don't want to overwrite.
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);
        final Optional<AggStats> presentAggStat = aggStatsDAODynamoDB.getSingleStat(accountId, deviceId, targetDateLocal);
        if (presentAggStat.isPresent() && !overwrite) {
            LOGGER.debug("action=skip-compute-agg-stats condition=agg-stats-already-present account_id={} target_date_local={}", accountId, targetDateLocal.toString());
            return Boolean.FALSE;
        }

        final Optional<AggStats> aggStats = computeAggStatsForDay(accountId, deviceId, targetDateLocal, timeZoneOffset);
        LOGGER.trace("action=computed-agg-stats account_id={} target_date_local={} present={}", accountId, targetDateLocal.toString(), aggStats.isPresent());
        if (!aggStats.isPresent()) {
            LOGGER.trace("action=do-nothing reason=agg-stats-absent");
            return Boolean.FALSE;
        }

        //Save aggregate statistics
        final Boolean successInsert = saveAggStat(aggStats.get());
        LOGGER.trace("action=insert-agg-stats success={} account_id={} overwrite={}", successInsert, aggStats.get().accountId, overwrite);
        return successInsert;
    }

    private Optional<AggStats> computeAggStatsForDay(final Long accountId, final DeviceId deviceId, final DateTime targetDateLocal, final Integer timeZoneOffset) {

        final DateTime startLocalTime = targetDateLocal.withHourOfDay(AggStats.DAY_START_END_HOUR);
        final DateTime endLocalTime = targetDateLocal.plusDays(1).withHourOfDay(AggStats.DAY_START_END_HOUR);

        final DateTime startUTCTime = startLocalTime.minusMillis(timeZoneOffset);
        final DateTime endUTCTime = endLocalTime.minusMillis(timeZoneOffset);

        //Query deviceData
        final Response<ImmutableList<DeviceData>> deviceDataListResponse = deviceDataDAODynamoDB.getBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, deviceDataDAODynamoDB.ALL_ATTRIBUTES);
        LOGGER.trace("processor=agg-stats action=queryed-device-data account_id={} targetDateLocal={} status={} len_data={}", accountId, targetDateLocal.toString(), deviceDataListResponse.status.toString(), deviceDataListResponse.data.size());

        //Query pillData
        final ImmutableList<TrackerMotion> pillDataList = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, startLocalTime, endLocalTime);
        LOGGER.trace("processor=agg-stats action=queryed-tracker-motion account_id={} targetDateLocal={} len_data={}", accountId, targetDateLocal.toString(), pillDataList.size());

        //Query sense color, dust calibration
        final Optional<Device.Color> senseColorOptional = getSenseColorOptional(senseColorDAO, deviceId);
        final Optional<Calibration> calibrationOptional = getCalibrationOptional(calibrationDAO, deviceId);

        //Compute aggregate stats
        final AggStatsInputs aggStatsInputs = AggStatsInputs.create(senseColorOptional, calibrationOptional, deviceDataListResponse, pillDataList);
        final Optional<AggStats> aggStats = AggStatsComputer.computeAggStats(accountId, deviceId, targetDateLocal, aggStatsInputs);

        //Check for data anomalies
        if (aggStats.isPresent()) {
            checkAnomaly(aggStats.get());
        }

        return aggStats;
    }

    private void checkAnomaly(final AggStats aggStats) {

        //temp check
        if (aggStats.minDailyTemp.isPresent()) {
            if (fromMicroIntConversion(aggStats.minDailyTemp.get()) < MIN_REASONABLE_TEMP) {
                LOGGER.info("anomaly=low-temp date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            }
        }

        if (aggStats.maxDailyTemp.isPresent()) {
            if (aggStats.maxDailyTemp.get() > MAX_REASONABLE_TEMP) {
                LOGGER.info("anomaly=high-temp date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            }
        }

        //humidity check
        if (aggStats.avgDailyHumidity.isPresent()) {
            if (fromMicroIntConversion(aggStats.avgDailyHumidity.get()) < MIN_REASONABLE_HUM) {
                LOGGER.info("anomaly=low-hum date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            } else if (fromMicroIntConversion(aggStats.avgDailyHumidity.get()) > MAX_REASONABLE_HUM) {
                LOGGER.info("anomaly=high-hum date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            }
        }

        //particulates check
        if (aggStats.avgDailyDustDensity.isPresent()) {
            if (fromMicroIntConversion(aggStats.avgDailyDustDensity.get()) > MAX_REASONABLE_PARTICULATE) {
                LOGGER.info("anomaly=high-dust date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            }
        }

        //light check
        for (SumCountData sumCountData : aggStats.sumCountMicroLuxHourMap.values()) {
            if (fromMicroIntConversion(sumCountData.sum) > MAX_REASONABLE_LUX) {
                LOGGER.info("anomaly=high-lux date_local={} account_id={} device_id={}", aggStats.dateLocal, aggStats.accountId, aggStats.externalDeviceId);
            }
        }

    }

    private static int fromMicroIntConversion(final int data) {
        return data / AggStatsComputer.TO_MICRO_CONVERSION;
    }

    public Optional<Device.Color> getSenseColorOptional(final SenseColorDAO senseColorDAO, final DeviceId deviceId) {
        if (!deviceId.externalDeviceId.isPresent()) {
            return Optional.absent();
        }

        return senseColorDAO.getColorForSense(deviceId.externalDeviceId.get());
    }

    public Optional<Calibration> getCalibrationOptional(final CalibrationDAO calibrationDAO, final DeviceId deviceId) {
        if (!deviceId.externalDeviceId.isPresent()) {
            return Optional.absent();
        }

        return calibrationDAO.getStrict(deviceId.externalDeviceId.get());
    }

    public Boolean saveAggStat(final AggStats aggStats) {
        return aggStatsDAODynamoDB.insertSingleStat(aggStats);
    }

}
