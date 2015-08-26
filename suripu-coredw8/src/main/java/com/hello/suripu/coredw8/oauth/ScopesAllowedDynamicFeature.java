package com.hello.suripu.coredw8.oauth;

import com.hello.suripu.core.oauth.OAuthScope;
import java.io.IOException;
import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.AnnotatedMethod;

/**
 * Created by jnorgan on 8/21/15.
 */
public class ScopesAllowedDynamicFeature implements DynamicFeature {

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext configuration) {
        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        // DenyAll on the method take precedence over RolesAllowed and PermitAll
        if (am.isAnnotationPresent(DenyAll.class)) {
            configuration.register(new RolesAllowedRequestFilter());
            return;
        }

        // RolesAllowed on the method takes precedence over PermitAll
        ScopesAllowed ra = am.getAnnotation(ScopesAllowed.class);
        if (ra != null) {
            configuration.register(new RolesAllowedRequestFilter(ra.value()));
            return;
        }

        // PermitAll takes precedence over RolesAllowed on the class
        if (am.isAnnotationPresent(PermitAll.class)) {
            // Do nothing.
            return;
        }

        // DenyAll can't be attached to classes

        // RolesAllowed on the class takes precedence over PermitAll
        ra = resourceInfo.getResourceClass().getAnnotation(ScopesAllowed.class);
        if (ra != null) {
            configuration.register(new RolesAllowedRequestFilter(ra.value()));
        }
    }

    @Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
    private static class RolesAllowedRequestFilter implements ContainerRequestFilter {

        private final boolean denyAll;
        private final OAuthScope[] scopesAllowed;

        RolesAllowedRequestFilter() {
            this.denyAll = true;
            this.scopesAllowed = null;
        }

        RolesAllowedRequestFilter(final OAuthScope[] scopesAllowed) {
            this.denyAll = false;
            this.scopesAllowed = (scopesAllowed != null) ? scopesAllowed : new OAuthScope[] {};
        }

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            if (!denyAll) {
                if (scopesAllowed.length > 0
                        && requestContext.getSecurityContext().getUserPrincipal() == null) {
                    throw new NotAuthorizedException(LocalizationMessages.USER_NOT_AUTHORIZED());
                }

                for (final OAuthScope role : scopesAllowed) {
                    if (requestContext.getSecurityContext().isUserInRole(role.toString())) {
                        return;
                    }
                }
            }

            throw new ForbiddenException();
        }
    }
}
