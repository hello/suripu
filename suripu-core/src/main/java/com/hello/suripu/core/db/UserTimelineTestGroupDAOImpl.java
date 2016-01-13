package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

/**
 * Created by benjo on 1/6/16.
 *
 * Schema is
 * id, account_id, utc_ts, group_id
 *
 */


public abstract class UserTimelineTestGroupDAOImpl {

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT group_id FROM user_timeline_test_group WHERE account_id = :account_id AND utc_ts < :time_to_query_utc ORDER BY utc_ts DESC limit 1;")
    public abstract Optional<Long> getUserGestGroup(@Bind("account_id") final Long accountId, @Bind("time_to_query_utc") final DateTime timeToQueryUTC);

    @SqlUpdate("INSERT INTO user_timeline_test_group (account_id,utc_ts,group_id) VALUES(:account_id,NOW(),:group_id)")
    public abstract void setUserTestGroup(@Bind("account_id") final Long accountId, @Bind("group_id") final Long groupId);

}
