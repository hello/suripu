package com.hello.suripu.coredw8.oauth;

import com.google.common.base.Optional;

import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.util.HttpUtils;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(Priorities.AUTHENTICATION)
public class OAuthCredentialAuthFilter<P extends Principal> extends AuthFilter<String, P> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthCredentialAuthFilter.class);
    private OAuthCredentialAuthFilter() {
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
      final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      final LoggingProtos.HttpRequest.Builder httpRequestBuilder = HttpUtils.httpRequestToProtobuf(requestContext);

        if (header != null) {
            try {
                final int space = header.indexOf(' ');
                if (space > 0) {
                    final String method = header.substring(0, space);
                    if (prefix.equalsIgnoreCase(method)) {
                        final String credentials = header.substring(space + 1);
                        final Optional<P> principal = authenticator.authenticate(credentials);

                        if (!principal.isPresent()) {
                          throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                        }

                        requestContext.setSecurityContext(new SecurityContext() {
                            @Override
                            public Principal getUserPrincipal() {
                                return principal.get();
                            }

                            @Override
                            public boolean isUserInRole(String role) {

                              final OAuthScope roleScope = OAuthScope.valueOf(role);
                              final AccessToken accessToken = (AccessToken)principal.get();

                              httpRequestBuilder.setAccessToken(accessToken.serializeAccessToken());
                              httpRequestBuilder.setApplicationId(accessToken.appId);
                              httpRequestBuilder.setAccessTokenCreatedAt(accessToken.createdAt.getMillis());

                              for(final OAuthScope scope : accessToken.scopes) {
                                httpRequestBuilder.addProvidedScopes(scope.name());
                              }

                              httpRequestBuilder.addRequiredScopes(roleScope.name());

                              logger.putAsync(accessToken.token.toString(), httpRequestBuilder.build().toByteArray());

                              return authorizer.authorize(principal.get(), roleScope);
                            }

                            @Override
                            public boolean isSecure() {
                                return requestContext.getSecurityContext().isSecure();
                            }

                            @Override
                            public String getAuthenticationScheme() {
                                return SecurityContext.BASIC_AUTH;
                            }
                        });
                      return;
                    }
                }
            } catch (MissingRequiredScopeAuthenticationException e) {
              throw new WebApplicationException(Response.Status.FORBIDDEN);
            } catch (AuthenticationException e) {
              LOGGER.error("error=credential_authentication message={}", e);
              throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    /**
     * Builder for {@link OAuthCredentialAuthFilter}.
     * <p>An {@link Authenticator} must be provided during the building process.</p>
     *
     * @param <P> the type of the principal
     */
    public static class Builder<P extends Principal>
            extends AuthFilterBuilder<String, P, OAuthCredentialAuthFilter<P>> {

        @Override
        protected OAuthCredentialAuthFilter<P> newInstance() {
            return new OAuthCredentialAuthFilter<>();
        }
    }
}