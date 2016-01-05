package com.hello.suripu.coredw.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountLocationDAO;
import com.hello.suripu.core.db.mappers.AccountLocationMapper;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.AccountLocation;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 1/4/16.
 */
public class AccountLocationDAOTest {

    private Handle handle;
    private AccountLocationDAO accountLocationDAO;

    @Before
    public void setUp() throws Exception
    {
        final String createQuery = "CREATE TABLE account_location(\n" +
                "id SERIAL PRIMARY KEY,\n" +
                "account_id BIGINT,\n" +
                "ip VARCHAR(20),\n" +
                "latitude DOUBLE PRECISION,\n" +
                "longitude DOUBLE PRECISION,\n" +
                "city VARCHAR(255),\n" +
                "state VARCHAR(255),\n" +
                "country_code CHAR(2) NOT NULL DEFAULT '',\n" +
                "created TIMESTAMP default current_timestamp);\n" +
                " CREATE UNIQUE INDEX uniq_account_location_created_idx ON account_location(account_id, created);";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        final DBI dbi = new DBI(ds);
        dbi.registerMapper(new AccountLocationMapper());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        handle = dbi.open();
        handle.execute(createQuery);
        accountLocationDAO = dbi.onDemand(AccountLocationDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("DROP TABLE account_location IF EXISTS");
        handle.close();
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

        accountLocationDAO.insertNewAccountLatLongIP(accountId, "66.66.66.66", 81.483083, -45.942446);
        final Optional<AccountLocation> secondOptional = accountLocationDAO.getLastLocationByAccountId(accountId);
        assertThat(secondOptional.isPresent(), is(true));
        assertThat(secondOptional.get().ip, not(ip));
        assertThat(secondOptional.get().countryCode, equalTo(""));
    }
}
