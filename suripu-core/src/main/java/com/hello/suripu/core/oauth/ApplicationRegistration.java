package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ApplicationRegistration {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("client_id")
    public final String clientId;

    @JsonProperty("client_secret")
    public final String clientSecret;

    // TODO: implement real redirectURI type
    @JsonProperty("redirect_uri")
    public final String redirectURI;

    @JsonProperty("scopes")
    public final OAuthScope[] scopes;

    @JsonIgnore
    public final Optional<Long> developerAccountId;

    @JsonProperty("description")
    public final String description;

    @JsonProperty("created")
    public final DateTime created;

    @JsonProperty("grant_type")
    public final GrantType grantType;

    @JsonCreator
    public ApplicationRegistration(
            @JsonProperty("name") final String name,
            @JsonProperty("client_id") final String clientId,
            @JsonProperty("client_secret") final String clientSecret,
            @JsonProperty("redirect_uri") final String redirectURI,
            @JsonProperty("scopes") final OAuthScope[] scopes,
            @JsonProperty("description") final String description,
            @JsonProperty("grant_type") final GrantType grantType
    ) {
        this(name, clientId, clientSecret, redirectURI, scopes, null, description, DateTime.now(DateTimeZone.UTC), grantType);
    }

    /**
     * Full constructor requires a creation datetime.
     * JsonCreator should NEVER be able to set this
     *
     * @param name
     * @param clientId
     * @param clientSecret
     * @param redirectURI
     * @param scopes
     * @param developerAccountId
     * @param description
     * @param created
     * @param grantType
     */
    private ApplicationRegistration(
        final String name,
        final String clientId,
        final String clientSecret,
        final String redirectURI,
        final OAuthScope[] scopes,
        final Long developerAccountId,
        final String description,
        final DateTime created,
        final GrantType grantType
    ) {
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;
        this.scopes = scopes;
        this.developerAccountId = Optional.fromNullable(developerAccountId);
        this.description = description;
        this.created = created;
        this.grantType = grantType == null ? GrantType.PASSWORD : grantType;
    }
    public static ApplicationRegistration addDevAccountId(final ApplicationRegistration applicationRegistration, final Long devAccountId) {
        return new ApplicationRegistration(
                applicationRegistration.name,
                applicationRegistration.clientId,
                applicationRegistration.clientSecret,
                applicationRegistration.redirectURI,
                applicationRegistration.scopes,
                devAccountId,
                applicationRegistration.description,
                applicationRegistration.created,
                applicationRegistration.grantType
            );
    }
}
