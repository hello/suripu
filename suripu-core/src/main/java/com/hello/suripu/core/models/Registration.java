package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mindrot.jbcrypt.BCrypt;

import static com.google.common.base.Preconditions.checkNotNull;

public class Registration {

    public final String name;
    public final String email;
    public final String password;
    public final Gender gender;
    public final Integer height;
    public final Integer weight;
    public final Integer tzOffsetMillis;
    public final Integer age;

    @JsonIgnore
    public  DateTime created;

    /**
     * Registration object
     * @param email
     * @param password
     * @param gender
     * @param height
     * @param weight
     * @param age
     * @param tzOffsetMillis
     */
    @JsonCreator
    public Registration(
            @JsonProperty("name") final String name,
            @JsonProperty("email") final String email,
            @JsonProperty("password") final String password,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("height") final Integer height,
            @JsonProperty("weight") final Integer weight,
            @JsonProperty("age") final Integer age,
            @JsonProperty("tz") final Integer tzOffsetMillis
    ) {
        checkNotNull(email, "email cannot be null");
        checkNotNull(email, "password cannot be null");
        this.name = name;
        this.email = email;
        this.password = password;
        this.gender = (gender == null) ? Gender.OTHER : gender;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.tzOffsetMillis = tzOffsetMillis;
        this.created = DateTime.now(DateTimeZone.UTC);
    }

    /**
     * Used internally to create a new registration with encrypted password from existing registration
     * @param name
     * @param email
     * @param hashedPassword
     * @param gender
     * @param height
     * @param weight
     * @param age
     * @param tzOffsetMillis
     * @param datetime
     */
    private Registration(
            final String name,
            final String email,
            final String hashedPassword,
            final Gender gender,
            final Integer height,
            final Integer weight,
            final Integer age,
            final Integer tzOffsetMillis,
            final DateTime datetime
    ) {
        this.name = name;
        this.email = email;
        this.password = hashedPassword;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.tzOffsetMillis = tzOffsetMillis;
        this.created = datetime;
    }

    /**
     * Creates a new Registration instance with password hashed
     * @param registration
     * @return Registration
     */
    public static Registration encryptPassword(final Registration registration) {
        return new Registration(
                registration.name,
                registration.email,
                BCrypt.hashpw(registration.password, BCrypt.gensalt(12)),
                registration.gender,
                registration.height,
                registration.weight,
                registration.age,
                registration.tzOffsetMillis,
                registration.created
        );
    }
}
