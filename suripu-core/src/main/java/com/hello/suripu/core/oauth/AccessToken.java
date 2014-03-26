package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class AccessToken {

    @JsonProperty("access_token")
    public final String token;

    @JsonProperty("token_type")
    public final String tokenType = "Bearer";

    @JsonProperty("issued_at")
    public final Long issuedAt;

    public AccessToken(final String token, final Long issuedAt) {
        this.token = token;
        this.issuedAt = issuedAt;
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
                .add("token_type", token)
                .toString();
    }
}
