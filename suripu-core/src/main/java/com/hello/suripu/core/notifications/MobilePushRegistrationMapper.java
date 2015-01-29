package com.hello.suripu.core.notifications;

import com.hello.suripu.core.models.MobilePushRegistration;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MobilePushRegistrationMapper implements ResultSetMapper<MobilePushRegistration> {
    @Override
    public MobilePushRegistration map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return MobilePushRegistration.create(
                r.getLong("account_id"),
                r.getString("os"),
                r.getString("version"),
                r.getString("app_version"),
                r.getString("device_token"),
                r.getString("oauth_token"),
                r.getString("endpoint")
        );
    }
}
