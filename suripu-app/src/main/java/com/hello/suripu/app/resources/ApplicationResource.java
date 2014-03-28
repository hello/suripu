package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.oauth.*;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("applications")
public class ApplicationResource {

    private final ApplicationStore<Application, ApplicationRegistration, ClientDetails> applicationStore;

    public ApplicationResource(final ApplicationStore<Application, ApplicationRegistration, ClientDetails> applicationStore) {
        this.applicationStore = applicationStore;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Valid final ApplicationRegistration applicationRegistration) {


        applicationStore.register(applicationRegistration);
        return Response.ok().build();
    }

    @GET
    @Path("/{dev_account_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Application> getApplicationsByDeveloper(@PathParam("dev_account_id") final Long devAccountId) {
        final List<Application> applications = new ArrayList<Application>();
        return ImmutableList.copyOf(applications);
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRegistration get() {

        final OAuthScope[] scopes = new OAuthScope[]{
                OAuthScope.USER_BASIC,
                OAuthScope.USER_EXTENDED,
                OAuthScope.SENSORS_BASIC,
                OAuthScope.SENSORS_EXTENDED,
        };

        final ApplicationRegistration registration = new ApplicationRegistration(
            "Hello OAuth Application",
            "123456ClientId",
            "654321ClientSecret",
            "http://hello.com/oauth",
            scopes,
            666L,
            "Official Hello Application"
        );
        return registration;
    }
}
