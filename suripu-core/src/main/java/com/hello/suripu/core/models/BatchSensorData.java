package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

public class BatchSensorData {

    public final Long accountId;
    public final List<Integer> ambientTemp;
    public final List<Integer> ambientAirQuality;
    public final List<Integer> ambientHumidity;
    public final List<Integer> ambientLight;
    public final DateTime dateTime;
    public final Integer offsetMillis;

    /**
     * Device batch
     * @param accountId
     * @param ambientTemp
     * @param ambientAirQuality
     * @param ambientHumidity
     * @param ambientLight
     * @param dateTime
     * @param offsetMillis
     */
    public BatchSensorData(
            final Long accountId,
            final List<Integer> ambientTemp,
            final List<Integer> ambientAirQuality,
            final List<Integer> ambientHumidity,
            final List<Integer> ambientLight,
            final DateTime dateTime,
            final Integer offsetMillis) {

        this.accountId = accountId;

        this.ambientTemp = ImmutableList.copyOf(ambientTemp);
        this.ambientAirQuality = ImmutableList.copyOf(ambientAirQuality);
        this.ambientHumidity = ImmutableList.copyOf(ambientHumidity);
        this.ambientLight = ImmutableList.copyOf(ambientLight);

        this.dateTime = dateTime;
        this.offsetMillis = offsetMillis;
    }


    public static class Builder {

        private Long accountId;
        private List<Integer> ambientTemp = new ArrayList<Integer>();
        private List<Integer> ambientAirQuality = new ArrayList<Integer>();
        private List<Integer> ambientHumidity = new ArrayList<Integer>();
        private List<Integer> ambientLight = new ArrayList<Integer>();
        private DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        private Integer offsetMillis;


        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withAmbientTemp(final List<Integer> temp) {
            this.ambientTemp = temp;
            return this;
        }

        public Builder withAmbientAirQuality(final List<Integer> airQuality) {
            this.ambientAirQuality = airQuality;
            return this;
        }

        public Builder withAmbientHumidity(final List<Integer> humidity) {
            this.ambientHumidity = humidity;
            return this;
        }

        public Builder withAmbientLight(final List<Integer> light) {
            this.ambientLight = light;
            return this;
        }

        public Builder withDateTime(final DateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Builder withOffsetMillis(final Integer offsetMillis) {
            this.offsetMillis = offsetMillis;
            return this;
        }

        public BatchSensorData build() {
            return new BatchSensorData(accountId, ambientTemp, ambientAirQuality, ambientHumidity, ambientLight, dateTime, offsetMillis);
        }
    }
}
