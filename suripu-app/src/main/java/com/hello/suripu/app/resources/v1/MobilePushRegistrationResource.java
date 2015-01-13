package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.notifications.HelloPushMessage;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

;

@Path("/v1/notifications")
public class MobilePushRegistrationResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;
    private final MobilePushNotificationProcessor pushNotificationProcessor;
    private final AccountDAO accountDAO;

    public MobilePushRegistrationResource(
            final NotificationSubscriptionsDAO notificationSubscriptionsDAO,
            final MobilePushNotificationProcessor pushNotificationProcessor,
            final AccountDAO accountDAO) {
        this.notificationSubscriptionsDAO = notificationSubscriptionsDAO;
        this.pushNotificationProcessor = pushNotificationProcessor;
        this.accountDAO = accountDAO;
    }

    @Timed
    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerDevice(@Scope(OAuthScope.PUSH_NOTIFICATIONS) final AccessToken accessToken,
                               final @Valid MobilePushRegistration mobilePushRegistration) {

        LOGGER.debug("{}", mobilePushRegistration);
        notificationSubscriptionsDAO.subscribe(accessToken.accountId, mobilePushRegistration);
    }

    @DELETE
    @Timed
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(
            @Scope(OAuthScope.PUSH_NOTIFICATIONS) final AccessToken accessToken,
            @Valid MobilePushRegistration mobilePushRegistration) {

        boolean deleted = notificationSubscriptionsDAO.unsubscribe(accessToken.accountId, mobilePushRegistration.deviceToken);
        if(!deleted) {
            LOGGER.warn("{} Was not successfully deleted for account = {}", mobilePushRegistration.deviceToken, accessToken.accountId);
        }
    }

    @POST
    @Path("/send/{email}")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public Response send(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @PathParam("email") final String email,
            @Valid final HelloPushMessage message) {

        final Optional<Account> accountOptional = accountDAO.getByEmail(email);
        if(!accountOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        pushNotificationProcessor.push(accountOptional.get().id.get(), message);
        return Response.ok().build();
    }
}
