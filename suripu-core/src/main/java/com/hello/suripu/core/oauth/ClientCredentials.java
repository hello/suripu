package com.hello.suripu.core.oauth;


import com.google.common.base.Objects;

import java.util.Arrays;

public class ClientCredentials {

    public final OAuthScope[] scopes;
    public final String tokenOrCode;

    public ClientCredentials(final OAuthScope[] scopes, final String tokenOrCode) {
        this.scopes = scopes;
        this.tokenOrCode = tokenOrCode;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(ClientDetails.class)
                .add("scope", scopes)
                .add("tokenOrCode", tokenOrCode)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(scopes), tokenOrCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ClientCredentials that = (ClientCredentials) o;

        return Arrays.equals(this.scopes, that.scopes) &&
                Objects.equal(this.tokenOrCode, that.tokenOrCode);
    }
}
