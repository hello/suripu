package com.hello.suripu.core.roomstate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.joda.time.DateTime;

public class State {

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

    @JsonProperty("value")
    public final Float value;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("ideal_conditions")
    public final String idealConditions;

    private final Condition condition;

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

    @JsonProperty("condition")
    public Condition condition() {
        return condition;
    }


    public static State ideal(final Float value, final String message, final String idealConditions, final DateTime lastUpdated, final Unit unit) {
        return new State(value, message, idealConditions, Condition.IDEAL, lastUpdated, unit);
    }
}
