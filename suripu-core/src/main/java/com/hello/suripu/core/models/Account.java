package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = Account.Builder.class)
public class Account {

    public static class MyAccountCreationException extends RuntimeException {
        public MyAccountCreationException(String message) {
            super(message);
        }
    }

    @JsonIgnore
    public final Optional<Long> id;

    @JsonIgnore
    public final Optional<UUID> externalId;

    @JsonProperty("id")
    public String generateExternalId() {
//        final StringBuilder sb = new StringBuilder();
//        sb.append(id);
//        sb.append("|");
//        sb.append(created.getMillis());
//
//        final String digest = DigestUtils.md5Hex(sb.toString());
//        return digest;
//        return id.or(0L).toString();
        return extId();
    }

    @JsonProperty("ext_id")
    public String extId() {
        // Default to internal ID to not eff MP
        final String internalId = id.or(0L).toString();
        return externalId.isPresent() ? externalId.get().toString() : internalId;
    }


    @JsonProperty("email")
    public final String email;

    @JsonProperty("tz")
    public final Integer tzOffsetMillis;

    private final String name;

    @JsonProperty("name")
    public final String name() {
        if(name.isEmpty()) {
            return firstname;
        }
        return name;
    }

    @JsonProperty("firstname")
    public final String firstname;

    @JsonProperty("lastname")
    public final Optional<String> lastname;

    @JsonProperty("gender")
    public final Gender gender;


    private final String genderName;

    @JsonProperty("gender_other")
    public final String genderName() {
        if(Gender.OTHER.equals(gender)) {
            return genderName;
        }

        return "";
    }

    @JsonProperty("height")
    public final Integer height;

    @JsonProperty("weight")
    public final Integer weight;

    @JsonIgnore
    public final String password;

    @JsonProperty("created")
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

    @JsonIgnore
    public final Double latitude;

    @JsonIgnore
    public final Double longitude; // android

    @JsonProperty("profile_photo")
    public final Optional<MultiDensityImage> profilePhoto;



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
                    final Optional<UUID> externalId,
                    final String email,
                    final String password,
                    final Integer tzOffsetMillis,
                    final String name,
                    final String firstname,
                    final Optional<String> lastname,
                    final Gender gender,
                    final String genderName,
                    final Integer height,
                    final Integer weight,
                    final DateTime created,
                    final Long lastModified,
                    final DateTime DOB,
                    final Boolean emailVerified,
                    final Double latitude,
                    final Double longitude,
                    final Optional<MultiDensityImage> profilePhoto
    ) {

        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.password = password;
        this.tzOffsetMillis = tzOffsetMillis;

        this.name = name;
        this.firstname = firstname;
        this.lastname = lastname;
        this.gender = gender;
        this.genderName = genderName;
        this.height = height;
        this.weight = weight;

        this.created = created;

        this.lastModified = lastModified;
        this.DOB = DOB;
        this.emailVerified = emailVerified;

        this.latitude = latitude;
        this.longitude = longitude;

        this.profilePhoto = profilePhoto;
    }

    /**
     * Transform registration to account
     * @param registration
     * @param externalId
     * @return
     */
    public static Account fromRegistration(final Registration registration, final Long id, final UUID externalId) {
        final String firstname = (registration.firstname == null) ? registration.name : registration.firstname;

        return new Account(Optional.fromNullable(id), Optional.of(externalId), registration.email, registration.password, registration.tzOffsetMillis,
                registration.name, firstname, Optional.fromNullable(registration.lastname), registration.gender,
                registration.genderName, registration.height, registration.weight,registration.created, registration.created.getMillis(),
                registration.DOB, Boolean.FALSE, registration.latitude, registration.longitude, Optional.absent());
    }

    /**
     *
     * @param account
     * @param profilePhoto
     * @return
     */
    public static Account withProfilePhoto(final Account account, final MultiDensityImage profilePhoto) {
        final Account.Builder builder = new Account.Builder(account);
        builder.withProfilePhoto(profilePhoto);
        return builder.build();
    }

    public Boolean hasLocation() {
        if (this.latitude == null || this.longitude == null) {
            return false;
        }
        return true;
    }

    public static class Builder {
        private Optional<Long> id;
        private Optional<UUID> externalID;
        private String name;
        private String firstname;
        private Optional<String> lastname;
        private Gender gender;
        private String genderName;
        private Integer height;
        private Integer weight;
        private String password;
        private String email;
        private Integer tzOffsetMillis;
        private DateTime created;
        private Long lastModified;
        private DateTime DOB;
        private Boolean emailVerified;
        private Double latitude;
        private Double longitude;
        private Optional<MultiDensityImage> profilePhoto;

        public Builder() {
            this.id = Optional.absent();
            this.externalID = Optional.absent();
            this.name = "";
            this.firstname = "";
            this.lastname = Optional.absent();
            this.gender = Gender.OTHER;
            this.genderName = "";
            this.height = 0;
            this.weight = 0;
            this.password = "";
            this.email = "";
            this.created = DateTime.now(DateTimeZone.UTC);
            this.lastModified = new DateTime(1970, 1 ,1, 0, 0, DateTimeZone.UTC).getMillis();
            this.DOB = new DateTime(1900,1,1,0,0, DateTimeZone.UTC);
            this.emailVerified = Boolean.FALSE;
            this.latitude = null;
            this.longitude = null;
            this.profilePhoto = Optional.absent();
        }

        public Builder(final Account account) {
            this.id = account.id;
            this.externalID = account.externalId;
            this.email = account.email;
            this.password = account.password;
            this.tzOffsetMillis = account.tzOffsetMillis;
            this.name = account.name;
            this.firstname = account.firstname;
            this.lastname = account.lastname;
            this.gender = account.gender;
            this.genderName = account.genderName;
            this.height = account.height;
            this.weight = account.weight;
            this.created = account.created;
            this.lastModified = account.lastModified;
            this.DOB = account.DOB;
            this.emailVerified = account.emailVerified;
            this.latitude = account.latitude;
            this.longitude = account.longitude;
            this.profilePhoto = account.profilePhoto;
        }

        @JsonProperty("name")
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        @JsonProperty("firstname")
        public Builder withFirstname(final String firstname) {
            this.firstname = firstname;
            return this;
        }

        @JsonProperty("lastname")
        public Builder withLastname(final String lastname) {
            this.lastname = Optional.fromNullable(lastname);
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

        @JsonProperty("gender_other")
        public Builder withGenderName(final String genderName) {
            this.genderName = (genderName == null) ? "" : genderName;
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

        @JsonIgnore
        public Builder withId(final Long id) {
            this.id = Optional.fromNullable(id);
            return this;
        }

        public Builder withExternalId(final UUID uuid) {
            this.externalID = Optional.fromNullable(uuid);
            return this;
        }

        @JsonProperty("tz")
        public Builder withTzOffsetMillis(final Integer tzOffsetMillis) {
            this.tzOffsetMillis = tzOffsetMillis;
            return this;
        }

        @JsonProperty("created")
        public Builder withCreated(final Long created) {
            this.created = new DateTime(created, DateTimeZone.UTC);
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

        @JsonProperty("lat")
        public Builder withLatitude(final Double latitude) {
            this.latitude = latitude;
            return this;
        }

        @JsonProperty("long")
        public Builder withLongitude(final Double longitude) {
            this.longitude = longitude;
            return this;
        }

        @JsonProperty("lon")
        public Builder withLongitudeLon(final Double longitudeLon) {
            this.longitude = longitudeLon;
            return this;
        }

        public Builder withProfilePhoto(final MultiDensityImage multiDensityImage) {
            this.profilePhoto = Optional.fromNullable(multiDensityImage);
            return this;
        }

        public Account build() throws MyAccountCreationException {
            checkNotNull(id, "ID can not be null");
            checkNotNull(email, "Email can not be null");
            return new Account(id, externalID, email, password, tzOffsetMillis, name, firstname, lastname, gender, genderName, height, weight,
                    created, lastModified, DOB, emailVerified, latitude, longitude, profilePhoto);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Account.class)
                .add("id", (id.isPresent()) ? id.get() : "N/A")
                .add("external_id", externalId.or(new UUID(0L, 0L)))
                .add("email", email)
                .add("password", obscurePassword(password))
                .add("tz", tzOffsetMillis)
                .add("name", name)
                .add("firstname", firstname)
                .add("lastname", lastname)
                .add("email", email)
                .add("height", height)
                .add("weight", weight)
                .add("gender", gender)
                .add("gender_name", genderName)
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
                account.externalId,
                account.email.toLowerCase().trim(),
                account.password,
                account.tzOffsetMillis,
                account.name,
                account.firstname,
                account.lastname,
                account.gender,
                account.genderName,
                account.height,
                account.weight,
                account.created,
                account.lastModified,
                account.DOB,
                account.emailVerified,
                account.latitude,
                account.longitude,
                account.profilePhoto
        );
    }

    public static Account forApplication(final Long applicationId, final Account account) {
//        return new Account();
        return account;
    }

    @JsonIgnore
    public int getAgeInDays() {
        return Days.daysBetween(created, DateTime.now(DateTimeZone.UTC)).getDays();
    }
}
