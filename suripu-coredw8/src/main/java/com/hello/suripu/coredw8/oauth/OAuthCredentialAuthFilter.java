package com.hello.suripu.coredw8.oauth;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.OAuthScope;
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
        if (header != null) {
            try {
                final int space = header.indexOf(' ');
                if (space > 0) {
                    final String method = header.substring(0, space);
                    if (prefix.equalsIgnoreCase(method)) {
                        final String credentials = header.substring(space + 1);
                        final Optional<P> principal = authenticator.authenticate(credentials);
                        if (principal.isPresent()) {
                            requestContext.setSecurityContext(new SecurityContext() {
                                @Override
                                public Principal getUserPrincipal() {
                                    return principal.get();
                                }

                                @Override
                                public boolean isUserInRole(String role) {
                                    final OAuthScope roleScope = OAuthScope.valueOf(role);
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
                }
            } catch (AuthenticationException e) {
                LOGGER.warn("Error authenticating credentials", e);
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