package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hello.suripu.core.util.DateTimeUtil;
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
    public String generateExternalId() {
        final StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append("|");
        sb.append(created.getMillis());

        final String digest = DigestUtils.md5Hex(sb.toString());
        return digest;
    };

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

    @JsonIgnore
    public final DateTime DOB;

    @JsonProperty("dob")
    public String getDateTime() {
        return DateTimeUtil.dateToYmdString(DOB);
    }

    @JsonProperty("email_verified")
    public final Boolean emailVerified;


    /**
     *
     * @param id
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
                    final String email,
                    final String password,
                    final Integer tzOffsetMillis,
                    final String name,
                    final Gender gender,
                    final Integer height,
                    final Integer weight,
                    final DateTime created,
                    final Long lastModified,
                    final DateTime DOB,
                    final Boolean emailVerified) {

        this.id = id;
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
        this.emailVerified = emailVerified;
    }

    /**
     * Transform registration to account
     * @param registration
     * @param id
     * @return
     */
    public static Account fromRegistration(final Registration registration, final Long id) {
        return new Account(Optional.fromNullable(id), registration.email, registration.password, registration.tzOffsetMillis,
                registration.name, registration.gender, registration.height, registration.weight, registration.created,
                registration.created.getMillis(), registration.DOB, Boolean.FALSE);
    }


    public static class Builder {
        private Optional<Long> id;
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
        private Boolean emailVerified;

        public Builder() {
            this.id = Optional.absent();
            this.name = "";
            this.gender = Gender.OTHER;
            this.height = 0;
            this.weight = 0;
            this.password = "";
            this.email = "";
            this.created = DateTime.now(DateTimeZone.UTC);
            this.lastModified = new DateTime(1970, 1 ,1, 0, 0, DateTimeZone.UTC).getMillis();
            this.DOB = new DateTime(1900,1,1,0,0, DateTimeZone.UTC);
            this.emailVerified = Boolean.FALSE;
        }

        public Builder(final Account account) {
            this.id = account.id;
            this.email = account.email;
            this.password = account.password;
            this.tzOffsetMillis = account.tzOffsetMillis;
            this.name = account.name;
            this.gender = account.gender;
            this.height = account.height;
            this.weight = account.weight;
            this.created = account.created;
            this.lastModified = account.lastModified;
            this.DOB = account.DOB;
            this.emailVerified = account.emailVerified;
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
        public Builder withCreated(final DateTime created) {
            this.created = created;
            return this;
        }

        @JsonProperty("last_modified")
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

        @JsonIgnore
        public Builder withAccountVerified(final Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Account build() throws MyAccountCreationException {
            checkNotNull(id, "ID can not be null");
            checkNotNull(email, "Email can not be null");
            return new Account(id, email, password, tzOffsetMillis, name, gender, height, weight, created, lastModified, DOB, emailVerified);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Account.class)
                .add("id", (id.isPresent()) ? id.get() : "N/A")
                .add("external_id", generateExternalId())
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
                .add("email_verified", emailVerified)
                .toString();
    }

    /**
     * We don't want to leak the password in the logs, do we?
     * @param password
     * @return
     */
    private String obscurePassword(final String password) {
        final StringBuilder sb = new StringBuilder();

        for(int i=0; i < password.length(); i++) {
            sb.append("*");
        }

        return sb.toString();
    }

    public static Account normalizeWithId(final Account account, final Long accountId) {
        return new Account(
                Optional.fromNullable(accountId),
                account.email.toLowerCase(),
                account.password,
                account.tzOffsetMillis,
                account.name,
                account.gender,
                account.height,
                account.weight,
                account.created,
                account.lastModified,
                account.DOB,
                account.emailVerified
        );
    }

    public static Account forApplication(final Long aaplicationId, final Account account) {
//        return new Account();
        return null;
    }
}
