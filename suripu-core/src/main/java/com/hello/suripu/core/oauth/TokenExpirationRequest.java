package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class TokenExpirationRequest {
    public final String dirtyToken;

    @JsonCreator
    public TokenExpirationRequest(@JsonProperty("dirty_token") final String dirtyToken) {
        this.dirtyToken = dirtyToken;
    }
}
