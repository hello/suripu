package com.hello.suripu.coredw.notifications;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.net.URL;
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
        final URL url = Resources.getResource("sql/notifications.sql");
        final String createTableQuery = Resources.toString(url, Charsets.UTF_8);
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new AccountMapper());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        dbi.registerContainerFactory(new ImmutableListContainerFactory());
        handle = dbi.open();

        handle.execute(createTableQuery);
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
        final MobilePushRegistration registration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "test-1", oauthToken, "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
        final MobilePushRegistration anotherRegistration = MobilePushRegistration.create(accountId, "ios", "xxx", "yyy", "test-2", secondOauthToken, "arn:aws:sns:us-east-1:053216739513:endpoint/APNS/hello-sense-ios-dev/e65fb9b1-9fd5-3555-bc81-03196726c5bc");
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
