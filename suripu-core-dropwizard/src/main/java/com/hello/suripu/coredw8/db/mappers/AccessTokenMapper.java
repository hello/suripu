package com.hello.suripu.coredw8.db.mappers;


import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.coredw8.oauth.AccessToken;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class AccessTokenMapper implements ResultSetMapper<AccessToken> {
    @Override
    public AccessToken map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Array scopes = r.getArray("scopes");

        // TODO: Scopes is nullable, handle failure cases
        final Integer[] a = (Integer[]) scopes.getArray();
        final OAuthScope[] scopeArray = OAuthScope.fromIntegerArray(a);

        return new AccessToken(
                UUID.fromString(r.getString("access_token")),
                UUID.fromString(r.getString("refresh_token")),
                r.getLong("expires_in"),
                r.getLong("refresh_expires_in"),
                new DateTime(r.getTimestamp("created_at"), DateTimeZone.UTC),
                r.getLong("account_id"),
                r.getLong("app_id"),
                scopeArray

        );
    }
}