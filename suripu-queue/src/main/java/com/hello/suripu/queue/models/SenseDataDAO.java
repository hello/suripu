package com.hello.suripu.queue.models;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by kingshy on 3/15/16.
 */
public interface SenseDataDAO {
    @RegisterMapper(AccountDataMapper.class)
    @SqlQuery("SELECT account_id, MAX(offset_millis) AS offset_millis, max(ts) AS ts FROM " +
            "prod_sense_data WHERE ts >= :night_date_utc AND ts <= :current_data_utc " +
            "AND offset_millis = :offset_millis " +
            "GROUP BY account_id ORDER BY account_id")
    ImmutableList<AccountData> getValidAccounts(@Bind("night_date_utc") DateTime nightDateUTC,
                                                @Bind("current_date_utc") DateTime currentDateUTC,
                                                @Bind("offset_millis") int offsetMillis);
}
