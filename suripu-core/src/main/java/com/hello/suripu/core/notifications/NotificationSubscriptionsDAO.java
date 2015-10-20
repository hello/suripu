package com.hello.suripu.core.notifications;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

@RegisterMapper(MobilePushRegistrationMapper.class)
public interface NotificationSubscriptionsDAO extends NotificationSubscriptionsReadDAO {

    @SqlUpdate("INSERT INTO notifications_subscriptions (account_id, os, version, app_version, device_token, oauth_token, endpoint, created_at_utc) VALUES(:account_id, :os, :version, :app_version, :device_token, :oauth_token, :endpoint, now())")
    public void subscribe(final Long accountId, @BindMobilePushRegistration final MobilePushRegistration mobilePushRegistration);

    @SqlUpdate("DELETE FROM notifications_subscriptions WHERE account_id = :account_id AND device_token = :device_token;")
    public Integer unsubscribe(@Bind("account_id") final Long accountId, @Bind("device_token") final String deviceToken);

    @SqlUpdate("DELETE FROM notifications_subscriptions WHERE access_token = :access_token")
    public Integer unsubscribe(@Bind("access_token") final String accessToken);


    @SqlUpdate("DELETE FROM notifications_subscriptions WHERE device_token = :device_token")
    public Integer deleteByDeviceToken(@Bind("device_token") final String deviceToken);


}
