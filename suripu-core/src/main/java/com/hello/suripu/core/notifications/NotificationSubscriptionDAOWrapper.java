package com.hello.suripu.core.notifications;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationSubscriptionDAOWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSubscriptionDAOWrapper.class);
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;

    private final AmazonSNSClient amazonSNSClient;
    private final Map<String, String> arns;

    private NotificationSubscriptionDAOWrapper(final NotificationSubscriptionsDAO dao, final AmazonSNSClient amazonSNSClient, final Map<String, String> arns) {
        checkNotNull(arns, "arns can't be null");

        this.notificationSubscriptionsDAO = dao;
        this.amazonSNSClient = amazonSNSClient;
        this.arns = ImmutableMap.copyOf(arns);
    }

    public static NotificationSubscriptionDAOWrapper create(final NotificationSubscriptionsDAO dao, final AmazonSNSClient amazonSNSClient, final Map<String, String> arns) {
        return new NotificationSubscriptionDAOWrapper(dao, amazonSNSClient, arns);
    }


    public Optional<MobilePushRegistration> getSubscription(final Long accountId, final String deviceToken) {
        return notificationSubscriptionsDAO.getSubscription(accountId, deviceToken);
    }


    public ImmutableList<MobilePushRegistration> getSubscriptions(final Long accountId) {
        return notificationSubscriptionsDAO.getSubscriptions(accountId);
    }


    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final ImmutableList<MobilePushRegistration> previousRegistrations = notificationSubscriptionsDAO.getSubscriptionsByDeviceToken(mobilePushRegistration.deviceToken);
        deleteFromSNS(previousRegistrations);
        final MobilePushRegistration updated = createSNSEndpoint(accountId, mobilePushRegistration);
        notificationSubscriptionsDAO.subscribe(accountId, updated);
    }


    public boolean unsubscribe(final Long accountId, final String deviceToken) {

        final Optional<MobilePushRegistration> optional = this.getSubscription(accountId, deviceToken);
        if(!optional.isPresent()) {
            LOGGER.debug("Did not find subscription with token = {}", deviceToken);
            return false;
        }

        final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest();
        deleteEndpointRequest.withEndpointArn(optional.get().endpoint.get());
        amazonSNSClient.deleteEndpoint(deleteEndpointRequest);

        final Integer numRowsAffected = notificationSubscriptionsDAO.unsubscribe(accountId, deviceToken);
        return true;
    }

    private boolean deleteFromSNS(final List<MobilePushRegistration> registrations) {
        for(final MobilePushRegistration registration : registrations) {
            final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest();
            deleteEndpointRequest.withEndpointArn(registration.endpoint.get());

            amazonSNSClient.deleteEndpoint(deleteEndpointRequest);
        }
        return true;
    }
    public boolean unsubscribe(String accessToken) {
        notificationSubscriptionsDAO.unsubscribe(accessToken);
        return true;
    }


//    @Override
//    public boolean unsubscribe(final AccessToken accessToken) {
//
//        final List<MobilePushRegistration> registrations = this.getSubscriptions(accessToken.accountId);
//
//        for(final MobilePushRegistration registration : registrations) {
//            if(registration.oauthToken.equals(accessToken.serializeAccessToken())) {
//                unsubscribe(accessToken.accountId, registration.deviceToken);
//                return true;
//            }
//        }
//
//        return false;
//    }


    /**
     * Creates a specific endpoint for this user + MobilePushRegistration
     *
     * @param accountId
     * @param mobilePushRegistration
     */
    private MobilePushRegistration createSNSEndpoint(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();

        request.setCustomUserData(accountId.toString());
        request.withToken(mobilePushRegistration.deviceToken); //custom per user
        request.setPlatformApplicationArn(arns.get(mobilePushRegistration.os));

        // TODO: catch exceptions when creating endpoint fails
        final CreatePlatformEndpointResult result = amazonSNSClient.createPlatformEndpoint(request);
        final MobilePushRegistration m = MobilePushRegistration.withEndpointForAccount(
                mobilePushRegistration,
                result.getEndpointArn(),
                accountId
        );

        return m;
    }
}
