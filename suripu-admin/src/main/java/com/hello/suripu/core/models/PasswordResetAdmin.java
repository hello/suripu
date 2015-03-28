package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zet on 3/27/15.
 */
public class PasswordResetAdmin {
    public final String email;
    public final String password;

    @JsonCreator
    public PasswordResetAdmin(@JsonProperty("email") final String email, @JsonProperty("password") final String password) {
        this.email = email;
        this.password = password;
    }
}
