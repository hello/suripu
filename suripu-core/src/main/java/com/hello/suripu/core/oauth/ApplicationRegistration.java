package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;
        this.scopes = scopes;
        this.developerAccountId = developerAccountId;
        this.description = description;
    }
}
