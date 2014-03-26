package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.TimeZone;

public class Registration {
    @JsonProperty("firstname")
    public final String firstname;

    @JsonProperty("lastname")
    public final String lastname;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("password")
    public final String password;

    @JsonProperty("gender")
    public final Gender gender;

    @JsonProperty("height")
    public final float height;

    @JsonProperty("weight")
    public final float weight;

    @JsonProperty("tz")
    public final TimeZone timeZone;

    @JsonProperty("age")
    public final Integer age;

    @JsonCreator
    public Registration(
            @JsonProperty("firstname") final String firstname,
            @JsonProperty("lastname") final String lastname,
            @JsonProperty("email") final String email,
            @JsonProperty("password") final String password,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("height") final float height,
            @JsonProperty("weight") final float weight,
            @JsonProperty("age") final Integer age,
            @JsonProperty("tz") final String timeZone
    ) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.timeZone = TimeZone.getTimeZone(timeZone);
    }
}
