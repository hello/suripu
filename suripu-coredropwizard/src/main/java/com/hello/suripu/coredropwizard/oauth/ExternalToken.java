package com.hello.suripu.coredropwizard.oauth;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExternalToken implements Principal {

    @JsonProperty("access_token")
    public final String accessToken;

    @JsonProperty("refresh_token")
    public final String refreshToken;

    @JsonProperty("token_type")
    public final String tokenType = "Bearer";

    @JsonProperty("access_expires_in")
    public final Long accessExpiresIn;

    @JsonIgnore
    @JsonProperty("refresh_expires_in")
    public final Long refreshExpiresIn;

    @JsonIgnore
    public final DateTime createdAt;

    @JsonProperty("device_id")
    public final String deviceId;

    @JsonIgnore
    public final Long appId;

    public ExternalToken(
            final String accessToken,
            final String refreshToken,
            final Long accessExpiresIn,
            final Long refreshExpiresIn,
            final DateTime createdAt,
            final String deviceId,
            final Long appId) {

        checkNotNull(accessToken, "accessToken can not be null");
        checkNotNull(accessExpiresIn, "accessExpiresIn can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(deviceId, "deviceId can not be null");
        checkNotNull(appId, "appId can not be null");

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresIn = accessExpiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.createdAt = createdAt;
        this.deviceId = deviceId;
        this.appId = appId;
    }


    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private Long accessExpiresIn;
        private Long refreshExpiresIn;
        private DateTime createdAt;
        private String deviceId;
        private Long appId;

        public Builder() {
            createdAt = DateTime.now(DateTimeZone.UTC);
            accessExpiresIn = 315569260L;
            refreshExpiresIn = 0L;
        }

        public Builder withAccessToken(final String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder withRefreshToken(final String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder withAccessExpiresIn(Long accessExpiresIn) {
            this.accessExpiresIn = accessExpiresIn;
            return this;
        }

        public Builder withRefreshExpiresIn(Long refreshExpiresIn) {
            this.refreshExpiresIn = refreshExpiresIn;
            return this;
        }

        public Builder withCreatedAt(final DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withDeviceId(final String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder withAppId(final Long appId) {
            this.appId = appId;
            return this;
        }

        public ExternalToken build() {
            return new ExternalToken(accessToken, refreshToken, accessExpiresIn, refreshExpiresIn, createdAt, deviceId, appId);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(accessToken, refreshToken, tokenType, accessExpiresIn, refreshExpiresIn, deviceId, appId);
    }

    @Override
    public String getName() {
        return accessToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExternalToken that = (ExternalToken) o;

        return Objects.equal(this.accessToken, that.accessToken)
                && Objects.equal(this.refreshToken, that.refreshToken)
                && Objects.equal(this.tokenType, that.tokenType)
                && Objects.equal(this.accessExpiresIn, that.accessExpiresIn)
                && Objects.equal(this.refreshExpiresIn, that.refreshExpiresIn)
                && Objects.equal(this.createdAt, that.createdAt)
                && Objects.equal(this.deviceId, that.deviceId)
                && Objects.equal(this.appId, that.appId);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ExternalToken.class)
                .add("access_token", accessToken)
                .add("refresh_token", refreshToken)
                .add("token_type", tokenType)
                .add("access_expires_in", accessExpiresIn)
                .add("refresh_expires_in", refreshExpiresIn)
                .add("created_at", createdAt)
                .add("device_id", deviceId)
                .add("app_id", appId)
                .toString();
    }

    public Boolean hasExpired(DateTime now) {
        long diffInSeconds= (now.getMillis() - this.createdAt.getMillis()) / 1000;
        return (diffInSeconds > this.accessExpiresIn);
    }

    public Boolean canRefresh(DateTime now) {
        if(refreshToken.isEmpty() |  refreshExpiresIn == null) {
            return false;
        }
        if(refreshExpiresIn.equals(0L)) {
            return false;
        }

        long diffInSeconds= (now.getMillis() - this.createdAt.getMillis()) / 1000;
        return (diffInSeconds < this.refreshExpiresIn);
    }
}