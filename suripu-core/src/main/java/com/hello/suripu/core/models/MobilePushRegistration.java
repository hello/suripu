package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class MobilePushRegistration {

    public enum OS {
        ANDROID("android"),
        IOS("ios");

        private String value;
        private OS(String value) {
            this.value = value;
        }

        public static OS fromString(final String val) {
            for(final OS os : OS.values()) {
                if(os.value.equalsIgnoreCase(val)) {
                    return os;
                }
            }

            throw new IllegalArgumentException(String.format("No OS with value: %s", val));
        }
    }
    @JsonIgnore
    public final Optional<Long> accountId;

    @JsonProperty("os")
    public final String os;

    @JsonProperty("version")
    public final String version;

    @JsonProperty("app_version")
    public final String appVersion;

    @JsonProperty("token")
    public final String deviceToken;

    @JsonIgnore
    public final Optional<String> oauthToken;

    @JsonIgnore
    public final Optional<String> endpoint;


    /**
     * Public constructor for MobilePushRegistration
     * Only use if all info is known ahead of time
     *
     * @param accountId
     * @param os
     * @param version
     * @param appVersion
     * @param deviceToken
     * @param endpoint
     */
    public MobilePushRegistration(final Long accountId, final String os, final String version, final String appVersion, final String deviceToken, final String oauthToken, final String endpoint) {
        this.accountId = Optional.fromNullable(accountId);
        this.os = os;
        this.version = version;
        this.appVersion = appVersion;
        this.deviceToken = deviceToken;
        this.oauthToken = Optional.fromNullable(oauthToken);
        this.endpoint = Optional.fromNullable(endpoint);
    }

    @JsonCreator
    public MobilePushRegistration(
            @JsonProperty("os") final String os,
            @JsonProperty("version") final String version,
            @JsonProperty("app_version") final String appVersion,
            @JsonProperty("token") final String deviceToken) {
        this(null, os, version, appVersion, deviceToken, null, null);
    }

    /**
     *
     * @param m
     * @param endpoint
     * @param accountId
     * @return
     */
    public static MobilePushRegistration withEndpointForAccount(final MobilePushRegistration m, final String endpoint, final Long accountId) {
        final String oAuthToken = (m.oauthToken.isPresent()) ? m.oauthToken.get() : null;
        return new MobilePushRegistration(accountId, m.os, m.version, m.appVersion, m.deviceToken, oAuthToken, endpoint);
    }

    /**
     * Creates a MobilePushRegistration instance with a given oauth token
     * @param m
     * @param oauthToken
     * @return
     */
    public static MobilePushRegistration withOauthToken(final MobilePushRegistration m, final String oauthToken) {
        return new MobilePushRegistration(null, m.os, m.version, m.appVersion, m.deviceToken, oauthToken, null);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MobilePushRegistration.class)
                .add("account_id", accountId)
                .add("os", os)
                .add("version", version)
                .add("app_version", appVersion)
                .add("endpoint", endpoint)
                .add("token", deviceToken)
                .toString();
    }

}
