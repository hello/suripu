package com.hello.suripu.core.db;

import com.hello.suripu.core.models.SleepFeedback;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public abstract class FeedbackDAO {

    @SqlUpdate("INSERT INTO sleep_feedback (account_id, day, hour, correct) VALUES(:account_id, :day, :hour, :correct)")
    abstract void insert(final Long accountId, final String day, String hour, Boolean correct);


    public void insert(SleepFeedback feedback) {
        if(feedback.accountId.isPresent()) {
            insert(feedback.accountId.get(), feedback.day, feedback.hour, feedback.correct);
        }

    }


}
