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

    //update timeline_feedback set new_time='07:15' where account_id=1012 and date_of$

    /* update if the passed object's old_time,account_id,night,type == database object's new_time,account_id,night,type
    *  So, basically, if passed object's old_time matches the DB entry's new_time, replace the DB entry's new_time with the passed object's new_time
    * */
    @SqlUpdate("UPDATE timeline_feedback SET new_time=:new_time,created=now() WHERE account_id=:account_id AND date_of_night=:date_of_night AND event_type=:event_type AND new_time=:old_time")
    abstract int updateExistingFeedbackByAccountAndDateAndTime(@Bind("account_id") final Long accountId,@BindTimelineFeedback final TimelineFeedback timelineFeedback);

    /* if old times match, update the new time.   */
    @SqlUpdate("UPDATE timeline_feedback SET new_time=:new_time,created=now() WHERE account_id=:account_id AND date_of_night=:date_of_night AND event_type=:event_type AND old_time=:old_time")
    public abstract int updateDuplicate(@Bind("account_id") final Long accountId,@BindTimelineFeedback final TimelineFeedback timelineFeedback);

    @SqlUpdate("INSERT INTO timeline_feedback (account_id, date_of_night, old_time, new_time, event_type, created) VALUES(:account_id, :date_of_night, :old_time, :new_time, :event_type, now())")
    public abstract void insertNewTimelineFeedback(@Bind("account_id") final Long accountId, @BindTimelineFeedback final TimelineFeedback timelineFeedback);

    public void insert(SleepFeedback feedback) {
        if(feedback.accountId.isPresent()) {
            insert(feedback.accountId.get(), feedback.day, feedback.hour, feedback.correct);
        }

    }

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

    @SqlQuery("SELECT * FROM timeline_feedback WHERE account_id = :account_id AND date_of_night = :date_of_night order by created")
    public abstract ImmutableList<TimelineFeedback> getForNight(@Bind("account_id") final Long accountId, @Bind("date_of_night") final DateTime dateOfNight);

    @SqlQuery("SELECT * FROM timeline_feedback WHERE date_of_night >= :tstartUTC and date_of_night < :tstopUTC")
    public abstract ImmutableList<TimelineFeedback> getForTimeRange(@Bind("start_time") final Long tstartUTC, @Bind("stop_time") final Long tstopUTC);


}
