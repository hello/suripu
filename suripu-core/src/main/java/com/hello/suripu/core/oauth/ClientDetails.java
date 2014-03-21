package com.hello.suripu.core.oauth;

import com.google.common.base.Objects;

public class ClientDetails{

    public final String responseType;
    public final String clientId;
    public final String redirectUri;
    public final OAuthScope[] scopes;
    public final String state;
    public final String code;
    public final Long accountId;
    public final String secret;

    public ClientDetails(String responseType, String clientId, String redirectUri, OAuthScope[] scopes, String state, String code, Long accountId, String secret) {
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
