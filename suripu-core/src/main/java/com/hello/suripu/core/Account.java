package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.TimeZoneSerializer;

import java.util.TimeZone;

public class Account {

    @JsonProperty("email")
    public final String email;

    @JsonIgnore
    public final Long id;

    @JsonProperty("tz")
    @JsonSerialize(using = TimeZoneSerializer.class)
    public final TimeZone timeZone;

    public Account(final String email, final Long id, TimeZone timeZone) {
        this.email = email;
        this.id = id;
        this.timeZone = timeZone;
    }
}
