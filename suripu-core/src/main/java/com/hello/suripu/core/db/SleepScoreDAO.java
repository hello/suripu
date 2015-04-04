package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.SleepScoreMapper;
import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

public abstract class SleepScoreDAO {

    @RegisterMapper(SleepScoreMapper.class)
    @SqlQuery("SELECT * FROM sleep_score " +
            "WHERE account_id = :account_id AND " +
            "date_bucket_utc >= :sleep_utc AND date_bucket_utc < :awake_utc ORDER BY date_bucket_utc")
    public abstract ImmutableList<SleepScore> getByAccountBetweenDateBucket(@Bind("account_id") Long account_id,
                                                                            @Bind("sleep_utc") DateTime sleepUTC,
                                                                            @Bind("awake_utc") DateTime awakeUTC);

}
