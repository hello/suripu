package com.hello.suripu.coredw.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountLocationDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.AccountLocation;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 1/4/16.
 */
public class AccountLocationDAOIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(AccountLocationDAOIT.class);

    private DatabaseConfiguration commonDB;
    private AccountLocationDAO accountLocationDAO;

    @Before
    public void setUp() {
        this.commonDB = this.getCommonDB();
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource;
        try {
            dataSource = managedDataSourceFactory.build(commonDB);
            final DBI jdbi = new DBI(dataSource);
            jdbi.registerArgumentFactory(new OptionalArgumentFactory(commonDB.getDriverClass()));
            jdbi.registerContainerFactory(new ImmutableListContainerFactory());
            jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
            jdbi.registerContainerFactory(new OptionalContainerFactory());
            jdbi.registerArgumentFactory(new JodaArgumentFactory());

            this.accountLocationDAO = jdbi.onDemand(AccountLocationDAO.class);
        } catch (ClassNotFoundException e) {
            LOGGER.error("No driver found for database:{}", e.getMessage());
        }

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInsertNewLocation() {
        final String ip = "123.123.123.123";
        final Long accountId = 1L;
        final Double latitude = 37.762756;
        final Double longitude = -122.400250;

        final long rowId = accountLocationDAO.insertNewAccountLatLongIP(accountId, ip, latitude, longitude);
        assertThat((rowId > 0), is(true));

        final Optional<AccountLocation> optional = accountLocationDAO.getLastLocationByAccountId(accountId);
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get().ip, is(ip));
        assertThat(optional.get().latitude, is(latitude));
    }

    @Test
    public void testUpdateLocation() {
        final String ip = "10.10.10.10";
        final Long accountId = 1L;
        final Double latitude = 37.756504;
        final Double longitude = -122.424325;

        final long locationId = accountLocationDAO.insertNewAccountLatLongIP(accountId, ip, latitude, longitude);
        assertThat((locationId > 0), is(true));

        final String city = "San Francisco";
        final String state = "CA";
        final String country = "US";
        final int updated = accountLocationDAO.updateLocationNames(locationId, city, state, country);
        assertThat(updated, is(1));

        final Optional<AccountLocation> optional = accountLocationDAO.getLastLocationByAccountId(accountId);
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get().latitude, is(latitude));
        assertThat(optional.get().city.toLowerCase(), equalTo(city.toLowerCase()));
        assertThat(optional.get().countryCode.toUpperCase(), equalTo(country.toUpperCase()));

        final String newIP = "66.66.66.66";
        accountLocationDAO.insertNewAccountLatLongIP(accountId, newIP, 81.483083, -45.942446);
        final Optional<AccountLocation> secondOptional = accountLocationDAO.getLastLocationByAccountId(accountId);
        assertThat(secondOptional.isPresent(), is(true));
        assertThat(secondOptional.get().ip, not(ip));
        assertThat(secondOptional.get().ip, is(newIP));
        assertThat((secondOptional.get().countryCode == null), is(true));
    }

    private DatabaseConfiguration getCommonDB() {

        final DatabaseConfiguration commonDB = new DatabaseConfiguration();

        commonDB.setDriverClass("org.postgresql.Driver");

        commonDB.setUser("ingress_user");
        commonDB.setPassword("hello ingress user");
        commonDB.setUrl("jdbc:postgresql://localhost:5432/common");

        final Map<String, String> property = new HashMap<>();
        property.put("charSet", "UTF-8");
        commonDB.setProperties(property);

        commonDB.setMinSize(2);
        commonDB.setMaxSize(8);
        commonDB.setCheckConnectionWhileIdle(false);

        return commonDB;
    }

}
