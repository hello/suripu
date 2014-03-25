package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.TimeZoneSerializer;

import java.util.TimeZone;

public class Account {

    @JsonIgnore
    public final Long id;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("tz")
    @JsonSerialize(using = TimeZoneSerializer.class)
    public final TimeZone timeZone;

    public Account(final Long id,final String email,  TimeZone timeZone) {
        this.id = id;
        this.email = email;
        this.timeZone = timeZone;
    }

    public static Account fromRegistration(final Registration registration, final Long id) {
        return new Account(id, registration.email, registration.timeZone);
    }
}
