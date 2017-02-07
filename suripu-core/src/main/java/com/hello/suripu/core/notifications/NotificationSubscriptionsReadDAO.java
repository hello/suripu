package com.hello.suripu.core.notifications;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

@RegisterMapper(MobilePushRegistrationMapper.class)
public interface NotificationSubscriptionsReadDAO {


//    @SqlQuery("SELECT * FROM notifications_subscriptions WHERE device_token = :device_token")
//    public ImmutableList<MobilePushRegistration> getSubscriptionsByDeviceToken(@Bind("device_token") final String deviceToken);

}
