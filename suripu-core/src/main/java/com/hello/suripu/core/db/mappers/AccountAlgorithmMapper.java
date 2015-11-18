package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountAlgorithm;
import com.hello.suripu.core.util.AlgorithmType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by benjo on 11/17/15.
 */
public class AccountAlgorithmMapper implements ResultSetMapper<AccountAlgorithm> {
    @Override
    public AccountAlgorithm map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final long accountId = r.getLong("account_id");
        final String algString = r.getString("algorithm");
        final long date = r.getDate("date").getTime();
        final String comment = r.getString("comment");
        final AlgorithmType algorithmType = AlgorithmType.valueOf(algString);

        return new AccountAlgorithm(algorithmType,accountId,new DateTime(date).withZone(DateTimeZone.UTC));
    }
}
