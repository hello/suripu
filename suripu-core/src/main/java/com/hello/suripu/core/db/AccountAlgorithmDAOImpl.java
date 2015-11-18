package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.mappers.AccountAlgorithmMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountAlgorithm;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

/**
 *
 *
 * Created by benjo on 11/17/15.
 */

@RegisterMapper(AccountAlgorithmMapper.class)
public abstract class AccountAlgorithmDAOImpl implements AccountAlgorithmDAO {

    /* get the latest algorithm for a day (but not after)  */
    @SqlQuery("SELECT * FROM account_algorithm_map WHERE account_id = :account_id AND utc_ts <= :date ORDER BY date DESC LIMIT 1;")
    @SingleValueResult(Account.class)
    public abstract Optional<AccountAlgorithm> getLatestAlgorithmForAccount(@Bind("account_id") final Long accountId,@Bind("date") final DateTime date);

}
