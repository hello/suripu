package com.hello.suripu.app.resources.v1;

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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/v1/notifications/registration")
public class MobilePushRegistrationResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;

    public MobilePushRegistrationResource(final NotificationSubscriptionsDAO notificationSubscriptionsDAO) {
        this.notificationSubscriptionsDAO = notificationSubscriptionsDAO;
    }

    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerDevice(@Scope(OAuthScope.USER_EXTENDED) final AccessToken accessToken,
                               final @Valid MobilePushRegistration mobilePushRegistration) {

        LOGGER.debug("{}", mobilePushRegistration);
        notificationSubscriptionsDAO.subscribe(accessToken.accountId, mobilePushRegistration);
    }


//    @GET
//    @Path("/trigger")
//    public void trigger(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken) {
//
//        final Map<String, String> helloMessage = new HashMap<>();
//        helloMessage.put("hello", "world");
//
//        final List<MobilePushRegistration> list = notificationSubscriptionsDAO.getSubscriptions(accessToken.accountId);
//        LOGGER.debug("Found {} mobilepush registrations", list.size());
//        for(final MobilePushRegistration m : list) {
//            final ObjectMapper mapper = new ObjectMapper();
//            try {
//                final GetEndpointAttributesRequest endpointAttributesRequest = new GetEndpointAttributesRequest();
//                endpointAttributesRequest.withEndpointArn(m.endpoint.get());
//
//                GetEndpointAttributesResult res = amazonSNSClient.getEndpointAttributes(endpointAttributesRequest);
//                Map<String, String> attr = res.getAttributes();
//                LOGGER.debug("{}", attr);
//
//                final PublishRequest request = new PublishRequest();
//                request.setMessage(mapper.writeValueAsString(helloMessage));
//                request.setTargetArn(m.endpoint.get());
//                amazonSNSClient.publish(request);
//                LOGGER.debug("Push notification sent");
//            } catch (JsonProcessingException e) {
//                LOGGER.error(e.getMessage());
//            } catch (Exception e) {
//                LOGGER.error("Exception: {}", e.getMessage() );
//            }
//        }
//    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(
            @Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
            @Valid MobilePushRegistration mobilePushRegistration) {

        boolean deleted = notificationSubscriptionsDAO.unsubscribe(accessToken.accountId, mobilePushRegistration.deviceToken);
        if(!deleted) {
            LOGGER.warn("{} Was not successfully deleted for account = {}", mobilePushRegistration.deviceToken, accessToken.accountId);
        }
    }
}
