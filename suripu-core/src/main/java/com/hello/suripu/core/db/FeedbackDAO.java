package com.hello.suripu.core.db;

import com.hello.suripu.core.db.binders.BindTimelineFeedback;
import com.hello.suripu.core.db.mappers.TimelineFeedbackMapper;
import com.hello.suripu.core.models.SleepFeedback;
import com.hello.suripu.core.models.TimelineFeedback;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(TimelineFeedbackMapper.class)
public abstract class FeedbackDAO extends FeedbackReadDAO {
    
    //update timeline_feedback set new_time='07:15' where account_id=1012 and date_of$

    /* update if the passed object's old_time,account_id,night,type == database object's new_time,account_id,night,type
    *  So, basically, if passed object's old_time matches the DB entry's new_time, replace the DB entry's new_time with the passed object's new_time
    * */
    @SqlUpdate("UPDATE timeline_feedback " +
            "SET new_time=:new_time,created=now(), is_correct=:is_correct, adjustment_delta_minutes=:adjustment_delta_minutes" +
            "WHERE account_id=:account_id AND date_of_night=:date_of_night AND event_type=:event_type AND new_time=:old_time")
    abstract int updateExistingFeedbackByAccountAndDateAndTime(@Bind("account_id") final Long accountId,@BindTimelineFeedback final TimelineFeedback timelineFeedback);

    /* if event type, date, and account id match, update it   */
    @SqlUpdate("UPDATE timeline_feedback " +
            "SET new_time=:new_time, old_time=:old_time, is_correct=:is_correct, created=now(), adjustment_delta_minutes=:adjustment_delta_minutes" +
            "WHERE account_id=:account_id AND date_of_night=:date_of_night AND event_type=:event_type")
    public abstract int updateDuplicate(@Bind("account_id") final Long accountId,@BindTimelineFeedback final TimelineFeedback timelineFeedback);

    @SqlUpdate("INSERT INTO timeline_feedback (account_id, date_of_night, old_time, new_time, event_type, created, is_correct,adjustment_delta_minutes) VALUES(:account_id, :date_of_night, :old_time, :new_time, :event_type, now(), :is_correct,:adjustment_delta_minutes)")
    public abstract void insertNewTimelineFeedback(@Bind("account_id") final Long accountId, @BindTimelineFeedback final TimelineFeedback timelineFeedback);

    /* Update new_time=old_time by Row ID   */
    @SqlUpdate("UPDATE timeline_feedback SET new_time=old_time, adjustment_delta_minutes=0 WHERE id=:id")
    public abstract int undoFeedbackUpdate(@Bind("id") final Long id);

    public void insertTimelineFeedback(final long accountId,final TimelineFeedback feedback) {

        /*  if old times match up (like two items sent at the same time) just update it and return  */
        final int numDuplicateUpdates = updateDuplicate(accountId, feedback);

        if (numDuplicateUpdates > 0) {
            return;
        }

        //attempt to update existing
        final int numUpdateRow = updateExistingFeedbackByAccountAndDateAndTime(accountId, feedback);

        if (numUpdateRow > 0) {
            return;
        }

        //if nothing was updated, than insert this
        insertNewTimelineFeedback(accountId, feedback);


    }

}
