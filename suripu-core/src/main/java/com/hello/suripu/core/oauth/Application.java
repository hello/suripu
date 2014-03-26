package com.hello.suripu.core.oauth;

public class Application {

    public final Long id;
    public final String name;
    public final String clientId;
    public final String clientSecret;
    public final String redirectURI;
    public final OAuthScope[] scopes;
    public final Long developerAccountId;
    public final String description;
    public final Boolean published;

    public Application(
            final Long id,
            final String name,
            final String clientId,
            final String clientSecret,
            final String redirectURI,
            final OAuthScope[] scopes,
            final Long developerAccountId,
            final String description,
            final Boolean published
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
    }
}
