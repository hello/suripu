package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("dev_account_id")
    public final Long developerAccountId;

    @JsonProperty("description")
    public final String description;

    @JsonProperty("created")
    public final DateTime created;

    @JsonCreator
    public ApplicationRegistration(
            @JsonProperty("name") final String name,
            @JsonProperty("client_id") final String clientId,
            @JsonProperty("client_secret") final String clientSecret,
            @JsonProperty("redirect_uri") final String redirectURI,
            @JsonProperty("scopes") final OAuthScope[] scopes,
            @JsonProperty("dev_account_id") final Long developerAccountId,
            @JsonProperty("description") final String description
    ) {
        this(name, clientId, clientSecret, redirectURI, scopes, developerAccountId, description, DateTime.now(DateTimeZone.UTC));
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
     */
    public ApplicationRegistration(
        final String name,
        final String clientId,
        final String clientSecret,
        final String redirectURI,
        final OAuthScope[] scopes,
        final Long developerAccountId,
        final String description,
        final DateTime created
    ) {
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;
        this.scopes = scopes;
        this.developerAccountId = developerAccountId;
        this.description = description;
        this.created = created;
    }
}
