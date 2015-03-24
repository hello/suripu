package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTimelineFeedback;
import com.hello.suripu.core.db.mappers.TimelineFeedbackMapper;
import com.hello.suripu.core.models.SleepFeedback;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(TimelineFeedbackMapper.class)
public abstract class FeedbackDAO {

    @SqlUpdate("INSERT INTO sleep_feedback (account_id, day, hour, correct) VALUES(:account_id, :day, :hour, :correct)")
    abstract void insert(@Bind("account_id") final Long accountId, @Bind("day") final String day, @Bind("hour") String hour, @Bind("correct") final Boolean correct);

    @SqlUpdate("INSERT INTO timeline_feedback (account_id, date_of_night, old_time, new_time, event_type, created) VALUES(:account_id, :date_of_night, :old_time, :new_time, :event_type, now())")
    public abstract void insertTimelineFeedback(@Bind("account_id") final Long accountId, @BindTimelineFeedback final TimelineFeedback timelineFeedback);

    public void insert(SleepFeedback feedback) {
        if(feedback.accountId.isPresent()) {
            insert(feedback.accountId.get(), feedback.day, feedback.hour, feedback.correct);
        }

    }

    @SqlQuery("SELECT * FROM timeline_feedback where account_id = :account_id AND date_of_night = :date_of_night")
    public abstract ImmutableList<TimelineFeedback> getForNight(@Bind("account_id") final Long accountId, @Bind("date_of_night") final DateTime dateOfNight);

    @SqlQuery("SELECT * FROM timeline_feedback where account_id = :account_id")
    public abstract ImmutableList<TimelineFeedback> getForAccount(@Bind("account_id") final Long accountId);
}
