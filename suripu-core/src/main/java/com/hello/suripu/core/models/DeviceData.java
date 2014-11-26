package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceData {

    public static final float FLOAT_2_INT_MULTIPLIER = 100;


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

    @JsonProperty("ambient_air_quality_raw")
    public final int ambientAirQualityRaw;

    @JsonProperty("ambient_dust_variance")
    public final int ambientDustVariance;

    @JsonProperty("ambient_dust_min")
    public final int ambientDustMin;

    @JsonProperty("ambient_dust_max")
    public final int ambientDustMax;

    @JsonProperty("ambient_light")
    public final int ambientLight;

    @JsonProperty("ambient_light_variance")
    public final int ambientLightVariance;

    @JsonProperty("ambient_light_peakiness")
    public final int ambientLightPeakiness;

    @JsonProperty("timestamp_utc")
    public final DateTime dateTimeUTC;

    @JsonProperty("offset_millis")
    public final Integer offsetMillis;

    @JsonIgnore
    public final Integer firmwareVersion;

    @JsonProperty("wave_count")
    public final Integer waveCount;

    @JsonProperty("hold_count")
    public final Integer holdCount;

    public DeviceData(
            final Long accountId,
            final Long deviceId,
            final int ambientTemperature,
            final int ambientHumidity,
            final int ambientAirQuality,
            final int ambientAirQualityRaw,
            final int ambientDustVariance,
            final int ambientDustMin,
            final int ambientDustMax,
            final int ambientLight,
            final int ambientLightVariance,
            final int ambientLightPeakiness,
            final DateTime dateTimeUTC,
            final Integer offsetMillis,
            final Integer firmwareVersion,
            final Integer waveCount,
            final Integer holdCount) {
        this.accountId = accountId;
        this.deviceId = deviceId;
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.ambientAirQualityRaw = ambientAirQualityRaw;
        this.ambientDustVariance = ambientDustVariance;
        this.ambientDustMin = ambientDustMin;
        this.ambientDustMax = ambientDustMax;
        this.ambientLight = ambientLight;
        this.dateTimeUTC = dateTimeUTC;
        this.ambientLightVariance = ambientLightVariance;
        this.ambientLightPeakiness = ambientLightPeakiness;
        this.offsetMillis = offsetMillis;
        this.firmwareVersion = firmwareVersion;
        this.waveCount = waveCount;
        this.holdCount = holdCount;

        checkNotNull(this.accountId);
        checkNotNull(this.deviceId);
        checkNotNull(this.dateTimeUTC);
        checkNotNull(this.offsetMillis);
    }

    public static int floatToDBInt(final float value){
        return (int)(value * FLOAT_2_INT_MULTIPLIER);
    }

    public static float dbIntToFloat(final int valueFromDB){
        return valueFromDB / FLOAT_2_INT_MULTIPLIER;
    }

    public static float dbIntToFloatDust(final int valueFromDB) {return valueFromDB / DataUtils.DUST_FLOAT_TO_INT_MULTIPLIER;}

    public static class Builder{
        private Long accountId;
        private Long deviceId;
        private int ambientTemperature;
        private int ambientHumidity;
        private int ambientAirQuality;
        private int ambientAirQualityRaw;
        private int ambientDustVariance;
        private int ambientDustMin;
        private int ambientDustMax;
        private int ambientLight;
        private int ambientLightVariance;
        private int ambientLightPeakiness;
        private DateTime dateTimeUTC;
        private Integer offsetMillis;
        private Integer firmwareVersion;
        private Integer waveCount;
        private Integer holdCount;

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

        public Builder withAmbientAirQuality(final int ambientAirQuality, final int firmwareVersion){
            final float dustDensity = DataUtils.convertDustDataFromCountsToDensity(ambientAirQuality, firmwareVersion); // milligrams
            this.ambientAirQuality = DataUtils.floatToDbIntDust(dustDensity); // multiplier to save as Int
            return this;
        }

        public Builder withAmbientAirQualityRaw(final int ambientAirQuality) {
            this.ambientAirQualityRaw = ambientAirQuality;
            return this;
        }

        public Builder withAmbientDustVariance(final int ambientDustVariance) {
            this.ambientDustVariance= ambientDustVariance;
            return this;
        }

        public Builder withAmbientDustMin(final int ambientDustMin) {
            this.ambientDustMin= ambientDustMin;
            return this;
        }

        public Builder withAmbientDustMax(final int ambientDustMax) {
            this.ambientDustMax = ambientDustMax;
            return this;
        }

        public Builder withAmbientLight(final int ambientLight){
            this.ambientLight = ambientLight;
            return this;
        }

        public Builder withAmbientLightVariance(final int ambientLightVariance){
            this.ambientLightVariance = ambientLightVariance;
            return this;
        }

        public Builder withAmbientLightPeakiness(final int ambientLightPeakiness){
            this.ambientLightPeakiness = ambientLightPeakiness;
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

        public Builder withFirmwareVersion(final Integer firmwareVersion){
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder withWaveCount(final Integer waveCount){
            this.waveCount = waveCount;
            return this;
        }

        public Builder withHoldCount(final Integer holdCount){
            this.holdCount = holdCount;
            return this;
        }

        public DeviceData build(){
            return new DeviceData(this.accountId, this.deviceId, this.ambientTemperature, this.ambientHumidity,
                    this.ambientAirQuality, this.ambientAirQualityRaw, this.ambientDustVariance, this.ambientDustMin, this.ambientDustMax,
                    this.ambientLight, this.ambientLightVariance, this.ambientLightPeakiness, this.dateTimeUTC, this.offsetMillis,
                    this.firmwareVersion, this.waveCount, this.holdCount);
        }


    }

    @Override
    public String toString() {
        return Objects.toStringHelper(DeviceData.class)
                .add("account_id", accountId)
                .add("device_id", deviceId)
                .add("ambient_temperature", ambientTemperature)
                .add("ambient_humidity", ambientHumidity)
                .add("ambient_light", ambientLight)
                .add("ambient_light_variance", ambientLightVariance)
                .add("ambient_light_peakiness", ambientLightPeakiness)
                .add("ambient_air_quality", ambientAirQuality)
                .add("ambient_air_quality_raw", ambientAirQualityRaw)
                .add("ambient_dust_variance", ambientDustVariance)
                .add("ambient_dust_min", ambientDustMin)
                .add("ambient_dust_max", ambientDustMax)
                .add("dateTimeUTC", dateTimeUTC)
                .add("offset_millis", offsetMillis)
                .add("firmware_version", firmwareVersion)
                .add("wave_count", waveCount)
                .add("hold_count", holdCount)
                .toString();
    }
}
