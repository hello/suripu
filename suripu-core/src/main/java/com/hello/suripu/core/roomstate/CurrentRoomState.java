package com.hello.suripu.core.roomstate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.roomstate.classifiers.classic.HumidityClassifier;
import com.hello.suripu.core.roomstate.classifiers.classic.LightClassifier;
import com.hello.suripu.core.roomstate.classifiers.classic.ParticulatesClassifier;
import com.hello.suripu.core.roomstate.classifiers.classic.SoundClassifier;
import com.hello.suripu.core.roomstate.classifiers.classic.TemperatureClassifier;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder({"temperature", "humidity", "light", "sound"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentRoomState {


    final static Map<String, Classifier> classifiers;
    static {
        final Map<String, Classifier> temp = new HashMap<>();
        temp.put("temperature", new TemperatureClassifier());
        temp.put("humidity", new HumidityClassifier());
        temp.put("light", new LightClassifier());
        temp.put("sound", new SoundClassifier());
        temp.put("particulates", new ParticulatesClassifier());
        classifiers = ImmutableMap.copyOf(temp);
    }

    public final static String DEFAULT_TEMP_UNIT = "c";
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentRoomState.class);

    private final State temperature;
    private final State humidity;
    private final State light;
    private final State sound;
    private final State particulates;
    private final Boolean showDust;

    @JsonProperty("temperature")
    public State temperature() {
        return temperature;
    }

    @JsonProperty("humidity")
    public State humidity() {
        return humidity;
    }

    @JsonProperty("light")
    public State light() {
        return light;
    }

    @JsonProperty("sound")
    public State sound() {
        return sound;
    }

    @JsonProperty("particulates")
    public State particulates() {
        return (showDust) ? particulates : null;
    }

    public CurrentRoomState(final State temperature, final State humidity, final State particulates, final State light, final State sound) {
        this(temperature, humidity, particulates, light, sound, Boolean.FALSE);
    }


    public CurrentRoomState(final State temperature, final State humidity, final State particulates, final State light, final State sound, final Boolean showDust) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.particulates = particulates;
        this.light = light;
        this.sound = sound;
        this.showDust = showDust;
    }

    public CurrentRoomState withDust(final Boolean hasDust) {
        return new CurrentRoomState(temperature, humidity, particulates, light, sound, hasDust);
    }

    public static CurrentRoomState fromRawData(final int rawTemperature, final int rawHumidity, final int rawDustMax, final int rawLight, final int rawBackgroundNoise, final int rawPeakNoise,
                                               final long timestamp,
                                               final int firmwareVersion,
                                               final DateTime referenceTime,
                                               final Integer thresholdInMinutes,
                                               final Optional<Calibration> calibrationOptional) {

        final float humidity = DataUtils.calibrateHumidity(rawTemperature, rawHumidity);
        final float temperature = DataUtils.calibrateTemperature(rawTemperature);
        final float particulates = DataUtils.convertRawDustCountsToDensity(rawDustMax, calibrationOptional);
        final float sound = DataUtils.calibrateAudio(DataUtils.convertAudioRawToDB(rawBackgroundNoise), DataUtils.convertAudioRawToDB(rawPeakNoise), firmwareVersion);
        return fromTempHumidDustLightSound(temperature, humidity, particulates, rawLight, sound, new DateTime(timestamp, DateTimeZone.UTC), referenceTime, thresholdInMinutes, DEFAULT_TEMP_UNIT);

    }



    public static CurrentRoomState fromTempHumidDustLightSound(final float temperature, final float humidity, final float particulates, final float light, final float sound,
                                                               final DateTime dataTimestampUTC,
                                                               final DateTime referenceTime,
                                                               final Integer thresholdInMinutes,
                                                               final String tempUnit) {


        LOGGER.debug("temp = {}, humidity = {}, particulates = {}, light = {}, sound = {}", temperature, humidity, particulates, light, sound);

        if(referenceTime.minusMinutes(thresholdInMinutes).getMillis() > dataTimestampUTC.getMillis()) {
            LOGGER.warn("{} is too old, not returning anything", dataTimestampUTC);
            final CurrentRoomState roomState = dataTooOld(temperature, humidity, particulates, light, sound, dataTimestampUTC);
            return roomState;
        }

        // get states
        final State temperatureState = getTemperatureState(temperature, dataTimestampUTC, tempUnit, false);
        final State humidityState = getHumidityState(humidity, dataTimestampUTC, false);
        final State particulatesState = getParticulatesState(particulates, dataTimestampUTC, false);
        final State lightState = getLightState(light, dataTimestampUTC, false);
        final State soundState = getSoundState(sound, dataTimestampUTC, false);

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState, lightState, soundState, true);
        return roomState;

    }

    public static CurrentRoomState fromDeviceData(
            final DeviceData data, final DateTime referenceTime, final Integer thresholdInMinutes,
            final String tempUnit, final Optional<Calibration> calibrationOptional, final float minSoundDb)
    {

        final float temp = DataUtils.calibrateTemperature(data.ambientTemperature);
        final float humidity = DataUtils.calibrateHumidity(data.ambientTemperature, data.ambientHumidity);
        final float light = data.ambientLightFloat; // dvt units values are already converted to lux
        final float sound = DataUtils.calibrateAudio(DataUtils.dbIntToFloatAudioDecibels(data.audioPeakBackgroundDB), DataUtils.dbIntToFloatAudioDecibels(data.audioPeakDisturbancesDB), data.firmwareVersion);
        final float soundFloored = Math.max(sound, minSoundDb);
        // max value is in raw counts, conversion needed
        final float particulates = DataUtils.convertRawDustCountsToDensity(data.ambientAirQualityRaw, calibrationOptional);
        return fromTempHumidDustLightSound(temp, humidity, particulates, light, soundFloored, data.dateTimeUTC, referenceTime, thresholdInMinutes, tempUnit);

    }

    /**
     * To be used when data isn’t found for the currently logged in user
     * Possibly happening during sign up if workers haven’t processed the first data upload yet
     *
     * @return
     */
    public static CurrentRoomState empty(final Boolean hasDust) {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(null, English.LOADING_TEMPERATURE_MESSAGE, "", Condition.UNKNOWN, DateTime.now(), State.Unit.CELCIUS),
                new State(null, English.LOADING_HUMIDITY_MESSAGE, "", Condition.UNKNOWN, DateTime.now(), State.Unit.PERCENT),
                new State(null, English.LOADING_PARTICULATES_MESSAGE, "", Condition.UNKNOWN, DateTime.now(), State.Unit.AQI), // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
                new State(null, English.LOADING_LIGHT_MESSAGE, "", Condition.UNKNOWN, DateTime.now(), State.Unit.LUX),
                new State(null, English.LOADING_SOUND_MESSAGE, "", Condition.UNKNOWN, DateTime.now(), State.Unit.DB),
                hasDust
        );

        return roomState;
    }

    public static CurrentRoomState dataTooOld(final float temperature, final float humidity,
                                              final float particulates, final float light, final float sound,
                                              final DateTime dataTimestampUTC) {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(temperature, English.UNKNOWN_TEMPERATURE_MESSAGE, "", Condition.UNKNOWN, dataTimestampUTC, State.Unit.CELCIUS),
                new State(humidity, English.UNKNOWN_HUMIDITY_MESSAGE, "", Condition.UNKNOWN, dataTimestampUTC, State.Unit.PERCENT),
                new State(particulates, English.UNKNOWN_PARTICULATES_MESSAGE, "", Condition.UNKNOWN, dataTimestampUTC, State.Unit.AQI), // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
                new State(light, English.UNKNOWN_LIGHT_MESSAGE, "", Condition.UNKNOWN, dataTimestampUTC, State.Unit.LUX),
                new State(sound, English.UNKNOWN_SOUND_MESSAGE, "", Condition.UNKNOWN, dataTimestampUTC, State.Unit.DB)
        );
        return roomState;
    }

    public static State getTemperatureState(final float temperature, final DateTime dataTimestampUTC, final String tempUnit, final Boolean preSleep) {
        // Global ideal range: 60 -- 70, less than 54 = too cold, above 75= too warm
        // TODO: personalize the range
        return classifiers.get("temperature").classify(temperature,dataTimestampUTC,preSleep, tempUnit);
    }

    public static State getHumidityState(final float humidity, final DateTime dataTimestampUTC, final Boolean preSleep) {
        return classifiers.get("humidity").classify(humidity,dataTimestampUTC,preSleep, "");
    }

    public static State getParticulatesState(final float particulates, final DateTime dataTimestampUTC, final Boolean preSleep) {
        return classifiers.get("particulates").classify(particulates,dataTimestampUTC,preSleep, "");
    }

    public static State getLightState(final float light, final DateTime dataTimestampUTC, final Boolean preSleep) {
        return classifiers.get("light").classify(light,dataTimestampUTC,preSleep, "");
    }

    public static State getSoundState(final float sound, final DateTime dataTimestampUTC, final Boolean preSleep) {
        return classifiers.get("sound").classify(sound,dataTimestampUTC,preSleep, "");
    }

    public static Optional<State> getSensorState(final Sensor sensor,
                                                 final float value,
                                                 final DateTime dataTimestampUTC,
                                                 final boolean preSleep) {
        switch (sensor) {
            case LIGHT:
                return Optional.of(CurrentRoomState.getLightState(value, dataTimestampUTC, preSleep));
            case SOUND:
                return Optional.of(CurrentRoomState.getSoundState(value, dataTimestampUTC, preSleep));
            case HUMIDITY:
                return Optional.of(CurrentRoomState.getHumidityState(value, dataTimestampUTC, preSleep));
            case TEMPERATURE:
                return Optional.of(CurrentRoomState.getTemperatureState(value, dataTimestampUTC, CurrentRoomState.DEFAULT_TEMP_UNIT, preSleep));
            case PARTICULATES:
                return Optional.of(CurrentRoomState.getParticulatesState(value, dataTimestampUTC, preSleep));
            default:
                return Optional.absent();
        }
    }
}
