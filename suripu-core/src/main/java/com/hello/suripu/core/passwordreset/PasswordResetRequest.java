package com.hello.suripu.core.passwordreset;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordResetRequest {
    public final String email;

    @JsonCreator
    public PasswordResetRequest(@JsonProperty("email") final String email) {
        this.email = email;
    }
}
