package com.hello.suripu.core.models;

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

    public final int deviceDataCount;
    public final int trackerMotionCount;

    //all processed and calibrated values, multiplied by 10e6
    //Note: please do not add stats above 1st moment without first discussing with data science team
    public final int avgDailyTemp; //micro-celsius
    public final int maxDailyTemp; //micro-celsius
    public final int minDailyTemp; //micro-celsius
    public final int avgDailyHumidity; // percent relative humidity * 10e-6
    public final int avgDailyDustDensity; //micro-micro-gram/m^3

    public final Map<Integer, SumCountData> sumCountMicroLuxHourMap; //micro-lux, calibrated by sense color, each hour

    private AggStats(final Long accountId,
                     final DateTime dateLocal,
                     final String externalDeviceId,

                     final int deviceDataCount,
                     final int trackerMotionCount,

                     final int avgDailyTemp,
                     final int maxDailyTemp,
                     final int minDailyTemp,
                     final int avgDailyHumidity,
                     final int avgDailyDustDensity,

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

        private int deviceDataCount;
        private int trackerMotionCount;

        private int avgDailyTemp;
        private int maxDailyTemp;
        private int minDailyTemp;
        private int avgDailyHumidity;
        private int avgDailyDustDensity;

        private Map<Integer, SumCountData> sumCountMicroLuxHourMap;

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

        public AggStats.Builder withDeviceDataCount(final int deviceDataCount) {
            this.deviceDataCount = deviceDataCount;
            return this;
        }

        public AggStats.Builder withTrackerMotionCount(final int trackerMotionCount) {
            this.trackerMotionCount = trackerMotionCount;
            return this;
        }

        public AggStats.Builder withAvgDailyTemp(final int avgDailyTemp) {
            this.avgDailyTemp = avgDailyTemp;
            return this;
        }

        public AggStats.Builder withMaxDailyTemp(final int maxDailyTemp) {
            this.maxDailyTemp = maxDailyTemp;
            return this;
        }

        public AggStats.Builder withMinDailyTemp(final int minDailyTemp) {
            this.minDailyTemp = minDailyTemp;
            return this;
        }

        public AggStats.Builder withAvgDailyHumidity(final int avgDailyHumidity) {
            this.avgDailyHumidity = avgDailyHumidity;
            return this;
        }

        public AggStats.Builder withAvgDailyDustDensity(final int avgDailyDustDensity) {
            this.avgDailyDustDensity = avgDailyDustDensity;
            return this;
        }

        public AggStats.Builder withSumCountMicroLuxHourMap(final Map<Integer, SumCountData> sumCountMicroLuxHourMap) {
            this.sumCountMicroLuxHourMap = sumCountMicroLuxHourMap;
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

}
