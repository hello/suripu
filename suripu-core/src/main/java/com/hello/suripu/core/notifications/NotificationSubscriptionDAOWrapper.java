package com.hello.suripu.core.notifications;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Creates a new SNS endpoint and saves the ARN if it doesn't exist, or updates the device token / other metadata
     * if they are outdated.
     *
     * Implements the pseudocode in http://docs.aws.amazon.com/sns/latest/dg/mobile-platform-endpoint.html
     */
    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        // retrieve the latest device token from the mobile operating system
        final String deviceToken = mobilePushRegistration.deviceToken;
        String endpointArn;

        // if the platform endpoint ARN is not stored
        final Optional<MobilePushRegistration> previousRegistrationOptional = notificationSubscriptionsDAO.getSubscription(accountId, deviceToken);
        if (!previousRegistrationOptional.isPresent())
        {
            if (!previousRegistrationOptional.get().endpoint.isPresent())
            {
                // It shouldn't happen that we have a row with no ARN, but just in case, delete the row so we can recreate it.
                LOGGER.error("empty_registration_endpoint account_id={} device_token={}", accountId, deviceToken);
                notificationSubscriptionsDAO.unsubscribe(accountId, deviceToken);
            }

            // This is a first-time registration
            // Call create platform endpoint, Store the returned platform ARN.
            final Optional<MobilePushRegistration> newRegistrationOptional = createAndStoreSNSEndpoint(accountId, mobilePushRegistration);
            if (newRegistrationOptional.isPresent()) {
                endpointArn = newRegistrationOptional.get().endpoint.get();
            } else {
                // Impail the fail whale on a snail tale.
                return;
            }
        }
        else
        {
            endpointArn = previousRegistrationOptional.get().endpoint.get();
        }

        // call get endpoint attributes on the platform endpoint ARN
        try {
            final GetEndpointAttributesRequest getEndpointAttributesRequest = new GetEndpointAttributesRequest()
                    .withEndpointArn(endpointArn);
            final Map<String, String> endpointAttributes = amazonSNSClient.getEndpointAttributes(getEndpointAttributesRequest).getAttributes();
            final String expectedUserData = String.valueOf(accountId);

            final String userData = endpointAttributes.get("CustomUserData");
            final boolean isCorrectAccountId = Objects.equals(expectedUserData, userData);

            final String endpointToken = endpointAttributes.get("Token");
            final boolean isCorrectDeviceToken = Objects.equals(deviceToken, endpointToken);

            final String enabledAttribute = endpointAttributes.get("Enabled");
            final boolean isEnabled = enabledAttribute != null && enabledAttribute.equalsIgnoreCase("true");

            // if the device token in the endpoint does not match the latest one
            // or get endpoint attributes shows the endpoint as disabled
            if (!(isCorrectAccountId && isCorrectDeviceToken && isEnabled))
            {
                LOGGER.debug("account_id={} device_token={} user_data={} endpoint_token={} is_correct_account_id={} is_correct_device_token={} is_enabled={}",
                        accountId, deviceToken, userData, endpointToken, isCorrectAccountId, isCorrectDeviceToken, isEnabled);
                // call set endpoint attributes to set the latest device token and then enable the platform endpoint
                setEndpointAttributes(expectedUserData, deviceToken, previousRegistrationOptional.get().endpoint.get());
            }
        } catch (NotFoundException nfe) {
            // if while getting the attributes a not-found exception is thrown, the platform endpoint was deleted.
            // Call create platform endpoint, Store the returned platform ARN
            createAndStoreSNSEndpoint(accountId, mobilePushRegistration);
        }

    }


    private void setEndpointAttributes(final String userData, final String deviceToken, final String endpointArn) {
        final SetEndpointAttributesRequest setEndpointAttributesRequest = new SetEndpointAttributesRequest()
                .addAttributesEntry("CustomUserData", userData)
                .addAttributesEntry("Token", deviceToken)
                .addAttributesEntry("Enabled", "true")
                .withEndpointArn(endpointArn);
        amazonSNSClient.setEndpointAttributes(setEndpointAttributesRequest);
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

    private Optional<MobilePushRegistration> createAndStoreSNSEndpoint(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final Optional<MobilePushRegistration> newRegistrationOptional = createSNSEndpoint(accountId, mobilePushRegistration);

        // Store the returned platform ARN
        if(newRegistrationOptional.isPresent()) {
            notificationSubscriptionsDAO.subscribe(accountId, newRegistrationOptional.get());
            return newRegistrationOptional;
        }
        LOGGER.error("error=could_not_create_sns_endpoint account_id={} device_token={}", accountId, mobilePushRegistration.deviceToken);
        return Optional.absent();
    }
}
