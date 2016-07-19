package com.hello.suripu.core.models;

import com.hello.suripu.core.models.Insights.SumLengthData;
import org.joda.time.DateTime;

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

    public final int deviceDataLength;
    public final int trackerMotionLength;

    //all processed and calibrated values, multiplied by 10e6
    //Note: please do not add stats above 1st moment without first discussing with data science team
    public final int avgDailyTemp; //micro-celsius
    public final int maxDailyTemp; //micro-celsius
    public final int minDailyTemp; //micro-celsius
    public final int avgDailyHumidity; // percent relative humidity * 10e-6
    public final int avgDailyDustDensity; //micro-micro-gram/m^3

    public final int sumMicroLux22; //micro-lux, calibrated by sense color, hour 22
    public final int sumMicroLux23; //micro-lux
    public final int sumMicroLux0; //micro-lux
    public final int sumMicroLux1; //micro-lux
    public final int sumMicroLux2; //micro-lux
    public final int sumMicroLux3; //micro-lux
    public final int sumMicroLux4; //micro-lux
    public final int sumMicroLux5; //micro-lux

    public final int lenMicroLux22;
    public final int lenMicroLux23;
    public final int lenMicroLux0;
    public final int lenMicroLux1;
    public final int lenMicroLux2;
    public final int lenMicroLux3;
    public final int lenMicroLux4;
    public final int lenMicroLux5;

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

                     final int sumMicroLux22,
                     final int sumMicroLux23,
                     final int sumMicroLux0,
                     final int sumMicroLux1,
                     final int sumMicroLux2,
                     final int sumMicroLux3,
                     final int sumMicroLux4,
                     final int sumMicroLux5,

                     final int lenMicroLux22,
                     final int lenMicroLux23,
                     final int lenMicroLux0,
                     final int lenMicroLux1,
                     final int lenMicroLux2,
                     final int lenMicroLux3,
                     final int lenMicroLux4,
                     final int lenMicroLux5) {

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

        this.sumMicroLux22 = sumMicroLux22;
        this.sumMicroLux23 = sumMicroLux23;
        this.sumMicroLux0 = sumMicroLux0;
        this.sumMicroLux1 = sumMicroLux1;
        this.sumMicroLux2 = sumMicroLux2;
        this.sumMicroLux3 = sumMicroLux3;
        this.sumMicroLux4 = sumMicroLux4;
        this.sumMicroLux5 = sumMicroLux5;

        this.lenMicroLux22 = lenMicroLux22;
        this.lenMicroLux23 = lenMicroLux23;
        this.lenMicroLux0 = lenMicroLux0;
        this.lenMicroLux1 = lenMicroLux1;
        this.lenMicroLux2 = lenMicroLux2;
        this.lenMicroLux3 = lenMicroLux3;
        this.lenMicroLux4 = lenMicroLux4;
        this.lenMicroLux5 = lenMicroLux5;
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

        private int sumMicroLux22;
        private int sumMicroLux23;
        private int sumMicroLux0;
        private int sumMicroLux1;
        private int sumMicroLux2;
        private int sumMicroLux3;
        private int sumMicroLux4;
        private int sumMicroLux5;

        private int lenMicroLux22;
        private int lenMicroLux23;
        private int lenMicroLux0;
        private int lenMicroLux1;
        private int lenMicroLux2;
        private int lenMicroLux3;
        private int lenMicroLux4;
        private int lenMicroLux5;

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

        public AggStats.Builder withSumLenMicroLux22(final SumLengthData sumlenMicroLux22) {
            this.sumMicroLux22 = sumlenMicroLux22.sum;
            this.lenMicroLux22 = sumlenMicroLux22.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux23(final SumLengthData sumlenMicroLux23) {
            this.sumMicroLux23 = sumlenMicroLux23.sum;
            this.lenMicroLux23 = sumlenMicroLux23.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux0(final SumLengthData sumlenMicroLux0) {
            this.sumMicroLux0 = sumlenMicroLux0.sum;
            this.lenMicroLux0 = sumlenMicroLux0.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux1(final SumLengthData sumlenMicroLux1) {
            this.sumMicroLux1 = sumlenMicroLux1.sum;
            this.lenMicroLux1 = sumlenMicroLux1.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux2(final SumLengthData sumlenMicroLux2) {
            this.sumMicroLux2 = sumlenMicroLux2.sum;
            this.lenMicroLux2 = sumlenMicroLux2.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux3(final SumLengthData sumlenMicroLux3) {
            this.sumMicroLux3 = sumlenMicroLux3.sum;
            this.lenMicroLux3 = sumlenMicroLux3.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux4(final SumLengthData sumlenMicroLux4) {
            this.sumMicroLux4 = sumlenMicroLux4.sum;
            this.lenMicroLux4 = sumlenMicroLux4.length;
            return this;
        }

        public AggStats.Builder withSumLenMicroLux5(final SumLengthData sumlenMicroLux5) {
            this.sumMicroLux5 = sumlenMicroLux5.sum;
            this.lenMicroLux5 = sumlenMicroLux5.length;
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

                    this.sumMicroLux22,
                    this.sumMicroLux23,
                    this.sumMicroLux0,
                    this.sumMicroLux1,
                    this.sumMicroLux2,
                    this.sumMicroLux3,
                    this.sumMicroLux4,
                    this.sumMicroLux5,

                    this.lenMicroLux22,
                    this.lenMicroLux23,
                    this.lenMicroLux0,
                    this.lenMicroLux1,
                    this.lenMicroLux2,
                    this.lenMicroLux3,
                    this.lenMicroLux4,
                    this.lenMicroLux5);
        }
    }

}
