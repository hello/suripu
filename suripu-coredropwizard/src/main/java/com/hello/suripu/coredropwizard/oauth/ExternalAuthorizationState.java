package com.hello.suripu.coredropwizard.oauth;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExternalAuthorizationState {

    @JsonProperty("auth_state")
    public final UUID authState;

    @JsonIgnore
    public final DateTime createdAt;

    @JsonProperty("device_id")
    final public String deviceId;

    @JsonIgnore
    public final Long appId;


    public ExternalAuthorizationState(
            final UUID authState,
            final DateTime createdAt,
            final String deviceId,
            final Long appId) {

        checkNotNull(authState, "authState can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(deviceId, "deviceId can not be null");
        checkNotNull(appId, "appId can not be null");

        this.authState = authState;
        this.createdAt = createdAt;
        this.deviceId = deviceId;
        this.appId = appId;
    }


    public static class Builder {
        private UUID authState;
        private DateTime createdAt;
        private String deviceId;
        private Long appId;

        public Builder() {
            createdAt = DateTime.now(DateTimeZone.UTC);
        }

        public Builder withAuthState(final UUID authState) {
            this.authState = authState;
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

        public ExternalAuthorizationState build() {
            return new ExternalAuthorizationState(authState, createdAt, deviceId, appId);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(authState, deviceId, appId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExternalAuthorizationState that = (ExternalAuthorizationState) o;

        return Objects.equal(this.authState, that.authState)
                && Objects.equal(this.createdAt, that.createdAt)
                && Objects.equal(this.deviceId, that.deviceId)
                && Objects.equal(this.appId, that.appId);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ExternalAuthorizationState.class)
                .add("authState", authState)
                .add("created_at", createdAt)
                .add("device_id", deviceId)
                .add("app_id", appId)
                .toString();
    }
}