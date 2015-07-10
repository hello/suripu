package com.hello.suripu.coredw.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.H2IntegerArrayArgumentFactory;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessTokenDAOTest {

    private DBI dbi;
    private Handle handle;
    private AccessTokenDAO dao;

    @Before
    public void setUp() throws Exception
    {
        final String createTableQuery = "CREATE TABLE oauth_tokens(" +
                "    id BIGSERIAL PRIMARY KEY, " +
                "    access_token UUID, " +
                "    refresh_token UUID, " +
                "    expires_in INTEGER, " +
                "    created_at TIMESTAMP, " +
                "    app_id INTEGER, " +
                "    account_id BIGINT, " +
                "    scopes  ARRAY)";

        System.out.println(createTableQuery);
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new AccountMapper());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        dbi.registerArgumentFactory(new H2IntegerArrayArgumentFactory());
        handle = dbi.open();

        handle.execute(createTableQuery);

        dao = dbi.onDemand(AccessTokenDAO.class);
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table oauth_tokens");
        handle.close();
    }

    @Test
    public void testStoreToken() {
        /*
        @SingleValueResult(AccessToken.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE access_token = cast(:access_token as uuid)")
    Optional<AccessToken> getByAccessToken(@Bind("access_token") UUID accessToken);

    @SingleValueResult(AccessToken.class)
    @SqlQuery("SELECT * FROM oauth_tokens WHERE refresh_token = :refresh_token")
    Optional<AccessToken> getByRefreshToken(@Bind("refresh_token") UUID accessToken);

    @SqlUpdate("INSERT INTO oauth_tokens (access_token, refresh_token, expires_in, app_id, account_id, scopes) VALUES (:access_token, :refresh_token, :expires_in, :app_id, :account_id, :scopes);")
    void storeAccessToken(@BindAccessToken AccessToken accessToken);

    @SqlUpdate("UPDATE oauth_tokens SET expires_in=0 WHERE access_token = cast(:access_token as uuid);")
    void disable(@Bind("access_token") UUID accessToken);
         */
        final OAuthScope[] scopes = new OAuthScope[0];
        UUID token = UUID.randomUUID();
        final AccessToken accessToken = new AccessToken(token, UUID.randomUUID(), 10L, DateTime.now(), 1L, 1L, scopes);
        dao.storeAccessToken(accessToken);
        Optional<AccessToken> optional = dao.getByAccessToken(token);
        assertThat(optional.isPresent(), is(true));
    }
}
