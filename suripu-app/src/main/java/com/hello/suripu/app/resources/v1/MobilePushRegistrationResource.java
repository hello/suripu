package com.hello.suripu.app.resources.v1;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.notifications.NotificationSubscriptionsDAO;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/v1/notifications/registration")
public class MobilePushRegistrationResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final AmazonSNSClient amazonSNSClient;
    private final String IOS_DEV_ARN = "arn:aws:sns:us-east-1:053216739513:app/APNS/hello-sense-ios-dev";
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;

    public MobilePushRegistrationResource(final AmazonSNSClient amazonSNSClient, final NotificationSubscriptionsDAO notificationSubscriptionsDAO) {
        this.amazonSNSClient = amazonSNSClient;
        this.notificationSubscriptionsDAO = notificationSubscriptionsDAO;
    }

    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerDevice(@Scope(OAuthScope.USER_EXTENDED) final AccessToken accessToken,
                               final @Valid MobilePushRegistration mobilePushRegistration) {


        LOGGER.debug("{}", mobilePushRegistration);
        final CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();

        request.setCustomUserData(accessToken.accountId.toString());
        request.withToken(mobilePushRegistration.deviceToken); //custom per user
        request.setPlatformApplicationArn(IOS_DEV_ARN);

        // TODO: catch exceptions when creating endpoint fails
        final CreatePlatformEndpointResult blah = amazonSNSClient.createPlatformEndpoint(request);

        final MobilePushRegistration m = MobilePushRegistration.withEndpointForAccount(
                mobilePushRegistration,
                blah.getEndpointArn(),
                accessToken.accountId
        );
        notificationSubscriptionsDAO.subscribe(accessToken.accountId, m);
        LOGGER.debug("Account (%s) is subscribed to (%s)", accessToken.accountId, blah.getEndpointArn());
    }

    @GET
    @Path("/{device_token}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDeviceInfo(@Scope(OAuthScope.USER_EXTENDED) final AccessToken accessToken, @PathParam("device_token") final String deviceToken) {
        notificationSubscriptionsDAO.getSubscription(accessToken.accountId, deviceToken);
        return "";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<MobilePushRegistration> getDeviceInfo(@Scope(OAuthScope.USER_EXTENDED) final AccessToken accessToken) {
        return notificationSubscriptionsDAO.getSubscriptions(accessToken.accountId);

    }

    @GET
    @Path("/trigger")
    @Produces(MediaType.APPLICATION_JSON)
    public String trigger(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken) {

        final Map<String, String> helloMessage = new HashMap<>();
        helloMessage.put("hello", "world");

        final List<MobilePushRegistration> list = notificationSubscriptionsDAO.getSubscriptions(accessToken.accountId);
        LOGGER.debug("Found {} mobilepush registrations", list.size());
        for(final MobilePushRegistration m : list) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                final GetEndpointAttributesRequest endpointAttributesRequest = new GetEndpointAttributesRequest();
                endpointAttributesRequest.withEndpointArn(m.endpoint.get());

                GetEndpointAttributesResult res = amazonSNSClient.getEndpointAttributes(endpointAttributesRequest);
                Map<String, String> attr = res.getAttributes();
                LOGGER.debug("{}", attr);

                final PublishRequest request = new PublishRequest();
                request.setMessage(mapper.writeValueAsString(helloMessage));
                request.setTargetArn(m.endpoint.get());
                amazonSNSClient.publish(request);
                LOGGER.debug("Push notification sent");
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Exception: {}", e.getMessage() );
            }
        }
        return "OK";
    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(
            @Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
            @Valid MobilePushRegistration mobilePushRegistration) {

        final Optional<MobilePushRegistration> optional = notificationSubscriptionsDAO.getSubscription(accessToken.accountId, mobilePushRegistration.deviceToken);
        if(!optional.isPresent()) {
            LOGGER.debug("Did not find subscription with token = {}", mobilePushRegistration.deviceToken);
            throw new WebApplicationException(404);
        }

        final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest();
        deleteEndpointRequest.withEndpointArn(optional.get().endpoint.get());
        amazonSNSClient.deleteEndpoint(deleteEndpointRequest);
        boolean deleted = notificationSubscriptionsDAO.unsubscribe(accessToken.accountId, mobilePushRegistration.deviceToken);
        if(!deleted) {
            LOGGER.warn("{} Was not successfully deleted for account = {}", mobilePushRegistration.deviceToken, accessToken.accountId);
        }
    }
}
