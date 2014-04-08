package com.hello.suripu.core.db;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AccessTokenMapper implements ResultSetMapper<AccessToken> {
    @Override
    public AccessToken map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Array scopes = r.getArray("scopes");
        // TODO: Scopes is nullable, handle failure cases
        final Integer[] a = (Integer[]) scopes.getArray();
        final OAuthScope[] s = new OAuthScope[a.length];
        for(int i = 0; i < a.length; i ++) {
            s[i] = OAuthScope.values()[a[i]];
        }

        return new AccessToken(
                UUID.fromString(r.getString("access_token")),
                UUID.fromString(r.getString("refresh_token")),
                r.getLong("expires_in"),
                new DateTime(r.getLong("created_at"), DateTimeZone.UTC),
                r.getLong("account_id"),
                r.getLong("app_id"),
                s

        );
    }
}
