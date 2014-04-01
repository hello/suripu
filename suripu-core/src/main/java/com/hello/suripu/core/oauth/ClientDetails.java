package com.hello.suripu.core.oauth;

import com.google.common.base.Objects;

public class ClientDetails{

    public final GrantTypeParam.GrantType responseType;
    public final String clientId;
    public final String redirectUri;
    public final OAuthScope[] scopes;
    public final String state;
    public final String code;
    public final Long accountId;
    public final String secret;

    public ClientDetails(
            final GrantTypeParam.GrantType responseType,
            final String clientId,
            final String redirectUri,
            final OAuthScope[] scopes,
            final String state,
            final String code,
            final Long accountId,
            final String secret) {
        this.responseType = responseType;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.state = state;
        this.code = code;
        this.accountId = accountId;
        this.secret = secret;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(responseType, clientId, redirectUri, scopes, state, code, accountId, secret);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ClientDetails that = (ClientDetails) o;

        return Objects.equal(this.responseType, that.responseType) &&
                Objects.equal(this.clientId, that.clientId) &&
                Objects.equal(this.redirectUri, that.redirectUri) &&
                Objects.equal(this.scopes, that.scopes) &&
                Objects.equal(this.state, that.state) &&
                Objects.equal(this.code, that.code) &&
                Objects.equal(this.accountId, that.accountId) &&
                Objects.equal(this.secret, that.secret);
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(ClientDetails.class)
                .add("responseType", responseType)
                .add("clientId", clientId)
                .add("redirectUri", redirectUri)
                .add("scopes", scopes)
                .add("state", state)
                .add("code", code)
                .add("accountId", accountId)
                .add("secret", secret)
                .toString();
    }
}
