package com.hello.suripu.coredropwizard.oauth;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.ApplicationStore;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

/**
 * Created by jnorgan on 8/21/15.
 */
public class ScopesAllowedDynamicFeature implements DynamicFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopesAllowedDynamicFeature.class);

    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;

    public ScopesAllowedDynamicFeature(final ApplicationStore<Application, ApplicationRegistration> applicationStore) {
        this.applicationStore = applicationStore;
    }
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
            configuration.register(new RolesAllowedRequestFilter(ra.value(), applicationStore));
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
            configuration.register(new RolesAllowedRequestFilter(ra.value(), applicationStore));
        }
    }

    @Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
    private static class RolesAllowedRequestFilter implements ContainerRequestFilter {

        private final boolean denyAll;
        private final OAuthScope[] scopesAllowed;
        private final ApplicationStore<Application, ApplicationRegistration> applicationStore;

        RolesAllowedRequestFilter() {
            this.denyAll = true;
            this.scopesAllowed = null;
            this.applicationStore = null;
        }

        RolesAllowedRequestFilter(final OAuthScope[] scopesAllowed, final ApplicationStore<Application, ApplicationRegistration> appStore) {
            this.denyAll = false;
            this.scopesAllowed = (scopesAllowed != null) ? scopesAllowed : new OAuthScope[] {};
            this.applicationStore = appStore;
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

            final AccessToken accessToken = (AccessToken) requestContext.getSecurityContext().getUserPrincipal();
            final Optional<Application> applicationOptional = this.applicationStore.getApplicationById(accessToken.appId);

            if(!applicationOptional.isPresent()) {
                LOGGER.warn("warning=no_app_with_id application_id={} token={}", accessToken.appId, accessToken);
            }

            boolean validScopes = hasRequiredScopes(applicationOptional.get().scopes, scopesAllowed);
            if(!validScopes) {
                LOGGER.warn("warning=token_not_authorized account_id={} application_id={} token={} allowed_scopes={}",
                    accessToken.accountId,
                    accessToken.appId,
                    accessToken.token,
                    scopesAllowed,
                    accessToken.accountId,
                    accessToken.appId);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
        }

        public boolean hasRequiredScopes(OAuthScope[] granted, OAuthScope[] required) {
            if(granted.length == 0 || required.length == 0) {
                LOGGER.warn("warning=empty_scopes_check");
                return false;
            }

            final Set<OAuthScope> requiredScopes = Sets.newHashSet(required);
            final Set<OAuthScope> grantedScopes = Sets.newHashSet(granted);

            // Make sure we have all the permissions
            boolean valid = grantedScopes.containsAll(requiredScopes);
            if(!valid) {
                LOGGER.warn("warning=scopes_not_valid required_scopes={} granted_scopes={}", requiredScopes, grantedScopes);
            }

            return valid;
        }
    }
}
