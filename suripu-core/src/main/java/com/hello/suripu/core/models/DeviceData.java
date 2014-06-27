package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceData {

    public static final float FLOAT_2_INT_MULTIPLER = 1000;

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("device_id")
    public final Long deviceId;

    @JsonProperty("ambient_temperature")
    public final int ambientTemperature;

    @JsonProperty("ambient_humidity")
    public final int ambientHumidity;

    @JsonProperty("ambient_air_quality")
    public final int ambientAirQuality;

    @JsonProperty("ambient_light")
    public final int ambientLight;

    @JsonProperty("timestamp_utc")
    public final DateTime dateTimeUTC;

    @JsonProperty("offset_millis")
    public final Integer offsetMillis;

    public DeviceData(
            final Long accountId,
            final Long deviceId,
            final int ambientTemperature,
            final int ambientHumidity,
            final int ambientAirQuality,
            final int ambientLight,
            final DateTime dateTimeUTC,
            final Integer offsetMillis) {
        this.accountId = accountId;
        this.deviceId = deviceId;
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.ambientLight = ambientLight;
        this.dateTimeUTC = dateTimeUTC;
        this.offsetMillis = offsetMillis;

        checkNotNull(this.accountId);
        checkNotNull(this.deviceId);
        checkNotNull(this.dateTimeUTC);
        checkNotNull(this.offsetMillis);
    }

    public static int floatToDBInt(final float value){
        return (int)(value * FLOAT_2_INT_MULTIPLER);
    }

    public static float dbIntToFloat(final int valueFromDB){
        return valueFromDB / FLOAT_2_INT_MULTIPLER;
    }


    public static class Builder{
        private Long accountId;
        private Long deviceId;
        private int ambientTemperature;
        private int ambientHumidity;
        private int ambientAirQuality;
        private int ambientLight;
        private DateTime dateTimeUTC;
        private Integer offsetMillis;

        public Builder withAccountId(final Long accountId){
            this.accountId = accountId;
            return this;
        }

        public Builder withDeviceId(final Long deviceId){
            this.deviceId = deviceId;
            return this;
        }

        public Builder withAmbientTemperature(final int ambientTemperature){
            this.ambientTemperature = ambientTemperature;
            return this;
        }

        public Builder withAmbientHumidity(final int ambientHumidity){
            this.ambientHumidity = ambientHumidity;
            return this;
        }

        public Builder withAmbientAirQuality(final int ambientAirQuality){
            this.ambientAirQuality = ambientAirQuality;
            return this;
        }

        public Builder withAmbientLight(final int ambientLight){
            this.ambientLight = ambientLight;
            return this;

        }

        public Builder withDateTimeUTC(final DateTime dateTimeUTC){
            this.dateTimeUTC = dateTimeUTC;
            return this;
        }


        public Builder withOffsetMillis(final Integer offsetMillis){
            this.offsetMillis = offsetMillis;
            return this;
        }

        public DeviceData build(){
            return new DeviceData(this.accountId, this.deviceId, this.ambientTemperature, this.ambientHumidity, this.ambientAirQuality, this.ambientLight, this.dateTimeUTC, this.offsetMillis);
        }


    }
}
