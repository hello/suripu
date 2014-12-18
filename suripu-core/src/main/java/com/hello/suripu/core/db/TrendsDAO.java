package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DowSampleMapper;
import com.hello.suripu.core.models.Insights.DowSample;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by kingshy on 12/17/14.
 */
public interface TrendsDAO {

    @RegisterMapper(DowSampleMapper.class)
    @SqlQuery("SELECT day_of_week, CAST(score_sum AS FLOAT)/ score_count AS value " +
            "FROM sleep_score_dow WHERE account_id = :account_id AND score_count > 0 ORDER BY day_of_week")
    public abstract ImmutableList<DowSample> getSleepScoreDow (@Bind("account_id") Long accountId);

    @RegisterMapper(DowSampleMapper.class)
    @SqlQuery("SELECT day_of_week, CAST(duration_sum AS FLOAT)/ duration_count AS value " +
            "FROM sleep_duration_dow WHERE account_id = :account_id AND duration_count > 0 ORDER BY day_of_week")
    public abstract ImmutableList<DowSample> getSleepDurationDow (@Bind("account_id") Long accountId);
}
