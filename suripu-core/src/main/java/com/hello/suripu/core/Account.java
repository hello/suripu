package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.TimeZoneSerializer;
import com.google.common.base.Objects;

import java.util.TimeZone;

public class Account {

    @JsonIgnore
    public final Long id;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("tz")
    @JsonSerialize(using = TimeZoneSerializer.class)
    public final TimeZone timeZone;

    @JsonIgnore
    public final String password;

    public Account(final Long id,final String email,  final String password, final TimeZone timeZone) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.timeZone = timeZone;
    }

    public static Account fromRegistration(final Registration registration, final Long id) {
        return new Account(id, registration.email, registration.password, registration.timeZone);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Account.class)
                .add("id", id)
                .add("email", email)
                .add("password", password)
                .add("timezone", timeZone.getDisplayName())
                .toString();
    }
}
