package com.hello.suripu.coredw8.db.mappers;


import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AuthorizationCode;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AuthorizationCodeMapper implements ResultSetMapper<AuthorizationCode> {
    @Override
    public AuthorizationCode map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Array scopes = r.getArray("scopes");

        final Integer[] a = (Integer[]) scopes.getArray();
        final OAuthScope[] scopeArray = OAuthScope.fromIntegerArray(a);

        return new AuthorizationCode(
                UUID.fromString(r.getString("auth_code")),
                r.getLong("expires_in"),
                new DateTime(r.getTimestamp("created_at"), DateTimeZone.UTC),
                r.getLong("account_id"),
                r.getLong("app_id"),
                scopeArray
        );
    }
}
