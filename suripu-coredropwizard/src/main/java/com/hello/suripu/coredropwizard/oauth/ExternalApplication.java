package com.hello.suripu.coredropwizard.oauth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.oauth.GrantType;

import org.joda.time.DateTime;

public class ExternalApplication {

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("name")
    public final String name;

    @JsonProperty("client_id")
    public final String clientId;

    @JsonProperty("client_secret")
    public final String clientSecret;

    @JsonProperty("auth_uri")
    public final String authURI;

    @JsonProperty("token_uri")
    public final String tokenURI;

    @JsonProperty("description")
    public final String description;

    @JsonProperty("created")
    public final DateTime created;

    @JsonIgnore
    public final GrantType grantType;

    public ExternalApplication(
            final Long id,
            final String name,
            final String clientId,
            final String clientSecret,
            final String authURI,
            final String tokenURI,
            final String description,
            final DateTime created,
            final GrantType grantType
    ) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authURI = authURI;
        this.tokenURI = tokenURI;
        this.description = description;
        this.created = created;
        this.grantType = grantType;
    }

}
