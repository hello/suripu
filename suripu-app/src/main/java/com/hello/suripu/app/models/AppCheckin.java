package com.hello.suripu.app.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppCheckin {

    public enum Platform {
        ANDROID("android"),
        IOS("ios");

        private String value;
        private Platform(final String value) {
            this.value = value;
        }

        @JsonCreator
        public static Platform create(final String val) {
            for (final Platform platform : Platform.values()) {
                if (platform.value.equals(val.toLowerCase())) {
                    return platform;
                }
            }
            throw new IllegalArgumentException(String.format("%s is not a valid Platform value", val));
        }
    }

    public final Platform platform;
    public final String appVersion;
    public final String langCode;

    @JsonCreator
    public AppCheckin(
            @JsonProperty("platform") Platform platform,
            @JsonProperty("app_version") String appVersion,
            @JsonProperty("lang_code") String langCode
            ) {
        this.platform = platform;
        this.appVersion = appVersion;
        this.langCode = langCode;
    }
}
