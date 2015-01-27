package com.hello.suripu.core.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountPreference {

    public enum TemperatureUnit {
        CELSIUS("C"),
        FAHRENHEIT("F");
        private String value;
        private TemperatureUnit(final String value) { this.value = value;}

        @Override
        public String toString() {
            if (this == CELSIUS) {
                return "C";
            }
            return "F";
        }
    }

    public enum EnabledPreference {
        ENHANCED_AUDIO("enhanced_audio"),
        TEMP_CELSIUS("temp_celsius"),
        TIME_TWENTY_FOUR_HOUR("time_twenty_four_hour");

        private String value;
        private EnabledPreference(final String value) {
            this.value = value;
        }

        @JsonCreator
        public static EnabledPreference fromString(final String value) {
            for(EnabledPreference pref: EnabledPreference.values()) {
                if(pref.value.equalsIgnoreCase(value)) {
                    return pref;
                }
            }

            throw new IllegalArgumentException("Invalid preference name");
        }
    }

    @JsonProperty("pref")
    public final EnabledPreference key;

    @JsonProperty("enabled")
    public final Boolean enabled;

    @JsonCreator
    public AccountPreference(
            @JsonProperty("pref") final EnabledPreference key,
            @JsonProperty("enabled") final Boolean enabled) {
        this.key = key; // EnabledPreference.fromString(key);
        this.enabled =  enabled;
    }
}
