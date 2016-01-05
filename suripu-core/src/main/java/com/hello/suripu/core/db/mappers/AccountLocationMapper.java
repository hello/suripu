package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.AccountLocation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 12/30/15.
 */
public class AccountLocationMapper implements ResultSetMapper<AccountLocation> {
    @Override
    public AccountLocation map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new AccountLocation(r.getLong("id"),
                r.getLong("account_id"),
                r.getString("ip"),
                r.getDouble("latitude"),
                r.getDouble("longitude"),
                r.getString("city"),
                r.getString("state"),
                r.getString("country_code"),
                new DateTime(r.getTimestamp("created"), DateTimeZone.UTC)
        );
    }
}
