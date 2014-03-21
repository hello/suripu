package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Account {

    @JsonProperty("email")
    public final String email;

    public Account(final String email) {
        this.email = email;
    }
}
