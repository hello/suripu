package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({"temperature", "humidity", "particulates", "light", "sound"})
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

    @JsonProperty("particulates")
    public final State particulates;

    @JsonProperty("light")
    public final State light;

    @JsonProperty("sound")
    public final State sound;

    public CurrentRoomState(final State temperature, final State humidity, final State particulates, final State light, final State sound) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.particulates = particulates;
        this.light = light;
        this.sound = sound;
    }

    public static CurrentRoomState fromRawData(final int rawTemperature, final int rawHumidity, final int rawDustMax, final int rawLight, final int rawSound,
                                               final long timestamp,
                                               final int firmwareVersion,
                                               final DateTime referenceTime,
                                               final Integer thresholdInMinutes) {

        final float humidity = DeviceData.dbIntToFloat(rawHumidity);
        final float temperature = DeviceData.dbIntToFloat(rawTemperature);
        final float particulatesAQI = Float.valueOf(DataUtils.convertRawDustCountsToAQI(rawDustMax, firmwareVersion));
        final float sound = DataUtils.convertAudioRawToDB(rawSound);
        return fromTempHumidDust(temperature, humidity, particulatesAQI, rawLight, sound, new DateTime(timestamp, DateTimeZone.UTC), referenceTime, thresholdInMinutes, DEFAULT_TEMP_UNIT);

    }

    public static CurrentRoomState fromTempHumidDust(final float temperature, final float humidity, final float particulatesAQI, final float light, final float sound,
                                                     final DateTime dataTimestampUTC,
                                                     final DateTime referenceTime,
                                                     final Integer thresholdInMinutes,
                                                     final String tempUnit) {


        LOGGER.debug("temp = {}, humidity = {}, particulates = {}", temperature, humidity, particulatesAQI);

        if(referenceTime.minusMinutes(thresholdInMinutes).getMillis() > dataTimestampUTC.getMillis()) {
            LOGGER.warn("{} is too old, not returning anything", dataTimestampUTC);
            final CurrentRoomState roomState = dataTooOld(temperature, humidity, particulatesAQI, light, sound, dataTimestampUTC);
            return roomState;
        }

        // get states
        final State temperatureState = getTemperatureState(temperature, dataTimestampUTC, tempUnit);
        final State humidityState = getHumidityState(humidity, dataTimestampUTC);
        final State particulatesState = getParticulatesState(particulatesAQI, dataTimestampUTC);
        final State lightState = getLightState(light, dataTimestampUTC);
        final State soundState = getSoundState(sound, dataTimestampUTC);

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState, lightState, soundState);
        return roomState;

    }

    /**
     * Converts BatchSensorData to a CurrentRoomState object
     * @param data
     * @return
     */
    public static CurrentRoomState fromDeviceData(final DeviceData data, final DateTime referenceTime, final Integer thresholdInMinutes, final String tempUnit) {

        final float temp = DeviceData.dbIntToFloat(data.ambientTemperature);
        final float humidity = DeviceData.dbIntToFloat(data.ambientHumidity);
        final float light = data.ambientLight; // dvt units values are already converted to lux
        final float sound = DataUtils.convertAudioRawToDB(data.audioPeakBackgroundDB);
        // max value is in raw counts, conversion needed
        final float particulatesAQI = Float.valueOf(DataUtils.convertRawDustCountsToAQI(data.ambientDustMax, data.firmwareVersion));
        return fromTempHumidDust(temp, humidity, particulatesAQI, light, sound, data.dateTimeUTC, referenceTime, thresholdInMinutes, tempUnit);

    }


    /**
     * To be used when data isn’t found for the currently logged in user
     * Possibly happening during sign up if workers haven’t processed the first data upload yet
     *
     * @return
     */
    public static CurrentRoomState empty() {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(null, English.LOADING_TEMPERATURE_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.CELCIUS),
                new State(null, English.LOADING_HUMIDITY_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.PERCENT),
                new State(null, English.LOADING_PARTICULATES_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.AQI),
                new State(null, English.LOADING_LIGHT_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.LUX),
                new State(null, English.LOADING_SOUND_MESSAGE, "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.DB)
        );

        return roomState;
    }

    public static CurrentRoomState dataTooOld(final float temperature, final float humidity,
                                              final float particulatesAQI, final float light, final float sound,
                                              final DateTime dataTimestampUTC) {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(temperature, English.UNKNOWN_TEMPERATURE_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.CELCIUS),
                new State(humidity, English.UNKNOWN_HUMIDITY_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.PERCENT),
                new State(particulatesAQI, English.UNKNOWN_PARTICULATES_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.AQI),
                new State(light, English.UNKNOWN_LIGHT_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.LUX),
                new State(sound, English.UNKNOWN_SOUND_MESSAGE, "", State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.DB)
        );
        return roomState;
    }

    public static State getTemperatureState(final float temperature, final DateTime dataTimestampUTC,  final String tempUnit) {
        // Global ideal range: 60 -- 72, less than 54 = too cold, above 75= too warm
        // TODO: personalize
        final String idealTempConditions = String.format(English.TEMPERATURE_ADVICE_MESSAGE, tempUnit, tempUnit);

        if (temperature  < 15.0) {
            return new State(temperature, English.LOW_TEMPERATURE_MESSAGE, idealTempConditions, State.Condition.ALERT, dataTimestampUTC, State.Unit.CELCIUS);
        } else if (temperature > 30.0) {
            return new State(temperature, English.HIGH_TEMPERATURE_MESSAGE, idealTempConditions, State.Condition.ALERT, dataTimestampUTC, State.Unit.CELCIUS);
        } else { // temp >= 60 && temp <= 72
            return new State(temperature, English.IDEAL_TEMPERATURE_MESSAGE, idealTempConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.CELCIUS);
        }
    }

    public static State getHumidityState(final float humidity, final DateTime dataTimestampUTC) {
        final String idealHumidityConditions = English.HUMIDITY_ADVICE_MESSAGE;

        if (humidity  < 30.0) {
            return new State(humidity, English.LOW_HUMIDITY_MESSAGE, idealHumidityConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.PERCENT);
        } else if (humidity > 60.0) {
            return new State(humidity, English.HIGH_HUMIDITY_MESSAGE, idealHumidityConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.PERCENT);
        } else { // humidity >= 30 && humidity<= 60
            return new State(humidity, English.IDEAL_HUMIDITY_MESSAGE, idealHumidityConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.PERCENT);
        }

    }

    public static State getParticulatesState(final float particulatesAQI, final DateTime dataTimestampUTC) {
        // see http://www.sparetheair.com/aqi.cfm
        final String idealParticulatesConditions = English.PARTICULATES_ADVICE_MESSAGE;

        if (particulatesAQI <= 50.0) {
            return new State(particulatesAQI, English.IDEAL_PARTICULATES_MESSAGE, idealParticulatesConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.AQI);
        } else if (particulatesAQI <= 300.0) {
            return new State(particulatesAQI, English.HIGH_PARTICULATES_MESSAGE, idealParticulatesConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.AQI);
        } else {
            return new State(particulatesAQI, English.VERY_HIGH_PARTICULATES_MESSAGE, idealParticulatesConditions, State.Condition.ALERT, dataTimestampUTC, State.Unit.AQI);
        }
    }

    public static State getLightState(final float light, final DateTime dataTimestampUTC) {
        if (light > 8.0) {
            return new State(light, English.IDEAL_LIGHT_MESSAGE, English.LIGHT_ADVICE_MESSAGE, State.Condition.ALERT, dataTimestampUTC, State.Unit.LUX);
        } else if (light > 2.0) {
            return new State(light, English.IDEAL_LIGHT_MESSAGE, English.LIGHT_ADVICE_MESSAGE, State.Condition.WARNING, dataTimestampUTC, State.Unit.LUX);
        } else {
            return new State(light, English.IDEAL_LIGHT_MESSAGE, English.LIGHT_ADVICE_MESSAGE, State.Condition.IDEAL, dataTimestampUTC, State.Unit.LUX);
        }
    }

    // TODO -- find right levels
    public static State getSoundState(final float sound, final DateTime dataTimestampUTC) {
        final State soundState = new State(sound, English.IDEAL_SOUND_MESSAGE, English.SOUND_ADVICE_MESSAGE, State.Condition.IDEAL, dataTimestampUTC, State.Unit.DB);
        return soundState;
    }


}
