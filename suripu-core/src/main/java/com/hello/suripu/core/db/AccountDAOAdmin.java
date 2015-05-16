package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.AccountCountMapper;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountCount;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

public interface AccountDAOAdmin {

    @RegisterMapper(AccountCountMapper.class)
    @SqlQuery("SELECT date_trunc('day', created) AS created_date, COUNT(*) FROM accounts GROUP BY created_date ORDER BY created_date DESC;")
    List<AccountCount> countByDate();

    @RegisterMapper(AccountMapper.class)
    @SqlQuery("SELECT * FROM accounts WHERE id < :max_id ORDER BY id DESC LIMIT :limit;")
    List<Account> getRecentBeforeId(@Bind("limit") final Integer limit, @Bind("max_id") final Integer maxId);
}
