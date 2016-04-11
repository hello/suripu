package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.GrantType;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApplicationMapper implements ResultSetMapper<Application>{
    @Override
    public Application map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Array scopes = r.getArray("scopes");

        // TODO: Scopes is nullable, handle failure cases
        final Integer[] a = (Integer[]) scopes.getArray();
        final OAuthScope[] scopeArray = OAuthScope.fromIntegerArray(a);

        return new Application(
                r.getLong("id"),
                r.getString("name"),
                r.getString("client_id"),
                r.getString("client_secret"),
                r.getString("redirect_uri"),
                scopeArray,
                r.getLong("dev_account_id"),
                r.getString("description"),
                r.getBoolean("published"),
                new DateTime(r.getTimestamp("created")),
                GrantType.values()[r.getInt("grant_type")]
        );
    }
}
