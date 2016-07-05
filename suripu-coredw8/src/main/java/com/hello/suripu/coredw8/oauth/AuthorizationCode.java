package com.hello.suripu.coredw8.oauth;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.hello.suripu.core.oauth.OAuthScope;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class AuthorizationCode {

    @JsonIgnore
    public final UUID authCode;

    @JsonProperty("expires_in")
    public final Long expiresIn;

    @JsonIgnore
    public final DateTime createdAt;

    @JsonProperty("account_id")
    @JsonSerialize(using = ToStringSerializer.class)
    public final Long accountId;

    @JsonIgnore
    public final Long appId;

    @JsonIgnore
    public final OAuthScope[] scopes;

    @JsonProperty("auth_code")
    public String serializeAuthCode() {
        return String.format("%d.%s", appId, authCode.toString().replace("-", ""));
    }

    public AuthorizationCode(
            final UUID authCode,
            final Long expiresIn,
            final DateTime createdAt,
            final Long accountId,
            final Long appId,
            final OAuthScope[] scopes) {

        checkNotNull(authCode, "authCode can not be null");
        checkNotNull(expiresIn, "expiresIn can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(accountId, "accountId can not be null");
        checkNotNull(appId, "appId can not be null");
        checkNotNull(scopes, "scopes can not be null");

        this.authCode = authCode;
        this.expiresIn = expiresIn;
        this.createdAt = createdAt;
        this.accountId = accountId;
        this.appId = appId;
        this.scopes = scopes;
    }


    public static class Builder {
        private UUID authCode;
        private Long expiresIn;
        private DateTime createdAt;
        private Long accountId;
        private Long appId;
        private OAuthScope[] scopes;

        public Builder() {
            createdAt = DateTime.now(DateTimeZone.UTC);
        }

        public Builder withAuthCode(final UUID authCode) {
            this.authCode = authCode;
            return this;
        }

        public Builder withExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder withCreatedAt(final DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withAppId(final Long appId) {
            this.appId = appId;
            return this;
        }

        public Builder withScopes(final OAuthScope[] scopes) {
            this.scopes = scopes;
            return this;
        }

        public AuthorizationCode build() {
            return new AuthorizationCode(authCode, expiresIn, createdAt, accountId, appId, scopes);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(authCode, expiresIn, accountId, appId, Arrays.hashCode(scopes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AuthorizationCode that = (AuthorizationCode) o;

        return Objects.equal(this.authCode, that.authCode)
                && Objects.equal(this.expiresIn, that.expiresIn)
                && Objects.equal(this.createdAt, that.createdAt)
                && Objects.equal(this.accountId, that.accountId)
                && Objects.equal(this.appId, that.appId)
                && Arrays.equals(this.scopes, that.scopes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(AuthorizationCode.class)
                .add("authCode", authCode)
                .add("expires_in", expiresIn)
                .add("created_at", createdAt)
                .add("account_id", accountId)
                .add("app_id", appId)
                .add("scopes", Joiner.on(',').join(scopes))
                .add("$auth_code", serializeAuthCode())
                .toString();
    }

    public Boolean hasScope(final OAuthScope scope) {
        for(final OAuthScope s: scopes) {
            if(s.equals(scope)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public Boolean hasExpired(DateTime now) {
        long diffInSeconds= (now.getMillis() - this.createdAt.getMillis()) / 1000;
        return (diffInSeconds > this.expiresIn);
    }
}