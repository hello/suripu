package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = Account.Builder.class)
public class Account {

    public static class MyAccountCreationException extends RuntimeException {
        public MyAccountCreationException(String message) {
            super(message);
        }
    }

    @NotNull
    @JsonIgnore
    public final Optional<Long> id;

    @JsonProperty("id")
    public final String externalID;

    @JsonProperty("email")
    public final String email;

    @JsonProperty("tz")
    public final Integer tzOffsetMillis;

    @JsonProperty("name")
    public final String name;

    @JsonProperty("gender")
    public final Gender gender;

    @JsonProperty("height")
    public final Integer height;

    @JsonProperty("weight")
    public final Integer weight;

    @JsonIgnore
    public final String password;

    @JsonIgnore
    public final DateTime created;

    @JsonProperty("last_modified")
    public final Long lastModified;

    @JsonProperty("dob")
    public final DateTime DOB;

    /**
     *
     * @param id
     * @param externalID
     * @param email
     * @param password
     * @param tzOffsetMillis
     * @param name
     * @param gender
     * @param height
     * @param weight
     * @param created
     * @param lastModified
     * @param DOB
     */
    private Account(final Optional<Long> id,
                    final String externalID,
                    final String email,
                    final String password,
                    final Integer tzOffsetMillis,
                    final String name,
                    final Gender gender,
                    final Integer height,
                    final Integer weight,
                    final DateTime created,
                    final Long lastModified,
                    final DateTime DOB) {

        this.id = id;
        this.externalID = externalID;
        this.email = email;
        this.password = password;
        this.tzOffsetMillis = tzOffsetMillis;

        this.name = name;
        this.gender = gender;
        this.height = height;
        this.weight = weight;

        this.created = created;

        this.lastModified = lastModified;
        this.DOB = DOB;
    }

    /**
     * Transform registration to account
     * @param registration
     * @param id
     * @return
     */
    public static Account fromRegistration(final Registration registration, final Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append("|");
        sb.append(registration.created.getMillis());

        final String digest = DigestUtils.md5Hex(sb.toString());
        return new Account(Optional.fromNullable(id), digest, registration.email, registration.password, registration.tzOffsetMillis,
                registration.name, registration.gender, registration.height, registration.weight, registration.created,
                DateTime.now(DateTimeZone.UTC).getMillis(), registration.DOB);
    }


    public static class Builder {
        private Optional<Long> id;
        private String externalId;
        private String name;
        private Gender gender;
        private Integer height;
        private Integer weight;
        private String password;
        private String email;
        private Integer tzOffsetMillis;
        private DateTime created;
        private Long lastModified;
        private DateTime DOB;

        public Builder() {
            this.id = Optional.absent();
            this.externalId = "";
            this.name = "";
            this.gender = Gender.OTHER;
            this.height = 0;
            this.weight = 0;
            this.password = "";
            this.email = "";
            this.created = DateTime.now(DateTimeZone.UTC);
            this.lastModified = DateTime.now(DateTimeZone.UTC).getMillis();
            this.DOB = DateTime.now().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfDay(0);
        }

        @JsonProperty("name")
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        @JsonProperty("gender")
        public Builder withGender(final String gender) {
            if(gender != null) {
                this.gender = Gender.valueOf(gender);
            }
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
            this.id = Optional.fromNullable(id);
            return this;
        }

        @JsonProperty("tz")
        public Builder withTzOffsetMillis(final Integer tzOffsetMillis) {
            this.tzOffsetMillis = tzOffsetMillis;
            return this;
        }

        @JsonIgnore
        public Builder withCreated(final Long created) {
            this.created = new DateTime(created);
            return this;
        }

        @JsonIgnore
        public Builder withLastModified(final Long lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @JsonProperty("dob")
        public Builder withDOB(final String DOB) {
            this.DOB = DateTime.parse(DOB);
            return this;
        }

        @JsonIgnore
        public Builder withDOB(final DateTime DOB) {
            this.DOB = DOB;
            return this;
        }

        public Account build() throws MyAccountCreationException {
            checkNotNull(id, "ID can not be null");
            checkNotNull(email, "Email can not be null");
            return new Account(id, externalId, email, password, tzOffsetMillis, name, gender, height, weight, created, lastModified, DOB);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Account.class)
                .add("id", (id.isPresent()) ? id.get() : "N/A")
                .add("external_id", externalID)
                .add("email", email)
                .add("password", obscurePassword(password))
                .add("tz", tzOffsetMillis)
                .add("name", name)
                .add("email", email)
                .add("height", height)
                .add("weight", weight)
                .add("gender", gender)
                .add("created", created)
                .add("last_modified", lastModified)
                .add("DOB", DOB)
                .toString();
    }

    private String obscurePassword(final String password) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < password.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public static Account forApplication(final Long id, final Account account) {
//        return new Account();
        return null;
    }
}
