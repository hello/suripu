package com.hello.suripu.coredw.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.converters.HttpUtils;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.oauth.Util;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.auth.AuthenticationException;
import com.yammer.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;

public class OAuthProvider implements InjectableProvider<Scope, Type> {

    private static class OAuthWithScopeInjectable extends AbstractHttpContextInjectable {
            private static final Logger LOGGER = LoggerFactory.getLogger(OAuthWithScopeInjectable.class);
            private static final String HEADER_NAME = "WWW-Authenticate";
            private static final String HEADER_VALUE = "Bearer realm=\"%s\"";

            private final Authenticator<ClientCredentials, AccessToken> authenticator;
            private final String realm;
            private final OAuthScope[] scopes;
            private final DataLogger logger;

            private OAuthWithScopeInjectable(Authenticator<ClientCredentials, AccessToken> authenticator,
                                             String realm, OAuthScope[] scopes,
                                             DataLogger logger) {
                this.authenticator = authenticator;
                this.realm = realm;
                this.scopes = scopes;
                this.logger = logger;
            }

            @Override
            public AccessToken getValue(HttpContext c) {
                final String header = c.getRequest().getHeaderValue(HttpHeaders.AUTHORIZATION);
                final LoggingProtos.HttpRequest.Builder httpRequestBuilder = HttpUtils.httpRequestToProtobuf(c.getRequest());
                final Optional<String> bearerString = Util.extractBearerToken(header);

                if (bearerString.isPresent()) {
                    AccessToken accessToken = null;
                    try {
                        final ClientCredentials creds = new ClientCredentials(scopes, bearerString.get());
                        final Optional<AccessToken> result = authenticator.authenticate(creds);

                        if (!result.isPresent()) {
                            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                        }
                        accessToken  = result.get();
                    } catch (MissingRequiredScopeAuthenticationException e) {
                        throw new WebApplicationException(Response.Status.FORBIDDEN);
                    } catch (AuthenticationException e) {
                        LOGGER.warn("Error authenticating credentials {}", e);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    }

                    httpRequestBuilder.setAccessToken(accessToken.serializeAccessToken());
                    httpRequestBuilder.setApplicationId(accessToken.appId);
                    httpRequestBuilder.setAccessTokenCreatedAt(accessToken.createdAt.getMillis());

                    for(final OAuthScope scope : accessToken.scopes) {
                        httpRequestBuilder.addProvidedScopes(scope.name());
                    }

                    for(final OAuthScope scope : scopes) {
                        httpRequestBuilder.addRequiredScopes(scope.name());
                    }

                    logger.putAsync(accessToken.accountId.toString(), httpRequestBuilder.build().toByteArray());
                    return accessToken;
                }

                for(final OAuthScope scope : scopes) {
                    if(scope == OAuthScope.ADMINISTRATION_READ ||
                       scope == OAuthScope.ADMINISTRATION_WRITE ||
                       scope == OAuthScope.API_INTERNAL_DATA_READ ||
                       scope == OAuthScope.API_INTERNAL_DATA_WRITE) {

                        final String oauthTokenMaybe = (bearerString.isPresent()) ? bearerString.get() : "[NO TOKEN]";
                        LOGGER.warn("Attempt to access admin protected resource with bad/wrong oauth token");
                        LOGGER.warn("OAuth token was: {} and request was {}", oauthTokenMaybe , c.getRequest().getAbsolutePath());
                        LOGGER.warn("Voluntarily returning a 404 to not expose too much to the client");
                        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
                    }
                }

                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        private final Authenticator<ClientCredentials, AccessToken> authenticator;
        private final String realm;
        private final DataLogger logger;

        /**
         * Creates a new OAuthProvider with the given {@link Authenticator} and realm.
         *
         * @param authenticator the authenticator which will take the OAuth2 bearer token and convert
         * them into instances of {@code T}
         * @param realm the name of the authentication realm
         */
        public OAuthProvider(Authenticator<ClientCredentials, AccessToken> authenticator, String realm, DataLogger logger) {
            this.authenticator = authenticator;
            this.realm = realm;
            this.logger = logger;
        }

        @Override
        public ComponentScope getScope() {
            return ComponentScope.PerRequest;
        }

        @Override
        public Injectable<?> getInjectable(ComponentContext ic,
                                           Scope a,
                                           Type c) {
            return new OAuthWithScopeInjectable(authenticator, realm, a.value(), logger);
        }
    }
