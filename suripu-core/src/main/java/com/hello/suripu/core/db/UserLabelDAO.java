package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.SleepLabelMapper;
import com.hello.suripu.core.db.mappers.UserLabelMapper;
import com.hello.suripu.core.models.DataScience.UserLabel;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.List;


@RegisterMapper(SleepLabelMapper.class)
public interface UserLabelDAO extends Transactional<UserLabelDAO> {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO user_labels (account_id, email, label, night_date, utc_ts, duration, local_utc_ts, tz_offset, note) " +
            "VALUES (:account_id, :email, CAST(:label AS USER_LABEL_TYPE), :night_date, " +
            ":utc_ts, :duration, :local_utc_ts, :tz_offset, :note)")
    Long insertUserLabel(@Bind("account_id") long accountId,
                         @Bind("email") String email,
                         @Bind("label") String label,
                         @Bind("night_date") DateTime nightDate,
                         @Bind("utc_ts") DateTime UTCTimestamp,
                         @Bind("duration") int duration,
                         @Bind("local_utc_ts") DateTime localUTCTimestamp,
                         @Bind("tz_offset") int tzOffset,
                         @Bind("note") String note
                         );

    @SqlBatch("INSERT INTO user_labels (account_id, email, label, night_date, utc_ts, duration, local_utc_ts, tz_offset, note) " +
            "VALUES (:account_id, :email, CAST(:label AS USER_LABEL_TYPE), :night_date, " +
                    ":utc_ts, :duration, :local_utc_ts, :tz_offset, :note)")
    void batchInsertUserLabels(@Bind("account_id") List<Long> accountId,
                               @Bind("email") List<String> email,
                               @Bind("label") List<String> label,
                               @Bind("night_date") List<DateTime> nightDate,
                               @Bind("utc_ts") List<DateTime> UTCTimestamp,
                               @Bind("duration") List<Integer> duration,
                               @Bind("local_utc_ts") List<DateTime> localUTCTimestamp,
                               @Bind("tz_offset") List<Integer> tzOffset,
                               @Bind("note") List<String> notes
    );

    @RegisterMapper(UserLabelMapper.class)
    @SqlQuery("SELECT * FROM user_labels " +
              "WHERE email = :email AND night_date = :night_date")
    List<UserLabel> getUserLabelsByEmailAndNight(@Bind("email") String email,
                                                 @Bind("night_date") DateTime nightDate);
}
