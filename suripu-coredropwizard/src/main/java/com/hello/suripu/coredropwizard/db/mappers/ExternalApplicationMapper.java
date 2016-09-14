package com.hello.suripu.coredropwizard.db.mappers;

import com.hello.suripu.core.oauth.GrantType;
import com.hello.suripu.coredropwizard.oauth.ExternalApplication;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExternalApplicationMapper implements ResultSetMapper<ExternalApplication>{
    @Override
    public ExternalApplication map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new ExternalApplication(
                r.getLong("id"),
                r.getString("name"),
                r.getString("client_id"),
                r.getString("client_secret"),
                r.getString("auth_uri"),
                r.getString("token_uri"),
                r.getString("description"),
                new DateTime(r.getTimestamp("created")),
                GrantType.values()[r.getInt("grant_type")]
        );
    }
}
