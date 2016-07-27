package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.SumLengthData;
import org.joda.time.DateTime;

import java.util.List;
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
    public static final List<Integer> LIGHT_HOUR_BUCKETS = ImmutableList.copyOf(Lists.newArrayList(22, 23, 0, 1, 2, 3, 4, 5));

    public final Long accountId;
    public final DateTime dateLocal;
    public final String externalDeviceId;

    public final int deviceDataLength;
    public final int trackerMotionLength;

    //all processed and calibrated values, multiplied by 10e6
    //Note: please do not add stats above 1st moment without first discussing with data science team
    public final int avgDailyTemp; //micro-celsius
    public final int maxDailyTemp; //micro-celsius
    public final int minDailyTemp; //micro-celsius
    public final int avgDailyHumidity; // percent relative humidity * 10e-6
    public final int avgDailyDustDensity; //micro-micro-gram/m^3

    public final Map<Integer, SumLengthData> sumLengthMicroLuxHourMap; //micro-lux, calibrated by sense color, each hour

    private AggStats(final Long accountId,
                     final DateTime dateLocal,
                     final String externalDeviceId,

                     final int deviceDataLength,
                     final int trackerMotionLength,

                     final int avgDailyTemp,
                     final int maxDailyTemp,
                     final int minDailyTemp,
                     final int avgDailyHumidity,
                     final int avgDailyDustDensity,

                     final Map<Integer, SumLengthData> sumLengthMicroLuxHourMap) {

        this.accountId = accountId;
        this.dateLocal = dateLocal;
        this.externalDeviceId = externalDeviceId;

        this.deviceDataLength = deviceDataLength;
        this.trackerMotionLength = trackerMotionLength;

        this.avgDailyTemp = avgDailyTemp;
        this.maxDailyTemp = maxDailyTemp;
        this.minDailyTemp = minDailyTemp;
        this.avgDailyHumidity = avgDailyHumidity;
        this.avgDailyDustDensity = avgDailyDustDensity;

        this.sumLengthMicroLuxHourMap = sumLengthMicroLuxHourMap;
    }

    public static class Builder {
        private Long accountId;
        private DateTime dateLocal;
        private String externalDeviceId;

        private int deviceDataLength;
        private int trackerMotionLength;

        private int avgDailyTemp;
        private int maxDailyTemp;
        private int minDailyTemp;
        private int avgDailyHumidity;
        private int avgDailyDustDensity;

        private Map<Integer, SumLengthData> sumLengthMicroLuxHourMap;

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

        public AggStats.Builder withDeviceDataLength(final int deviceDataLength) {
            this.deviceDataLength = deviceDataLength;
            return this;
        }

        public AggStats.Builder withTrackerMotionLength(final int trackerMotionLength) {
            this.trackerMotionLength = trackerMotionLength;
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

        public AggStats.Builder withSumLenMicroLuxHourMap(final Map<Integer, SumLengthData> sumLengthMicroLuxHourMap) {
            this.sumLengthMicroLuxHourMap = sumLengthMicroLuxHourMap;
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

                    this.deviceDataLength,
                    this.trackerMotionLength,
                    this.avgDailyTemp,
                    this.maxDailyTemp,
                    this.minDailyTemp,
                    this.avgDailyHumidity,
                    this.avgDailyDustDensity,

                    this.sumLengthMicroLuxHourMap);
        }
    }

}
