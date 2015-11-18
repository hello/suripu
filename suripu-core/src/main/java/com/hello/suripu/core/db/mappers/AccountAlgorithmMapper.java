package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountAlgorithm;
import com.hello.suripu.core.util.AlgorithmType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by benjo on 11/17/15.
 */
public class AccountAlgorithmMapper implements ResultSetMapper<AccountAlgorithm> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAlgorithmMapper.class);

    @Override
    public AccountAlgorithm map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final long accountId = r.getLong("account_id");
        final Integer algInt = r.getInt("algorithm");
        final long date = r.getDate("utc_ts").getTime();
        final String comment = r.getString("comment");

        AlgorithmType algorithmType = AlgorithmType.NONE;

        try {
            algorithmType = AlgorithmType.values()[algInt];
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            LOGGER.error("unknown algorithm integer: {}",algInt);
        }

        return new AccountAlgorithm(algorithmType,accountId,new DateTime(date).withZone(DateTimeZone.UTC));
    }
}
