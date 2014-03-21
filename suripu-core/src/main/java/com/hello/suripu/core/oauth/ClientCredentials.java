package com.hello.suripu.core.oauth;


import com.google.common.base.Objects;

public class ClientCredentials {

    public final OAuthScope[] scopes;
    public final String token;

    public ClientCredentials(final OAuthScope[] scopes, final String token) {
        this.scopes = scopes;
        this.token = token;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ClientDetails.class)
                .add("scope", scopes)
                .add("token", token)
                .toString();
    }
}
