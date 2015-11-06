package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceData {

    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceData.class);
    private final static int DEFAULT_AMBIENT_AIR_QUALITY = 0; // Because we are now saving only raw data to the database

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("device_id")
    public final Long deviceId;

    @JsonProperty("external_device_id")
    public final String externalDeviceId;

    @JsonProperty("ambient_temperature")
    public final int ambientTemperature;

    @JsonProperty("ambient_humidity")
    public final int ambientHumidity;

    @JsonProperty("ambient_air_quality")
    public final int ambientAirQuality;

    @JsonProperty("ambient_air_quality_raw")
    public final int ambientAirQualityRaw;  // raw counts

    @JsonProperty("ambient_dust_variance")
    public final int ambientDustVariance;

    @JsonProperty("ambient_dust_min")
    public final int ambientDustMin;

    @JsonProperty("ambient_dust_max")
    public final int ambientDustMax;

    @JsonProperty("ambient_light")
    public final int ambientLight;  // raw counts when inserting to DB, lux when retrieved from DB

    @JsonIgnore
    public final float ambientLightFloat; //for internal use only; use as directed.  If symptoms persist for more than one day, consult a doctor.

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

    @JsonProperty("audio_num_disturbances")
    public final Integer audioNumDisturbances;

    @JsonProperty("audio_peak_disturbances_db")
    public final Integer audioPeakDisturbancesDB; // already converted to decibels, multiplied by 1000

    @JsonProperty("audio_peak_background_db")
    public final Integer audioPeakBackgroundDB; // already converted to decibels, multiplied by 1000

    public DateTime localTime() {
        return dateTimeUTC.plusMillis(offsetMillis);
    }

    public DeviceData withCalibratedLight(Optional<Device.Color> colorOptional) {

        Device.Color color = Device.DEFAULT_COLOR;

        if (colorOptional.isPresent()) {
            color = colorOptional.get();
        }

        final float calibratedAmbientLightFloat = DataUtils.calibrateLight(ambientLightFloat, color);
        final int calibratedAmbientLight = (int)calibratedAmbientLightFloat;

        return new DeviceData(accountId,
                deviceId,
                externalDeviceId,
                ambientTemperature,
                ambientHumidity,
                ambientAirQuality,
                ambientAirQualityRaw,
                ambientDustVariance,
                ambientDustMin,
                ambientDustMax,
                calibratedAmbientLight,
                calibratedAmbientLightFloat,
                ambientLightVariance,
                ambientLightPeakiness,
                dateTimeUTC,
                offsetMillis,
                firmwareVersion,
                waveCount,
                holdCount,
                audioNumDisturbances,
                audioPeakDisturbancesDB,
                audioPeakBackgroundDB);
    }

    @Deprecated
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
            final float ambientLightFloat,
            final int ambientLightVariance,
            final int ambientLightPeakiness,
            final DateTime dateTimeUTC,
            final Integer offsetMillis,
            final Integer firmwareVersion,
            final Integer waveCount,
            final Integer holdCount,
            final Integer audioNumDisturbances,
            final Integer audioPeakDisturbancesDB,
            final Integer audioPeakBackgroundDB) {
        this(accountId,
                deviceId,
                "",
                ambientTemperature,
                ambientHumidity,
                ambientAirQuality,
                ambientAirQualityRaw,
                ambientDustVariance,
                ambientDustMin,
                ambientDustMax,
                ambientLight,
                ambientLightFloat,
                ambientLightVariance,
                ambientLightPeakiness,
                dateTimeUTC,
                offsetMillis,
                firmwareVersion,
                waveCount,
                holdCount,
                audioNumDisturbances,
                audioPeakDisturbancesDB,
                audioPeakBackgroundDB);
    }

    public DeviceData(
            final Long accountId,
            final Long deviceId,
            final String externalDeviceId,
            final int ambientTemperature,
            final int ambientHumidity,
            final int ambientAirQuality,
            final int ambientAirQualityRaw,
            final int ambientDustVariance,
            final int ambientDustMin,
            final int ambientDustMax,
            final int ambientLight,
            final float ambientLightFloat,
            final int ambientLightVariance,
            final int ambientLightPeakiness,
            final DateTime dateTimeUTC,
            final Integer offsetMillis,
            final Integer firmwareVersion,
            final Integer waveCount,
            final Integer holdCount,
            final Integer audioNumDisturbances,
            final Integer audioPeakDisturbancesDB,
            final Integer audioPeakBackgroundDB) {
        this.accountId = accountId;
        this.deviceId = deviceId;
        this.externalDeviceId = externalDeviceId;
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.ambientAirQualityRaw = ambientAirQualityRaw;
        this.ambientDustVariance = ambientDustVariance;
        this.ambientDustMin = ambientDustMin;
        this.ambientDustMax = ambientDustMax;
        this.ambientLight = ambientLight;
        this.ambientLightFloat = ambientLightFloat;
        this.dateTimeUTC = dateTimeUTC;
        this.ambientLightVariance = ambientLightVariance;
        this.ambientLightPeakiness = ambientLightPeakiness;
        this.offsetMillis = offsetMillis;
        this.firmwareVersion = firmwareVersion;
        this.waveCount = waveCount;
        this.holdCount = holdCount;
        this.audioNumDisturbances = audioNumDisturbances;
        this.audioPeakBackgroundDB = audioPeakBackgroundDB;
        this.audioPeakDisturbancesDB = audioPeakDisturbancesDB;

        checkNotNull(this.accountId, "Account id can not be null");
//        checkNotNull(this.deviceId, "Device id can not be null");
        checkNotNull(this.dateTimeUTC, "DateTimeUTC can not be null");
        checkNotNull(this.offsetMillis,"Offset millis can not be null");
    }

    public static float dbIntToFloatDust(final int valueFromDB) {return valueFromDB / DataUtils.DUST_FLOAT_TO_INT_MULTIPLIER;}

    public static class Builder{
        private Long accountId;
        private Long deviceId;
        private String externalDeviceId;
        private int ambientTemperature;
        private int ambientHumidity;
        private int ambientAirQuality = DEFAULT_AMBIENT_AIR_QUALITY;
        private int ambientAirQualityRaw;
        private int ambientDustVariance;
        private int ambientDustMin;
        private int ambientDustMax;
        private int ambientLight;
        private float ambientLightFloat;
        private int ambientLightVariance;
        private int ambientLightPeakiness;
        private DateTime dateTimeUTC;
        private Integer offsetMillis;
        private Integer firmwareVersion;
        private Integer waveCount = 0;
        private Integer holdCount = 0;
        private Integer audioNumDisturbances = 0;
        private Integer audioPeakDisturbancesDB = 0;
        private Integer audioPeakBackgroundDB = 0;

        public Builder withAccountId(final Long accountId){
            this.accountId = accountId;
            return this;
        }

        @Deprecated
        // Prefer external device ID going forward
        public Builder withDeviceId(final Long deviceId){
            this.deviceId = deviceId;
            return this;
        }

        public Builder withExternalDeviceId(final String deviceId) {
            this.externalDeviceId = deviceId;
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

        @Deprecated
        public Builder withAmbientAirQuality(final int ambientAirQuality, final int firmwareVersion){
            LOGGER.warn("DeviceData builder's withAmbientHumidty has been deprecated, it will always set ambient air quality to zero");
            this.ambientHumidity = DEFAULT_AMBIENT_AIR_QUALITY;
            return this;
        }

        public Builder withAmbientAirQualityRaw(final int ambientAirQualityRaw){
            this.ambientAirQualityRaw  = ambientAirQualityRaw;
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

        public Builder calibrateAmbientLight(final int ambientLight){
            int lux = ambientLight;
            float fLux =DataUtils.convertLightCountsToLux(lux);
            lux = (int)fLux;

            this.ambientLight = lux;
            this.ambientLightFloat = fLux;
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

        public Builder withAudioNumDisturbances(final Integer audioNumDisturbances) {
            this.audioNumDisturbances = audioNumDisturbances;
            return this;
        }

        public Builder withAudioPeakDisturbancesDB(final Integer audioPeakDisturbancesDB) {
            final float decibelValue = DataUtils.convertAudioRawToDB(audioPeakDisturbancesDB);
            this.audioPeakDisturbancesDB = DataUtils.floatToDbIntAudioDecibels(decibelValue);
            return this;
        }

        public Builder withAudioPeakBackgroundDB(final Integer audioPeakBackgroundDB) {
            final float decibelValue = DataUtils.convertAudioRawToDB(audioPeakBackgroundDB);
            this.audioPeakBackgroundDB = DataUtils.floatToDbIntAudioDecibels(decibelValue);
            return this;
        }

        public Builder withAlreadyCalibratedAudioPeakBackgroundDB(final Integer audioPeakBackgroundDB) {
            this.audioPeakBackgroundDB = audioPeakBackgroundDB;
            return this;
        }

        public Builder withAlreadyCalibratedAudioPeakDisturbancesDB(final Integer audioPeakDisturbancesDB) {
            this.audioPeakDisturbancesDB = audioPeakDisturbancesDB;
            return this;
        }

        public DeviceData build(){
            return new DeviceData(this.accountId, this.deviceId, this.externalDeviceId, this.ambientTemperature, this.ambientHumidity,
                    this.ambientAirQuality, this.ambientAirQualityRaw, this.ambientDustVariance, this.ambientDustMin, this.ambientDustMax,
                    this.ambientLight,this.ambientLightFloat, this.ambientLightVariance, this.ambientLightPeakiness, this.dateTimeUTC, this.offsetMillis,
                    this.firmwareVersion, this.waveCount, this.holdCount,
                    this.audioNumDisturbances, this.audioPeakDisturbancesDB, this.audioPeakBackgroundDB);
        }

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(DeviceData.class)
                .add("account_id", accountId)
                .add("device_id", deviceId)
                .add("external_device_id", externalDeviceId)
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
                .add(("audio_num_disturbances"), audioNumDisturbances)
                .add(("audio_peak_disturbances_db"), audioPeakDisturbancesDB)
                .toString();
    }
}
