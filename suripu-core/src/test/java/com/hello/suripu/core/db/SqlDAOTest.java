package com.hello.suripu.core.db;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.UUID;

/**
 * Created by jakepiccolo on 3/11/16.
 */
public abstract class SqlDAOTest<T> {
    protected DBI dbi;
    protected Handle handle;
    protected T dao;

    protected abstract Class<T> tClass();
    protected abstract String setupQuery();
    protected abstract String tearDownQuery();

    @Before
    public void setUp() throws Exception {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute(setupQuery());
        dao = dbi.onDemand(tClass());
    }

    @After
    public void tearDown() throws Exception {
        handle.execute(tearDownQuery());
        handle.close();
    }
}
