package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AccessToken {

    @JsonProperty("token")
    public final String token;

    public AccessToken(final String token) {
        this.token = token;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AccessToken that = (AccessToken) o;

        return Objects.equal(this.token, that.token);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(AccessToken.class)
                .add("token", token)
                .toString();
    }
}
