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
    public final Integer age;
    public final Gender gender;
    public final Integer height;
    public final Integer weight;
    public final Integer tzOffsetMillis;
    public final Double latitude;
    public final Double longitude;
    public final DateTime DOB;

    @JsonIgnore
    public  DateTime created;

    /**
     * Registration object
     * @param name
     * @param email
     * @param password
     * @param age
     * @param gender
     * @param height
     * @param weight
     * @param DOB
     * @param tzOffsetMillis
     * @param latitude
     * @param longitude
     */
    @JsonCreator
    public Registration(
            @JsonProperty("name") final String name,
            @JsonProperty("email") final String email,
            @JsonProperty("password") final String password,
            @JsonProperty("age") final Integer age,
            @JsonProperty("gender") final Gender gender,
            @JsonProperty("height") final Integer height,
            @JsonProperty("weight") final Integer weight,
            @JsonProperty("dob") final DateTime DOB,
            @JsonProperty("tz") final Integer tzOffsetMillis,
            @JsonProperty("lat") final Double latitude,
            @JsonProperty("lon") final Double longitude
    ) {
        checkNotNull(email, "email cannot be null");
        checkNotNull(password, "password cannot be null");
        checkNotNull(name, "name cannot be null");
        this.name = name;
        this.email = email;
        this.password = password;
        this.age = age;
        this.gender = (gender == null) ? Gender.OTHER : gender;
        this.height = height;
        this.weight = weight;
        this.DOB = DOB;
        this.tzOffsetMillis = tzOffsetMillis;
        this.created = DateTime.now(DateTimeZone.UTC);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Used internally to create a new registration with encrypted password from existing registration
     * @param name
     * @param email
     * @param hashedPassword
     * @param age
     * @param gender
     * @param height
     * @param weight
     * @param DOB
     * @param tzOffsetMillis
     * @param datetime
     * @param latitude
     * @param longitude
     */
    private Registration(
            final String name,
            final String email,
            final String hashedPassword,
            final Integer age,
            final Gender gender,
            final Integer height,
            final Integer weight,
            final DateTime DOB,
            final Integer tzOffsetMillis,
            final DateTime datetime,
            final Double latitude,
            final Double longitude
    ) {
        this.name = name;
        this.email = email;
        this.password = hashedPassword;
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.DOB = DOB;
        this.tzOffsetMillis = tzOffsetMillis;
        this.created = datetime;
        this.latitude = latitude;
        this.longitude = longitude;
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
                registration.age,
                registration.gender,
                registration.height,
                registration.weight,
                registration.DOB,
                registration.tzOffsetMillis,
                registration.created,
                registration.latitude,
                registration.longitude
        );
    }
}
