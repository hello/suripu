package com.hello.suripu.core.notifications;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationSubscriptionDAOWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSubscriptionDAOWrapper.class);
    private final NotificationSubscriptionsDAO notificationSubscriptionsDAO;

    private final AmazonSNS amazonSNSClient;
    private final Map<String, String> arns;

    private NotificationSubscriptionDAOWrapper(final NotificationSubscriptionsDAO dao, final AmazonSNS amazonSNSClient, final Map<String, String> arns) {
        checkNotNull(arns, "arns can't be null");

        this.notificationSubscriptionsDAO = dao;
        this.amazonSNSClient = amazonSNSClient;
        this.arns = ImmutableMap.copyOf(arns);
    }

    /**
     * Creates and returns an instance of NotificationSubscriptionDAOWrapper
     * @param dao
     * @param amazonSNSClient
     * @param arns
     * @return
     */
    public static NotificationSubscriptionDAOWrapper create(final NotificationSubscriptionsDAO dao, final AmazonSNS amazonSNSClient, final Map<String, String> arns) {
        return new NotificationSubscriptionDAOWrapper(dao, amazonSNSClient, arns);
    }


    public NotificationSubscriptionsDAO dao() {
        return notificationSubscriptionsDAO;
    }

    public AmazonSNS sns() {
        return amazonSNSClient;
    }

    public Optional<MobilePushRegistration> getSubscription(final Long accountId, final String deviceToken) {
        return notificationSubscriptionsDAO.getSubscription(accountId, deviceToken);
    }


    /**
     * Return a list of all subscriptions for this account
     * @param accountId
     * @return
     */
    public ImmutableList<MobilePushRegistration> getSubscriptions(final Long accountId) {
        return notificationSubscriptionsDAO.getSubscriptions(accountId);
    }

    /**
     * To subscribe we need to make sure that no endpoint has been created with the same deviceToken but different metadata
     * It is VERY likely to happen if you log in /log out often.
     * @param accountId
     * @param mobilePushRegistration
     */
    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final Optional<MobilePushRegistration> previousRegistration = notificationSubscriptionsDAO.getSubscription(mobilePushRegistration.deviceToken);
        if(previousRegistration.isPresent()) {
            deleteFromSNS(Lists.newArrayList(previousRegistration.get()));
            notificationSubscriptionsDAO.deleteByDeviceToken(mobilePushRegistration.deviceToken);
        }

        final Optional<MobilePushRegistration> updated = createSNSEndpoint(accountId, mobilePushRegistration);
        if(updated.isPresent()) {
            notificationSubscriptionsDAO.subscribe(accountId, updated.get());
            return;
        }
        LOGGER.error("Did not subscribe account_id {}", accountId);
    }


    /**
     * Unsubscribe just this account for this deviceToken
     * @param accountId
     * @param deviceToken
     * @return
     */
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

    /**
     * Delete current notification subscription based on oauth token
     * @param oauthToken
     * @return
     */
    public boolean unsubscribe(final String oauthToken) {
        final Optional<MobilePushRegistration> mobilePushRegistrationOptional = notificationSubscriptionsDAO.getSubscriptionByOauthToken(oauthToken);
        if(mobilePushRegistrationOptional.isPresent()) {
            deleteFromSNS(Lists.newArrayList(mobilePushRegistrationOptional.get()));
            notificationSubscriptionsDAO.unsubscribe(mobilePushRegistrationOptional.get().accountId.get(), mobilePushRegistrationOptional.get().deviceToken);
        }

        return true;
    }

    /**
     * SNS won't let us create an endpoint with the same deviceToken if the metadata is different
     * We have to delete previous endpoints
     * @param registrations
     * @return
     */
    private boolean deleteFromSNS(final List<MobilePushRegistration> registrations) {
        for(final MobilePushRegistration registration : registrations) {
            final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest();
            deleteEndpointRequest.withEndpointArn(registration.endpoint.get());

            amazonSNSClient.deleteEndpoint(deleteEndpointRequest);
        }
        return true;
    }

    /**
     * Creates a specific endpoint for this user + MobilePushRegistration
     *
     * @param accountId
     * @param mobilePushRegistration
     */
    private Optional<MobilePushRegistration> createSNSEndpoint(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();

        request.setCustomUserData(accountId.toString());
        request.withToken(mobilePushRegistration.deviceToken); //custom per user
        request.setPlatformApplicationArn(arns.get(mobilePushRegistration.os));


        try {
            // TODO: catch exceptions when creating endpoint fails
            final CreatePlatformEndpointResult result = amazonSNSClient.createPlatformEndpoint(request);
            final MobilePushRegistration m = MobilePushRegistration.withEndpointForAccount(
                    mobilePushRegistration,
                    result.getEndpointArn(),
                    accountId
            );

            return Optional.of(m);
        } catch (Exception e) {
            LOGGER.error("Failed creating SNS endpoint for account_id: {}. Reason: {}", accountId, e.getMessage());
        }

        return Optional.absent();
    }
}