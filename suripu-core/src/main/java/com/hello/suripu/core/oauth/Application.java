package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Application {

    @JsonIgnore
    public final Long id;

    @JsonProperty("name")
    public final String name;

    @JsonProperty("client_id")
    public final String clientId;

    @JsonProperty("client_id")
    public final String clientSecret;

    @JsonProperty("redirect_uri")
    public final String redirectURI;

    @JsonProperty("scopes")
    public final OAuthScope[] scopes;

    @JsonProperty("developer_account_id")
    public final Long developerAccountId;

    @JsonProperty("description")
    public final String description;

    @JsonProperty("published")
    public final Boolean published;

    @JsonProperty("created")
    public final DateTime created;

    public Application(
            final Long id,
            final String name,
            final String clientId,
            final String clientSecret,
            final String redirectURI,
            final OAuthScope[] scopes,
            final Long developerAccountId,
            final String description,
            final Boolean published,
            final DateTime created
    ) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;
        this.scopes = scopes;
        this.developerAccountId = developerAccountId;
        this.description = description;
        this.published = published;
        this.created = created;
    }

    public static Application fromApplicationRegistration(final ApplicationRegistration registration, Long id) {
        return new Application(
                id,
                registration.name,
                registration.clientId,
                registration.clientSecret,
                registration.redirectURI,
                registration.scopes,
                registration.developerAccountId,
                registration.description,
                Boolean.FALSE, // Application aren't published by default.
                DateTime.now()
        );
    }
}
