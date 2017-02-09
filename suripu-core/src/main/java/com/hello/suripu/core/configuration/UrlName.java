package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by david on 2/7/17.
 */
public enum  UrlName {
    VOICE("voice");

    private String value;

    UrlName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public UrlName fromString(final String val) {
        final UrlName[] urlNames = UrlName.values();

        for (final UrlName url: urlNames) {
            if (url.value.equalsIgnoreCase(val)) {
                return url;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid Url Name", val));
    }

}
