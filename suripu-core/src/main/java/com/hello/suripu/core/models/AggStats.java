package com.hello.suripu.core.models;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Insights.SumCountData;
import org.joda.time.DateTime;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jyfan on 7/1/16.
 *
 * We define a day as a roughly 24-hour period of daytime hours plus the immediately preceding hours of sleep at night.
 * Example day ranges [2016-07-01 12noon, 2016-07-02 12noon) referred to by the date "2016-07-01" (user's local time)
 *
 * See AggStatsDAODynamoDB getIntegerFromDDBIItem() => values of -999 indicates absence of parameter in database
 */
public class AggStats {
    //Definitions https://hello.hackpad.com/agg_stats-KETKVqohjiv

    public static final int DAY_START_END_HOUR = 12; //a day begins at 12noon and ends at 12noon the next date

    public final Long accountId;
    public final DateTime dateLocal;
    public final String externalDeviceId;

    public final Optional<Integer> deviceDataCount;
    public final Optional<Integer> trackerMotionCount;

    //all processed and calibrated values, multiplied by 10e6
    //Note: please do not add stats above 1st moment without first discussing with data science team
    public final Optional<Integer> avgDailyTemp; //micro-celsius
    public final Optional<Integer> maxDailyTemp; //micro-celsius
    public final Optional<Integer> minDailyTemp; //micro-celsius
    public final Optional<Integer> avgDailyHumidity; // percent relative humidity * 10e-6
    public final Optional<Integer> avgDailyDustDensity; //micro-micro-gram/m^3

    public final Map<Integer, SumCountData> sumCountMicroLuxHourMap; //micro-lux, calibrated by sense color, each hour

    private AggStats(final Long accountId,
                     final DateTime dateLocal,
                     final String externalDeviceId,

                     final Optional<Integer> deviceDataCount,
                     final Optional<Integer> trackerMotionCount,

                     final Optional<Integer> avgDailyTemp,
                     final Optional<Integer> maxDailyTemp,
                     final Optional<Integer> minDailyTemp,
                     final Optional<Integer> avgDailyHumidity,
                     final Optional<Integer> avgDailyDustDensity,

                     final Map<Integer, SumCountData> sumCountMicroLuxHourMap) {

        this.accountId = accountId;
        this.dateLocal = dateLocal;
        this.externalDeviceId = externalDeviceId;

        this.deviceDataCount = deviceDataCount;
        this.trackerMotionCount = trackerMotionCount;

        this.avgDailyTemp = avgDailyTemp;
        this.maxDailyTemp = maxDailyTemp;
        this.minDailyTemp = minDailyTemp;
        this.avgDailyHumidity = avgDailyHumidity;
        this.avgDailyDustDensity = avgDailyDustDensity;

        this.sumCountMicroLuxHourMap = sumCountMicroLuxHourMap;
    }

    public static class Builder {
        private Long accountId;
        private DateTime dateLocal;
        private String externalDeviceId;

        private Optional<Integer> deviceDataCount;
        private Optional<Integer> trackerMotionCount;

        private Optional<Integer> avgDailyTemp;
        private Optional<Integer> maxDailyTemp;
        private Optional<Integer> minDailyTemp;
        private Optional<Integer> avgDailyHumidity;
        private Optional<Integer> avgDailyDustDensity;

        private Map<Integer, SumCountData> sumCountMicroLuxHourMap = Maps.newHashMap();

        public AggStats.Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public AggStats.Builder withDateLocal(final DateTime dateLocal) {
            this.dateLocal = dateLocal;
            return this;
        }

        public AggStats.Builder withExternalDeviceId(final String externalDeviceId) {
            this.externalDeviceId = externalDeviceId;
            return this;
        }

        public AggStats.Builder withDeviceDataCount(final Optional<Integer> deviceDataCount) {
            this.deviceDataCount = deviceDataCount;
            return this;
        }

        public AggStats.Builder withTrackerMotionCount(final Optional<Integer> trackerMotionCount) {
            this.trackerMotionCount = trackerMotionCount;
            return this;
        }

        public AggStats.Builder withAvgDailyTemp(final Optional<Integer> avgDailyTemp) {
            this.avgDailyTemp = avgDailyTemp;
            return this;
        }

        public AggStats.Builder withMaxDailyTemp(final Optional<Integer> maxDailyTemp) {
            this.maxDailyTemp = maxDailyTemp;
            return this;
        }

        public AggStats.Builder withMinDailyTemp(final Optional<Integer> minDailyTemp) {
            this.minDailyTemp = minDailyTemp;
            return this;
        }

        public AggStats.Builder withAvgDailyHumidity(final Optional<Integer> avgDailyHumidity) {
            this.avgDailyHumidity = avgDailyHumidity;
            return this;
        }

        public AggStats.Builder withAvgDailyDustDensity(final Optional<Integer> avgDailyDustDensity) {
            this.avgDailyDustDensity = avgDailyDustDensity;
            return this;
        }

        public AggStats.Builder withSumCountMicroLuxHourMap(final Map<Integer, SumCountData> sumCountMicroLuxHourMap) {
            this.sumCountMicroLuxHourMap.putAll(sumCountMicroLuxHourMap);
            return this;
        }

        public AggStats build(){

            checkNotNull(accountId, "accountId cannot be null");
            checkNotNull(dateLocal, "dateLocal cannot be null");
            checkNotNull(externalDeviceId, "externalDeviceId cannot be null");

            return new AggStats(
                    this.accountId,
                    this.dateLocal,
                    this.externalDeviceId,

                    this.deviceDataCount,
                    this.trackerMotionCount,
                    this.avgDailyTemp,
                    this.maxDailyTemp,
                    this.minDailyTemp,
                    this.avgDailyHumidity,
                    this.avgDailyDustDensity,

                    this.sumCountMicroLuxHourMap);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(AggStats.class)
                .add("account_id", accountId)
                .add("date_local", dateLocal)
                .add("sense_id", externalDeviceId)
                .add("device_data_count", deviceDataCount)
                .add("tracker_motion_count", trackerMotionCount)
                .add("avg_day_temp", avgDailyTemp)
                .add("max_day_temp", maxDailyTemp)
                .add("min_day_temp", minDailyTemp)
                .add("avg_day_humid", avgDailyHumidity)
                .add("avg_day_dust_density", avgDailyDustDensity)
                .add("sum_count_mlux_hrs_map", sumCountMicroLuxHourMap)
                .toString();
    }
}
