package com.hello.suripu.coredw.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAOImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by benjo on 1/13/16.
 */
public class UserTimelineGroupDAOTest {
    private DBI dbi;
    private Handle handle;
    private UserTimelineTestGroupDAO userTimelineTestGroupDAO;

    @Before
    public void setUp() throws Exception
    {
        final String createTableQuery = "CREATE TABLE user_timeline_test_group(\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    account_id BIGINT,\n" +
                "    utc_ts TIMESTAMP,\n" +
                "    group_id BIGINT NOT NULL DEFAULT 0);\n" +
                "\n" +
                "CREATE INDEX user_test_group_account_id_idx on user_timeline_test_group(account_id);";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        handle = dbi.open();

        handle.execute(createTableQuery);

        userTimelineTestGroupDAO = dbi.onDemand(UserTimelineTestGroupDAOImpl.class);

    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table user_timeline_test_group");
        handle.close();
    }



    @Test
    public void testSimpleQuery() {

        final DateTime before = DateTime.now().minus(60000L);

        userTimelineTestGroupDAO.setUserTestGroup(1012L,42L);

        final DateTime after = DateTime.now().plus(60000L);

        TestCase.assertFalse(userTimelineTestGroupDAO.getUserGestGroup(1001L,after).isPresent());
        TestCase.assertFalse(userTimelineTestGroupDAO.getUserGestGroup(1012L,before).isPresent());
        TestCase.assertTrue(userTimelineTestGroupDAO.getUserGestGroup(1012L,after).isPresent());
        TestCase.assertEquals(userTimelineTestGroupDAO.getUserGestGroup(1012L,after).get().longValue(),42L);

    }


}
