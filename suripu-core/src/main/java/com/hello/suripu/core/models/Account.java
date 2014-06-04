package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.TimeZoneSerializer;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = Account.Builder.class)
public class Account {

    public static class MyAccountCreationException extends RuntimeException {
        public MyAccountCreationException(String message) {
            super(message);
        }
    }
    // TODO: add age or DoB

    @NotNull
    @JsonIgnore
    public final Long id;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("tz")
    @JsonSerialize(using = TimeZoneSerializer.class)
    public final TimeZone timeZone;

    @JsonProperty("firstname")
    public final String firstname;

    @JsonProperty("lastname")
    public final String lastname;

    @JsonProperty("gender")
    public final Gender gender;

    @JsonProperty("height")
    public final Integer height;

    @JsonProperty("weight")
    public final Integer weight;

    @JsonIgnore
    public final String password;

    /**
     *
     * @param id
     * @param email
     * @param password
     * @param timeZone
     * @param firstname
     * @param lastname
     * @param gender
     * @param height
     * @param weight
     */
    private Account(final Long id,
                   final String email,
                   final String password,
                   final TimeZone timeZone,
                   final String firstname,
                   final String lastname,
                   final Gender gender,
                   final Integer height,
                   final Integer weight) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.timeZone = timeZone;

        this.firstname = firstname;
        this.lastname = lastname;
        this.gender = gender;
        this.height = height;
        this.weight = weight;

    }

    /**
     * Transform registration to account
     * @param registration
     * @param id
     * @return
     */
    public static Account fromRegistration(final Registration registration, final Long id) {
        return new Account(id, registration.email, registration.password, registration.timeZone, registration.firstname,
                registration.lastname, registration.gender, registration.height, registration.weight);
    }


    public static class Builder {
        private Long id;
        private String firstname;
        private String lastname;
        private Gender gender;
        private Integer height;
        private Integer weight;
        private String password;
        private String email;
        private TimeZone tz;

        public Builder() {
            this.firstname = "";
            this.lastname = "";
            this.gender = Gender.OTHER;
            this.height = 0;
            this.weight = 0;
            this.password = "";
            this.email = "";
        }

        @JsonProperty("firstname")
        public Builder withFirstname(final String firstname) {
            this.firstname = firstname;
            return this;
        }

        @JsonProperty("lastname")
        public Builder withLastname(final String lastname) {
            this.lastname = lastname;
            return this;
        }

        @JsonProperty("gender")
        public Builder withGender(final String gender) {
            this.gender = Gender.valueOf(gender);
            return this;
        }

        @JsonIgnore
        public Builder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        @JsonProperty("height")
        public Builder withHeight(final Integer height) {
            this.height = height;
            return this;
        }

        @JsonProperty("weight")
        public Builder withWeight(final Integer weight) {
            this.weight = weight;
            return this;
        }

        @JsonProperty("password")
        public Builder withPassword(final String password) {
            this.password = password;
            return this;
        }

        @JsonProperty("email")
        public Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        @NotNull
        @JsonProperty("id")
        public Builder withId(final Long id) {
            this.id = id;
            return this;
        }

        @JsonIgnore
        public Builder withTimeZone(TimeZone tz) {
            this.tz = tz;
            return this;
        }

        @JsonProperty("tz")
        public Builder withTimeZone(String tz) {
            this.tz = TimeZone.getTimeZone(tz);
            return this;
        }

        public Account build() throws MyAccountCreationException {
            checkNotNull(id, "ID can not be null");
            checkNotNull(email, "Email can not be null");
            return new Account(id, email, password, tz, firstname, lastname, gender, height, weight);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Account.class)
                .add("id", id)
                .add("email", email)
                .add("password", obscurePassword(password))
                .add("timezone", timeZone.getDisplayName())
                .add("firstname", firstname)
                .add("lastname", lastname)
                .add("email", email)
                .add("height", height)
                .add("weight", weight)
                .toString();
    }

    private String obscurePassword(final String password) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < password.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }
}
