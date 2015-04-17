package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.oauth.stores.ApplicationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/applications")
public class ApplicationResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationResources.class);
    private final ApplicationStore<Application, ApplicationRegistration> applicationStore;

    public ApplicationResources(final ApplicationStore<Application, ApplicationRegistration> applicationStore) {
        this.applicationStore = applicationStore;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(
            @Valid final ApplicationRegistration applicationRegistration,
            @Scope({OAuthScope.ADMINISTRATION_WRITE}) final AccessToken token) {
        final ApplicationRegistration applicationWithDevAccountId = ApplicationRegistration.addDevAccountId(applicationRegistration, token.accountId);
        applicationStore.register(applicationWithDevAccountId);
        return Response.ok().build();
    }

    @GET
    @Path("/{dev_account_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getApplicationsByDeveloper(
            @Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken token,
            @PathParam("dev_account_id") final Long devAccountId) {

        return applicationStore.getApplicationsByDevId(devAccountId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getAllApplications(
            @Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken accessToken) {
        List<Application> applications = applicationStore.getAll();
        LOGGER.debug("Size of applications = {}", applications.size());
        return applications;
    }


    @GET
    @Path("/scopes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthScope> scopes(@Scope({OAuthScope.ADMINISTRATION_READ}) AccessToken accessToken) {
        final List<OAuthScope> scopes = new ArrayList<>();
        for(OAuthScope scope : OAuthScope.values()) {
            scopes.add(scope);
        }
        return scopes;
    }


    @GET
    @Path("/{id}/scopes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthScope> scopesForApplication(@Scope({OAuthScope.ADMINISTRATION_READ}) AccessToken accessToken, @PathParam("id") Long applicationId) {
        final Optional<Application> applicationOptional = applicationStore.getApplicationById(applicationId);
        if(!applicationOptional.isPresent()) {

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final List<OAuthScope> scopes = new ArrayList<>();

        for(OAuthScope scope : applicationOptional.get().scopes) {
            scopes.add(scope);
        }

        return scopes;
    }

    @PUT
    @Path("/{id}/scopes")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateScopes(@Scope({OAuthScope.ADMINISTRATION_WRITE}) AccessToken accessToken, @Valid List<OAuthScope> scopes, @PathParam("id") Long applicationId) {
        applicationStore.updateScopes(applicationId, scopes);
    }
}
