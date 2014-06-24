package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class CurrentRoomState {

    public static class State {

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
        public final int value;

        @JsonProperty("message")
        public final String message;

        @JsonProperty("condition")
        public final Condition condition;

        @JsonProperty("last_updated_utc")
        public final DateTime lastUpdated;

        public State(final int value, final String message, final Condition condition, final DateTime lastUpdated) {
            this.value = value;
            this.message = message;
            this.condition = condition;
            this.lastUpdated = lastUpdated;
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
    public static CurrentRoomState fromBatchSensorData(final BatchSensorData data) {
        final int temp = data.getLastTemperature() / 1000;
        final int humidity = data.getLastHumidity() / 1000;
        final int particulates = data.getLastAirQuality() / 1000;
        State temperatureState;
        State humidityState;
        State particulatesState;

        // Global ideal range: 60 -- 72, less than 54 = too cold, above 75= too warm

        // Temp
        if (temp >= 54 && temp < 60 || temp > 72 && temp <= 75) {
            temperatureState = new State(temp, "Global ideal range: 60 -- 72", State.Condition.WARNING, data.dateTime);
        } else if (temp  < 54) {
            temperatureState = new State(temp, "It’s pretty cold in here.", State.Condition.ALERT, data.dateTime);
        } else if (temp > 75) {
            temperatureState = new State(temp, "It’s pretty hot in here.", State.Condition.ALERT, data.dateTime);
        } else { // temp >= 60 && temp <= 72
            temperatureState = new State(temp, "", State.Condition.IDEAL, data.dateTime);
        }

        // Humidity
        if (humidity  < 30) {
            humidityState = new State(humidity, "It’s pretty dry in here.", State.Condition.WARNING, data.dateTime);
        } else if (humidity > 60) {
            humidityState = new State(humidity, "It’s pretty humid in here.", State.Condition.WARNING, data.dateTime);
        } else { // humidity >= 30 && humidity<= 60
            humidityState = new State(humidity, "", State.Condition.IDEAL, data.dateTime);
        }


        // Air Quality
        if (particulates > 35) {
            particulatesState = new State(particulates, "Air Particulates EPA standard: Daily: 35 µg/m3, AQI = 99", State.Condition.WARNING, data.dateTime);
        } else{
            particulatesState = new State(particulates, "", State.Condition.IDEAL, data.dateTime);
        }

        final CurrentRoomState roomState = new CurrentRoomState(temperatureState, humidityState, particulatesState);
        return roomState;
    }
}
