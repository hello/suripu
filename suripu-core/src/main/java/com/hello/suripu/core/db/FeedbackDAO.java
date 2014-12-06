package com.hello.suripu.core.db;

import com.hello.suripu.core.models.SleepFeedback;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public abstract class FeedbackDAO {

    @SqlUpdate("INSERT INTO sleep_feedback (account_id, day, hour, correct) VALUES(:account_id, :day, :hour, :correct)")
    abstract void insert(@Bind("account_id") final Long accountId, @Bind("day") final String day, @Bind("hour") String hour, @Bind("correct") final Boolean correct);


    public void insert(SleepFeedback feedback) {
        if(feedback.accountId.isPresent()) {
            insert(feedback.accountId.get(), feedback.day, feedback.hour, feedback.correct);
        }

    }


}
