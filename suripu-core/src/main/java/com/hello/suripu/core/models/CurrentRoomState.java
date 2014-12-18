package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentRoomState {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentRoomState.class);

    public static class State {

        public enum Unit {
            CELCIUS("c"),
            PERCENT("%"),
            PPM("ppm"),
            MICRO_G_M3("µg/m3"),
            AQI("AQI");

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
            ALERT(3);

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


    public CurrentRoomState(final State temperature, final State humidity, final State particulates) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.particulates = particulates;
    }

    public static CurrentRoomState fromRawData(final int rawTemperature, final int rawHumidity, final int rawDustMax,
                                               final long timestamp,
                                               final int firmwareVersion,
                                               final DateTime referenceTime,
                                               final Integer thresholdInMinutes) {
        final float humidity = DeviceData.dbIntToFloat(rawHumidity);
        final float temperature = DeviceData.dbIntToFloat(rawTemperature);
        final float particulatesAQI = Float.valueOf(DataUtils.convertRawDustCountsToAQI(rawDustMax, firmwareVersion));
        return fromTempHumidDust(temperature, humidity, particulatesAQI, new DateTime(timestamp, DateTimeZone.UTC), referenceTime, thresholdInMinutes);

    }

    public static CurrentRoomState fromTempHumidDust(final float temperature, final float humidity, final float particulatesAQI,
                                                     final DateTime dataTimestampUTC,
                                                     final DateTime referenceTime,
                                                     final Integer thresholdInMinutes) {

        State temperatureState;
        State humidityState;
        State particulatesState;

        LOGGER.debug("temp = {}, humidity = {}, particulates = {}", temperature, humidity, particulatesAQI);


        final String idealTempConditions = "You sleep better when temperature is between **XX** and **YY**.";
        final String idealHumidityConditions = "You sleep better when humidity is between **XX** and **YY**.";
        final String idealParticulatesConditions = "You sleep better when particulates are below **XX**.";

        if(referenceTime.minusMinutes(thresholdInMinutes).getMillis() > dataTimestampUTC.getMillis()) {

            LOGGER.warn("{} is too old, not returning anything", dataTimestampUTC);
            temperatureState = new State(temperature, "Could not retrieve a recent temperature reading", idealTempConditions, State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.CELCIUS);
            humidityState = new State(humidity, "Could not retrieve a recent humidity reading", idealHumidityConditions, State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.PERCENT);
            particulatesState = new State(humidity, "Could not retrieve recent particulates reading", idealParticulatesConditions, State.Condition.UNKNOWN, dataTimestampUTC, State.Unit.AQI);
            final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState);
            return roomState;
        }



        // Global ideal range: 60 -- 72, less than 54 = too cold, above 75= too warm
        // Temp
        if (temperature  < 15.0) {
            temperatureState = new State(temperature, "It’s **pretty cold** in here.", idealTempConditions,State.Condition.ALERT, dataTimestampUTC, State.Unit.CELCIUS);
        } else if (temperature > 30.0) {
            temperatureState = new State(temperature, "It’s **pretty hot** in here.", idealTempConditions, State.Condition.ALERT, dataTimestampUTC, State.Unit.CELCIUS);
        } else { // temp >= 60 && temp <= 72
            temperatureState = new State(temperature, "Temperature is **just right**.", idealTempConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.CELCIUS);
        }


        // Humidity
        if (humidity  < 30.0) {
            humidityState = new State(humidity, "It’s **pretty dry** in here.", idealHumidityConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.PERCENT);
        } else if (humidity > 60.0) {
            humidityState = new State(humidity, "It’s **pretty humid** in here.", idealHumidityConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.PERCENT);
        } else { // humidity >= 30 && humidity<= 60
            humidityState = new State(humidity, "Humidity is **just right**.", idealHumidityConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.PERCENT);
        }


        // Air Quality - see http://www.sparetheair.com/aqi.cfm
        if (particulatesAQI <= 50.0) {
            particulatesState = new State(particulatesAQI, "Particulates level is **just right**.", idealParticulatesConditions, State.Condition.IDEAL, dataTimestampUTC, State.Unit.AQI);
        } else if (particulatesAQI <= 300.0) {
            particulatesState = new State(particulatesAQI, "AQI is **moderately high**.", idealParticulatesConditions, State.Condition.WARNING, dataTimestampUTC, State.Unit.AQI);
        } else {
            particulatesState = new State(particulatesAQI, "AQI is at an **UNHEALTHY** level.", idealParticulatesConditions, State.Condition.ALERT, dataTimestampUTC, State.Unit.AQI);
        }

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState);
        return roomState;

    }

    /**
     * Converts BatchSensorData to a CurrentRoomState object
     * @param data
     * @return
     */
    public static CurrentRoomState fromDeviceData(final DeviceData data, final DateTime referenceTime, final Integer thresholdInMinutes) {

        final float temp = DeviceData.dbIntToFloat(data.ambientTemperature);
        final float humidity = DeviceData.dbIntToFloat(data.ambientHumidity);
        // max value is in raw counts, conversion needed
        final float particulatesAQI = Float.valueOf(DataUtils.convertRawDustCountsToAQI(data.ambientDustMax, data.firmwareVersion));
        return fromTempHumidDust(temp, humidity, particulatesAQI, data.dateTimeUTC, referenceTime, thresholdInMinutes);

    }


    /**
     * To be used when data isn’t found for the currently logged in user
     * Possibly happening during sign up if workers haven’t processed the first data upload yet
     *
     * @return
     */
    public static CurrentRoomState empty() {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(null, "Waiting for data.", "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.CELCIUS),
                new State(null, "Waiting for data.", "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.PERCENT),
                new State(null, "Waiting for data.", "", State.Condition.UNKNOWN, DateTime.now(), State.Unit.AQI)
        );

        return roomState;
    }
}
