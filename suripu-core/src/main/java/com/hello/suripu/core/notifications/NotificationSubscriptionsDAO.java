package com.hello.suripu.core.notifications;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.oauth.AccessToken;

import java.util.List;

public interface NotificationSubscriptionsDAO {

    public Optional<MobilePushRegistration> getSubscription(final Long accountId, final String deviceToken);
    public List<MobilePushRegistration> getSubscriptions(final Long accountId);
    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration);
    public boolean unsubscribe(final Long accountId, final String deviceToken);
    public boolean unsubscribe(final AccessToken accessToken);
}
