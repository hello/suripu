package com.hello.suripu.core.db.mappers;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OnBoardingLog;
import com.hello.suripu.core.util.PairAction;
import com.hello.suripu.core.util.PairingResults;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by pangwu on 5/7/15.
 */
public class OnBoardingLogMapper implements ResultSetMapper<OnBoardingLog> {
    @Override
    public OnBoardingLog map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
        final OnBoardingLog onBoardingLog = new OnBoardingLog(r.getString("sense_id"),
                Optional.fromNullable(r.getString("pill_id")),
                Optional.fromNullable(r.getLong("account_id")),
                r.getString("info"),
                PairingResults.valueOf(r.getString("result")),
                PairAction.valueOf(r.getString("operation")),
                r.getString("ip"),
                r.getTimestamp("utc_ts").getTime());
        return onBoardingLog;
    }
}
