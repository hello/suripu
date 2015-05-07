package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.AccountCountMapper;
import com.hello.suripu.core.models.AccountCount;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

public interface AccountDAOAdmin {

    @RegisterMapper(AccountCountMapper.class)
    @SqlQuery("SELECT date_trunc('day', created) AS created_date, COUNT(*) FROM accounts GROUP BY created_date ORDER BY created_date DESC;")
    List<AccountCount> countByDate();
}
