package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class AccessToken {

    public static final Long DEFAULT_INTERNAL_ID = 0L;

    @JsonIgnore
    public final UUID token;

    @JsonIgnore
    public final UUID refreshToken;

    @JsonProperty("token_type")
    public final String tokenType = "Bearer";

    @JsonProperty("expires_in")
    public final Long expiresIn;

    @JsonProperty("refresh_expires_in")
    public final Long refreshExpiresIn;

    @JsonIgnore
    public final DateTime createdAt;

    @JsonIgnore
    public final Optional<UUID> externalId;

    @JsonIgnore
    public final Long accountId;

    @JsonProperty("account_id")
    public String externalId() {
        final String internalId = String.valueOf(DEFAULT_INTERNAL_ID);
        return externalId.isPresent() ? externalId.get().toString() : internalId;

    }

    @JsonIgnore
    public final Long appId;

    @JsonIgnore
    public final OAuthScope[] scopes;

    @JsonProperty("access_token")
    public String serializeAccessToken() {
        return String.format("%d.%s", appId, token.toString().replace("-", ""));
    }

    @JsonProperty("refresh_token")
    public String serializeRefreshToken() {
        return String.format("%d.%s", appId, refreshToken.toString().replace("-", ""));
    }

    public AccessToken(
            final UUID token,
            final UUID refreshToken,
            final Long expiresIn,
            final Long refreshExpiresIn,
            final DateTime createdAt,
            final Long accountId,
            final Long appId,
            final OAuthScope[] scopes) {

        checkNotNull(token, "token can not be null");
        checkNotNull(refreshToken, "refreshToken can not be null");
        checkNotNull(expiresIn, "expiresIn can not be null");
        checkNotNull(refreshExpiresIn, "refreshExpiresIn can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(accountId, "accountId can not be null");
        checkNotNull(appId, "appId can not be null");
        checkNotNull(scopes, "scopes can not be null");

        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.createdAt = createdAt;
        this.accountId = accountId;
        this.appId = appId;
        this.scopes = scopes;
        this.externalId = Optional.absent();
    }

    private AccessToken(
            final UUID token,
            final UUID refreshToken,
            final Long expiresIn,
            final Long refreshExpiresIn,
            final DateTime createdAt,
            final Long accountId,
            final Long appId,
            final OAuthScope[] scopes,
            final Optional<UUID> externalId) {

        checkNotNull(token, "token can not be null");
        checkNotNull(refreshToken, "refreshToken can not be null");
        checkNotNull(expiresIn, "expiresIn can not be null");
        checkNotNull(refreshExpiresIn, "refreshExpiresIn can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(accountId, "accountId can not be null");
        checkNotNull(appId, "appId can not be null");
        checkNotNull(scopes, "scopes can not be null");

        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.createdAt = createdAt;
        this.accountId = accountId;
        this.appId = appId;
        this.scopes = scopes;
        this.externalId = externalId;
    }

    public static AccessToken createWithExternalId(final AccessToken accessToken, final UUID externalId) {
        return new AccessToken(accessToken.token, accessToken.refreshToken,
                accessToken.expiresIn, accessToken.refreshExpiresIn, accessToken.createdAt,
                accessToken.accountId, accessToken.appId, accessToken.scopes,
                Optional.of(externalId));
    }

    public static class Builder {
        private UUID token;
        private UUID refreshToken;
        private Long expiresIn;
        private Long refreshExpiresIn;
        private DateTime createdAt;
        private Long accountId;
        private Long appId;
        private OAuthScope[] scopes;
        private Optional<UUID> externalId;

        public Builder() {
            createdAt = DateTime.now(DateTimeZone.UTC);
            externalId = Optional.absent();
        }

        public Builder withToken(final UUID token) {
            this.token = token;
            return this;
        }

        public Builder withRefreshToken(final UUID refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder withExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
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

        public Builder withExternalId(final UUID externalId) {
            this.externalId = Optional.of(externalId);
            return this;
        }

        public AccessToken build() {
            return new AccessToken(token, refreshToken, expiresIn, refreshExpiresIn, createdAt, accountId, appId, scopes, externalId);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(token, refreshToken, tokenType, expiresIn, refreshExpiresIn, accountId, appId, Arrays.hashCode(scopes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AccessToken that = (AccessToken) o;

        return Objects.equal(this.token, that.token)
                && Objects.equal(this.refreshToken, that.refreshToken)
                && Objects.equal(this.tokenType, that.tokenType)
                && Objects.equal(this.expiresIn, that.expiresIn)
                && Objects.equal(this.refreshExpiresIn, that.refreshExpiresIn)
                && Objects.equal(this.createdAt, that.createdAt)
                && Objects.equal(this.accountId, that.accountId)
                && Objects.equal(this.appId, that.appId)
                && Arrays.equals(this.scopes, that.scopes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(AccessToken.class)
                .add("token", token)
                .add("refresh", refreshToken)
                .add("token_type", tokenType)
                .add("expires_in", expiresIn)
                .add("refresh_expires_in", refreshExpiresIn)
                .add("created_at", createdAt)
                .add("account_id", accountId)
                .add("app_id", appId)
                .add("scopes", Joiner.on(',').join(scopes))
                .add("$access_token", serializeAccessToken())
                .add("$refresh_token", serializeRefreshToken())
                .toString();
    }


    public String getName() {
        return token.toString();
    }

    public Boolean hasScope(final OAuthScope scope) {
        for(final OAuthScope s: scopes) {
            if(s.equals(scope)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}