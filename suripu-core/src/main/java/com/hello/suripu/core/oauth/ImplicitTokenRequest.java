package com.hello.suripu.core.oauth;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImplicitTokenRequest {
    public final String email;
    public final String clientId;

    @JsonCreator
    public ImplicitTokenRequest(@JsonProperty("email") final String email,
                                @JsonProperty("client_id") final String clientId) {

        this.email = email;
        this.clientId = clientId;
    }
}
