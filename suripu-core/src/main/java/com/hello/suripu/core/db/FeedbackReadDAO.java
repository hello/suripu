package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.TimelineFeedbackMapper;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(TimelineFeedbackMapper.class)
public abstract class FeedbackReadDAO {
    @SqlQuery("SELECT * FROM timeline_feedback WHERE account_id = :account_id AND date_of_night = :date_of_night AND is_correct order by created")
    public abstract ImmutableList<TimelineFeedback> getCorrectedForNight(@Bind("account_id") final Long accountId, @Bind("date_of_night") final DateTime dateOfNight);

    @SqlQuery("SELECT * FROM timeline_feedback WHERE date_of_night >= :tstartUTC and date_of_night < :tstopUTC")
    public abstract ImmutableList<TimelineFeedback> getForTimeRange(@Bind("start_time") final Long tstartUTC, @Bind("stop_time") final Long tstopUTC);
}
