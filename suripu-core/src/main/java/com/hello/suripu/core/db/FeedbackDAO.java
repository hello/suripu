package com.hello.suripu.core.db;

import com.hello.suripu.core.db.binders.BindTimelineFeedback;
import com.hello.suripu.core.models.SleepFeedback;
import com.hello.suripu.core.models.TimelineFeedback;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

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
}
