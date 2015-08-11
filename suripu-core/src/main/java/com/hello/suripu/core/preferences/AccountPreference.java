package com.hello.suripu.core.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountPreference {
    @JsonProperty("pref")
    public final PreferenceName key;

    @JsonProperty("enabled")
    public final Boolean enabled;

    @JsonCreator
    public AccountPreference(@JsonProperty("pref") final PreferenceName key,
                             @JsonProperty("enabled") final Boolean enabled) {
        this.key = key;
        this.enabled =  enabled;
    }
}
