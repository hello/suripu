package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({"temperature", "humidity", "light", "sound"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentRoomState {


    public final static String DEFAULT_TEMP_UNIT = "c";
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentRoomState.class);

    public static class State {

        public enum Unit {
            CELCIUS("c"),
            PERCENT("%"),
            PPM("ppm"),
            MICRO_G_M3("µg/m3"),
            AQI("AQI"),
            LUX("lux"),
            DB("dB");

            private final String value;
            private Unit(final String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }
        public enum Condition {
            UNKNOWN(0),
            IDEAL(1),
            WARNING(2),
            ALERT(3),
            IDEAL_EXCLUDING_LIGHT(4);

            private final int value;

            private Condition(final int value) {
                this.value = value;
            }
        }

        @JsonProperty("value")
        public final Float value;

        @JsonProperty("message")
        public final String message;

        @JsonProperty("ideal_conditions")
        public final String idealConditions;

        @JsonProperty("condition")
        public final Condition condition;

        @JsonProperty("last_updated_utc")
        public final DateTime lastUpdated;

        @JsonProperty("unit")
        public final Unit unit;

        public State(final Float value, final String message, final String idealConditions, final Condition condition, final DateTime lastUpdated, final Unit unit) {
            this.value = value;
            this.message = message;
            this.idealConditions = idealConditions;
            this.condition = condition;
            this.lastUpdated = lastUpdated;
            this.unit = unit;
        }
    }

    @JsonProperty("temperature")
    public final State temperature;

    @JsonProperty("humidity")
    public final State humidity;

    @JsonIgnore
    public final State particulates;

    @JsonProperty("particulates")
    public State particulates() {
        return (hasDust) ? particulates : null;
    }

    @JsonProperty("light")
    public final State light;

    @JsonProperty("sound")
    public final State sound;

    @JsonIgnore
    public final Boolean hasDust;

    public CurrentRoomState(final State temperature, final State humidity, final State particulates, final State light, final State sound) {
        this(temperature, humidity, particulates, light, sound, Boolean.FALSE);
    }


    public CurrentRoomState(final State temperature, final State humidity, final State particulates, final State light, final State sound, final Boolean hasDust) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.particulates = particulates;
        this.light = light;
        this.sound = sound;
        this.hasDust = hasDust;
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
        final float sound = DataUtils.calibrateAudio(DataUtils.convertAudioRawToDB(rawBackgroundNoise), DataUtils.convertAudioRawToDB(rawPeakNoise));
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

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState, lightState, soundState);
        return roomState;

    }

    public static CurrentRoomState fromDeviceData(final DeviceData data, final DateTime referenceTime, final Integer thresholdInMinutes, final String tempUnit, final Optional<Calibration> calibrationOptional) {

        final float temp = DataUtils.calibrateTemperature(data.ambientTemperature);
        final float humidity = DataUtils.calibrateHumidity(data.ambientTemperature, data.ambientHumidity);
        final float light = data.ambientLight; // dvt units values are already converted to lux
        final float sound = DataUtils.calibrateAudio(DataUtils.dbIntToFloatAudioDecibels(data.audioPeakBackgroundDB), DataUtils.dbIntToFloatAudioDecibels(data.audioPeakDisturbancesDB));
        // max value is in raw counts, conversion needed
        final float particulates = DataUtils.convertRawDustCountsToDensity(data.ambientAirQualityRaw, calibrationOptional);
        return fromTempHumidDustLightSound(temp, humidity, particulates, light, sound, data.dateTimeUTC, referenceTime, thresholdInMinutes, tempUnit);

    }

    /**
     * To be used when data isn’t found for the currently logged in user
     * Possibly happening during sign up if workers haven’t processed the first data upload yet
     *
     * @return
     */
    public static CurrentRoomState empty(final Boolean hasDust) {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(null, English.LOADING_TEMPERATURE_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.CELCIUS),
                new State(null, English.LOADING_HUMIDITY_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.PERCENT),
                new State(null, English.LOADING_PARTICULATES_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.AQI), // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
                new State(null, English.LOADING_LIGHT_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.LUX),
                new State(null, English.LOADING_SOUND_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.DB),
                hasDust
        );

        return roomState;
    }

    public static CurrentRoomState dataTooOld(final float temperature, final float humidity,
                                              final float particulates, final float light, final float sound,
                                              final DateTime dataTimestampUTC) {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(temperature, English.UNKNOWN_TEMPERATURE_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.CELCIUS),
                new State(humidity, English.UNKNOWN_HUMIDITY_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.PERCENT),
                new State(particulates, English.UNKNOWN_PARTICULATES_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.AQI), // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
                new State(light, English.UNKNOWN_LIGHT_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.LUX),
                new State(sound, English.UNKNOWN_SOUND_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.DB)
        );
        return roomState;
    }

    public static State getTemperatureState(final float temperature, final DateTime dataTimestampUTC, final String tempUnit, final Boolean preSleep) {
        // Global ideal range: 60 -- 70, less than 54 = too cold, above 75= too warm
        // TODO: personalize the range

        String idealTempConditions;
        if (tempUnit.equals(DEFAULT_TEMP_UNIT)) {
            idealTempConditions = String.format(English.TEMPERATURE_ADVICE_MESSAGE_C,
                    TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS, TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS);
        } else {
            idealTempConditions = String.format(English.TEMPERATURE_ADVICE_MESSAGE_F,
                    TemperatureHumidity.IDEAL_TEMP_MIN, TemperatureHumidity.IDEAL_TEMP_MAX);

        }

        State.Condition condition = State.Condition.IDEAL;
        String message = (preSleep) ? English.IDEAL_TEMPERATURE_PRE_SLEEP_MESSAGE: English.IDEAL_TEMPERATURE_MESSAGE;

        if (temperature > (float) TemperatureHumidity.ALERT_TEMP_MAX_CELSIUS) {
            condition = State.Condition.ALERT;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_HIGH;
            message = (preSleep) ? English.HIGH_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE: English.HIGH_TEMPERATURE_ALERT_MESSAGE;

        } else if (temperature > (float) TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS) {
            condition = State.Condition.WARNING;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_HIGH;
            message = (preSleep) ? English.HIGH_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE: English.HIGH_TEMPERATURE_WARNING_MESSAGE;

        } else if (temperature  < (float) TemperatureHumidity.ALERT_TEMP_MIN_CELSIUS) {
            condition = State.Condition.ALERT;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_LOW;
            message = (preSleep) ? English.LOW_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE: English.LOW_TEMPERATURE_ALERT_MESSAGE;

        } else if (temperature < (float) TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS) {
            condition = State.Condition.WARNING;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_LOW;
            message = (preSleep) ? English.LOW_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE: English.LOW_TEMPERATURE_WARNING_MESSAGE;
        }

        return new State(temperature, message, idealTempConditions, condition, dataTimestampUTC, State.Unit.CELCIUS);
    }

    public static State getHumidityState(final float humidity, final DateTime dataTimestampUTC, final Boolean preSleep) {

        State.Condition condition = State.Condition.IDEAL;;
        String idealHumidityConditions = English.HUMIDITY_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_HUMIDITY_PRE_SLEEP_MESSAGE: English.IDEAL_HUMIDITY_MESSAGE;

        if (humidity < (float) TemperatureHumidity.ALERT_HUMIDITY_LOW) {
            condition = State.Condition.ALERT;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_LOW;
            message = (preSleep) ? English.LOW_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE : English.LOW_HUMIDITY_ALERT_MESSAGE;

        } else if (humidity  < (float) TemperatureHumidity.IDEAL_HUMIDITY_MIN) {
            condition = State.Condition.WARNING;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_LOW;
            message = (preSleep) ? English.LOW_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE : English.LOW_HUMIDITY_WARNING_MESSAGE;

        } else if (humidity > (float) TemperatureHumidity.IDEAL_HUMIDITY_MAX) {
            condition = State.Condition.WARNING;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_HIGH;
            message = (preSleep) ? English.HIGH_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE : English.HIGH_HUMIDITY_WARNING_MESSAGE;

        } else if (humidity > (float) TemperatureHumidity.ALERT_HUMIDITY_HIGH) {
            condition = State.Condition.ALERT;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_HIGH;
            message = (preSleep) ? English.HIGH_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE : English.HIGH_HUMIDITY_ALERT_MESSAGE;
        }

        return new State(humidity, message, idealHumidityConditions, condition, dataTimestampUTC, State.Unit.PERCENT);
    }

    public static State getParticulatesState(final float particulates, final DateTime dataTimestampUTC, final Boolean preSleep) {
        // see http://www.sparetheair.com/aqi.cfm

        String idealParticulatesConditions = English.PARTICULATES_ADVICE_MESSAGE;
        State.Condition condition = State.Condition.ALERT;
        String message = (preSleep) ? English.VERY_HIGH_PARTICULATES_PRE_SLEEP_MESSAGE: English.VERY_HIGH_PARTICULATES_MESSAGE;;

        if (particulates <= Particulates.PARTICULATE_DENSITY_MAX_IDEAL) {
            condition = State.Condition.IDEAL;
            message = (preSleep) ? English.IDEAL_PARTICULATES_PRE_SLEEP_MESSAGE : English.IDEAL_PARTICULATES_MESSAGE;
        } else if (particulates <= Particulates.PARTICULATE_DENSITY_MAX_WARNING) {
            condition = State.Condition.WARNING;
            message = (preSleep) ? English.HIGH_PARTICULATES_PRE_SLEEP_MESSAGE : English.HIGH_PARTICULATES_MESSAGE;
        }


        if (condition != State.Condition.IDEAL && !preSleep) {
            idealParticulatesConditions += English.RECOMMENDATION_PARTICULATES_TOO_HIGH;
        }

        return new State(particulates, message, idealParticulatesConditions, condition, dataTimestampUTC, State.Unit.AQI); // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
    }

    public static State getLightState(final float light, final DateTime dataTimestampUTC, final Boolean preSleep) {
        State.Condition condition = State.Condition.IDEAL;;
        String idealConditions = English.LIGHT_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_LIGHT_PRE_SLEEP_MESSAGE: English.IDEAL_LIGHT_MESSAGE;;

        if (light >  Lights.LIGHT_LEVEL_ALERT) {
            condition = State.Condition.ALERT;
            idealConditions += English.RECOMMENDATION_LIGHT_TOO_HIGH;
            message = (preSleep) ? English.ALERT_LIGHT_PRE_SLEEP_MESSAGE : English.ALERT_LIGHT_MESSAGE;

        } else if (light > Lights.LIGHT_LEVEL_WARNING) {
            condition = State.Condition.WARNING;
            idealConditions += English.RECOMMENDATION_LIGHT_TOO_HIGH;
            message = (preSleep) ? English.WARNING_LIGHT_PRE_SLEEP_MESSAGE: English.WARNING_LIGHT_MESSAGE;
        }

        return new State(light, message, idealConditions, condition, dataTimestampUTC, State.Unit.LUX);
    }

    public static State getSoundState(final float sound, final DateTime dataTimestampUTC, final Boolean preSleep) {
        // see http://www.noisehelp.com/noise-level-chart.html

        State.Condition condition = State.Condition.IDEAL;;
        String idealSoundCondition = English.SOUND_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_SOUND_PRE_SLEEP_MESSAGE: English.IDEAL_SOUND_MESSAGE;;

        if (sound > 90.0) {
            // lawn mower
            condition = State.Condition.ALERT;
            idealSoundCondition += English.RECOMMENDATION_SOUND_TOO_HIGH;
            message = (preSleep) ? English.ALERT_SOUND_PRE_SLEEP_MESSAGE : English.ALERT_SOUND_MESSAGE;
        } else if (sound > 40.0) {
            condition = State.Condition.WARNING;
            idealSoundCondition += English.RECOMMENDATION_SOUND_TOO_HIGH;
            message = (preSleep) ? English.WARNING_SOUND_PRE_SLEEP_MESSAGE: English.WARNING_SOUND_MESSAGE;
        }

        return new State(sound, message, idealSoundCondition, condition, dataTimestampUTC, State.Unit.DB);
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
