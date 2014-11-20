package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentRoomState {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentRoomState.class);

    public static class State {

        public enum Unit {
            CELCIUS("c"),
            PERCENT("%"),
            PPM("ppm"),
            MICRO_G_M3("µg/m3");

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

        @JsonProperty("condition")
        public final Condition condition;

        @JsonProperty("last_updated_utc")
        public final DateTime lastUpdated;

        @JsonProperty("unit")
        public final Unit unit;

        public State(final Float value, final String message, final Condition condition, final DateTime lastUpdated, final Unit unit) {
            this.value = value;
            this.message = message;
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

    /**
     * Converts BatchSensorData to a CurrentRoomState object
     * @param data
     * @return
     */
    public static CurrentRoomState fromDeviceData(final DeviceData data, final DateTime referenceTime, final Integer thresholdInMinutes) {

        // BEWARE, MUTABLE STATE BELOW. SCAAAAAARY
        Float temp = DeviceData.dbIntToFloat(data.ambientTemperature);
        Float humidity = DeviceData.dbIntToFloat(data.ambientHumidity);
        final int dustDensity = DeviceData.convertDustAnalogToMicroGM3(data.ambientDustMax, 0); // max values are raw counts
        Float particulates = DeviceData.dbIntToFloatDust(dustDensity);
        State temperatureState;
        State humidityState;
        State particulatesState;

        LOGGER.debug("temp = {}, humidity = {}, particulates = {}", temp, humidity, particulates);

        if(referenceTime.minusMinutes(thresholdInMinutes).getMillis() > data.dateTimeUTC.getMillis()) {

            LOGGER.warn("{} is too old, not returning anything", data.dateTimeUTC);
            temp = null;
            humidity = null;
            particulates = null;
        }

        // Global ideal range: 60 -- 72, less than 54 = too cold, above 75= too warm

        // Temp
        if(temp == null) {
           temperatureState = new State(temp, "Could not retrieve a recent temperature reading", State.Condition.UNKNOWN, data.dateTimeUTC, State.Unit.CELCIUS);
        } else if (temp  < 15.0) {
            temperatureState = new State(temp, "It’s *pretty cold* in here.", State.Condition.ALERT, data.dateTimeUTC, State.Unit.CELCIUS);
        } else if (temp > 30.0) {
            temperatureState = new State(temp, "It’s *pretty hot* in here.", State.Condition.ALERT, data.dateTimeUTC, State.Unit.CELCIUS);
        } else { // temp >= 60 && temp <= 72
            temperatureState = new State(temp, "", State.Condition.IDEAL, data.dateTimeUTC, State.Unit.CELCIUS);
        }

        // Humidity
        if(humidity == null) {
            humidityState = new State(humidity, "Could not retrieve a recent humidity reading", State.Condition.UNKNOWN, data.dateTimeUTC, State.Unit.PERCENT);
        } else if (humidity  < 30.0) {
            humidityState = new State(humidity, "It’s *pretty dry* in here.", State.Condition.WARNING, data.dateTimeUTC, State.Unit.PERCENT);
        } else if (humidity > 60.0) {
            humidityState = new State(humidity, "It’s *pretty humid* in here.", State.Condition.WARNING, data.dateTimeUTC, State.Unit.PERCENT);
        } else { // humidity >= 30 && humidity<= 60
            humidityState = new State(humidity, "", State.Condition.IDEAL, data.dateTimeUTC, State.Unit.PERCENT);
        }


        // Air Quality
        if(particulates == null) {
            particulatesState = new State(humidity, "Could not retrieve a recent particulates reading", State.Condition.UNKNOWN, data.dateTimeUTC, State.Unit.MICRO_G_M3);
        } else if (particulates > 0.025) { // 35.0 divide by 1440 minutes
            particulatesState = new State(particulates, "Air particulates level is *higher than usual*, do not let this condition persists for more than 24 hours", State.Condition.WARNING, data.dateTimeUTC, State.Unit.MICRO_G_M3);
        } else if (particulates > 35.0) {
            particulatesState = new State(particulates, "Air Particulates EPA standard: Daily: 35 µg/m3, AQI = 99", State.Condition.ALERT, data.dateTimeUTC, State.Unit.MICRO_G_M3);
        } else{
            particulatesState = new State(particulates, "", State.Condition.IDEAL, data.dateTimeUTC, State.Unit.MICRO_G_M3);
        }

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState);
        return roomState;
    }


    /**
     * To be used when data isn’t found for the currently logged in user
     * Possibly happening during sign up if workers haven’t processed the first data upload yet
     *
     * @return
     */
    public static CurrentRoomState empty() {
        final CurrentRoomState roomState = new CurrentRoomState(
                new State(null, "Waiting for data.", State.Condition.UNKNOWN, DateTime.now(), State.Unit.CELCIUS),
                new State(null, "Waiting for data.", State.Condition.UNKNOWN, DateTime.now(), State.Unit.PERCENT),
                new State(null, "Waiting for data.", State.Condition.UNKNOWN, DateTime.now(), State.Unit.MICRO_G_M3)
        );

        return roomState;
    }
}
