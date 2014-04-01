package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class AccessToken {

    @JsonIgnore
    public final UUID token;

    @JsonIgnore
    public final UUID refreshToken;

    @JsonProperty("token_type")
    public final String tokenType = "Bearer";

    @JsonProperty("expires_in")
    public final Long expiresIn;

    @JsonIgnore
    public final DateTime createdAt;

    @JsonIgnore
    public final Long accountId;

    @JsonIgnore
    public final Long appId;

    @JsonIgnore
    public final OAuthScope[] scopes;


    @JsonProperty("access_token")
    public String serializeAccessToken() {
        return token.toString().replace("-", "");
    }

    @JsonProperty("refresh_token")
    public String serializeRefreshToken() {
        return refreshToken.toString().replace("-", "");
    }

    public AccessToken(
            final UUID token,
            final UUID refreshToken,
            final Long expiresIn,
            final DateTime createdAt,
            final Long accountId,
            final Long appId,
            final OAuthScope[] scopes) {

        checkNotNull(token, "token can not be null");
        checkNotNull(refreshToken, "refreshToken can not be null");
        checkNotNull(expiresIn, "expiresIn can not be null");
        checkNotNull(createdAt, "createdAt can not be null");
        checkNotNull(accountId, "accountId can not be null");
        checkNotNull(appId, "appId can not be null");
        checkNotNull(scopes, "scopes can not be null");

        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.createdAt = createdAt;
        this.accountId = accountId;
        this.appId = appId;
        this.scopes = scopes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(token, refreshToken, tokenType, expiresIn, accountId, appId, scopes);
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
                && Objects.equal(this.createdAt, that.createdAt)
                && Objects.equal(this.accountId, that.accountId)
                && Objects.equal(this.appId, that.appId)
                && Objects.equal(this.scopes, that.scopes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(AccessToken.class)
                .add("token", token)
                .add("refresh", refreshToken)
                .add("token_type", tokenType)
                .add("expires_in", expiresIn)
                .add("created_at", createdAt)
                .add("account_id", accountId)
                .add("app_id", appId)
                .add("scopes", Joiner.on(',').join(scopes))
                .add("$access_token", serializeAccessToken())
                .add("$refresh_token", serializeRefreshToken())
                .toString();
    }
}