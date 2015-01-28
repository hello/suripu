package com.hello.suripu.core.notifications;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationSubscriptionsDAOTest {

    private DBI dbi;
    private Handle handle;
    private NotificationSubscriptionsDAO dao;

    @Before
    public void setUp() throws Exception
    {
        final String createTableQuery = "CREATE TABLE notifications_subscriptions (id SERIAL, account_id BIGINT, os VARCHAR(10), version VARCHAR(10), app_version VARCHAR(10), device_token VARCHAR, endpoint VARCHAR, oauth_token VARCHAR, created_at_utc TIMESTAMP);";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new AccountMapper());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        dbi.registerContainerFactory(new ImmutableListContainerFactory());
        handle = dbi.open();

        handle.execute(createTableQuery);

        final String index = "create unique index uniq_device on notifications_subscriptions (oauth_token);";
        handle.execute(index);
        dao = dbi.onDemand(NotificationSubscriptionsDAO.class);
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table notifications_subscriptions;");
        handle.close();
    }

    @Test
    public void testgetSubscriptions() {
        final ImmutableList<MobilePushRegistration> registrations = dao.getSubscriptions(1L);
        assertThat(registrations.isEmpty(), is(true));
    }

    @Test
    public void testRegisterAndGetSubscriptions() {
        final Long accountId = 123L;
        final MobilePushRegistration registration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "123456789", "987654321", "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        dao.subscribe(accountId, registration);
        final ImmutableList<MobilePushRegistration> registrations = dao.getSubscriptions(accountId);
        assertThat(registrations.isEmpty(), is(false));
        assertThat(registrations.size(), is(1));
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testRegisterAndGetMultipleIdenticalSubscriptions() {
        final Long accountId = 123L;
        final MobilePushRegistration registration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "123456789", "987654321", "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        dao.subscribe(accountId, registration);
        dao.subscribe(accountId, registration);
    }


    @Test
    public void testRegisterAndGetMultipleSubscriptions() {
        final Long accountId = 123L;
        final String oauthToken = "XXXX";
        final String secondOauthToken = "YYYY";
        final MobilePushRegistration registration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "123456789", oauthToken, "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        final MobilePushRegistration anotherRegistration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "123456789", secondOauthToken, "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        dao.subscribe(accountId, registration);
        dao.subscribe(accountId, anotherRegistration);
        final ImmutableList<MobilePushRegistration> registrations = dao.getSubscriptions(accountId);
        assertThat(registrations.isEmpty(), is(false));
        assertThat(registrations.size(), is(2));
    }

    @Test
    public void testRegisterAndGetSubscription() {
        final String deviceToken = "123456789";
        final Long accountId = 123L;
        final MobilePushRegistration registration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", deviceToken, "987654321", "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        dao.subscribe(accountId, registration);
        final Optional<MobilePushRegistration> registrationOptional = dao.getSubscription(accountId, deviceToken);
        assertThat(registrationOptional.isPresent(), is(true));
    }
}
