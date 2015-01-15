package com.hello.suripu.core.notifications;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.oauth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryNotificationSubscriptionDAO implements NotificationSubscriptionsDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryNotificationSubscriptionDAO.class);
    private ListMultimap<Long, MobilePushRegistration> store = ArrayListMultimap.create();
    private Map<Long, MobilePushRegistration> attributes = new HashMap<>();

    @Override
    public Optional<MobilePushRegistration> getSubscription(final Long accountId, final String deviceToken) {
        if(!store.containsKey(accountId)) {
            LOGGER.debug("{} was not present in our store for account = {}", deviceToken, accountId);
            return Optional.absent();
        }
        final List<MobilePushRegistration> registrations = store.get(accountId);

        LOGGER.debug("Found {} registrations ", registrations.size());

        for(final MobilePushRegistration registration : registrations) {
            LOGGER.debug("Checking if this is the right one {} vs {}", deviceToken, registration.deviceToken);
            if (registration.deviceToken.equals(deviceToken)) {
                LOGGER.debug("Yup found it");
                return Optional.of(registration);
            }
        }

        LOGGER.debug("Nothing found");
        return Optional.absent();
    }

    @Override
    public List<MobilePushRegistration> getSubscriptions(final Long accountId) {
        if(!store.containsKey(accountId)) {
            LOGGER.debug("Didn't find any subscriptions for account {}", accountId);
            return Collections.EMPTY_LIST;
        }

        return store.get(accountId);

    }

    @Override
    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        store.put(accountId, mobilePushRegistration);
        attributes.put(accountId, mobilePushRegistration);
        LOGGER.debug("Subscribed account {} !", accountId);
    }

    @Override
    public boolean unsubscribe(Long accountId, String deviceToken) {
        store.removeAll(accountId);
        return true;
    }

    @Override
    public boolean unsubscribe(final AccessToken accessToken) {
        store.removeAll(accessToken.accountId);
        return true;
    }
}
