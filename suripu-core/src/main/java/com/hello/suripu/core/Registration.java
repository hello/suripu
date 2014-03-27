package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mindrot.jbcrypt.BCrypt;

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

    @JsonIgnore
    public  DateTime created;

    /**
     * Registration object
     * @param firstname
     * @param lastname
     * @param email
     * @param password
     * @param gender
     * @param height
     * @param weight
     * @param age
     * @param timeZone
     */
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
        this.created = DateTime.now(DateTimeZone.UTC);
    }

    /**
     * Used internally to create a new registration with encrypted password from existing registration
     * @param firstname
     * @param lastname
     * @param email
     * @param hashedPassword
     * @param gender
     * @param height
     * @param weight
     * @param age
     * @param timeZone
     * @param datetime
     */
    private Registration(
            final String firstname,
            final String lastname,
            final String email,
            final String hashedPassword,
            final Gender gender,
            final float height,
            final float weight,
            final Integer age,
            final String timeZone,
            final DateTime datetime
    ) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = hashedPassword;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.timeZone = TimeZone.getTimeZone(timeZone);
        this.created = datetime;
    }

    /**
     * Creates a new Registration instance with password hashed
     * @param registration
     * @return Registration
     */
    public static Registration encryptPassword(final Registration registration) {
        return new Registration(
                registration.firstname,
                registration.lastname,
                registration.email,
                BCrypt.hashpw(registration.password, BCrypt.gensalt(12)),
                registration.gender,
                registration.height,
                registration.weight,
                registration.age,
                registration.timeZone.getID(),
                registration.created
        );
    }
}
