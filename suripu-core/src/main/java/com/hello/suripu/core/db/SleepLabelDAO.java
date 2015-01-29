package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.SleepLabelMapper;
import com.hello.suripu.core.models.SleepLabel;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.customizers.TransactionIsolation;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.List;


@RegisterMapper(SleepLabelMapper.class)
public interface SleepLabelDAO extends Transactional<SleepLabelDAO> {


    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO sleep_label (account_id, date_utc, rating, sleep_at_utc, wakeup_at_utc, offset_millis) " +
            "VALUES(:account_id, :date_utc, :rating, :sleep_at_utc, :wakeup_at_utc, :offset_millis)")
    @TransactionIsolation(TransactionIsolationLevel.SERIALIZABLE)
    Long insert(@Bind("account_id") long accountId,
                     @Bind("date_utc") DateTime dateUTC,
                     @Bind("rating") int rating,
                     @Bind("sleep_at_utc") DateTime sleepTimeUTC,
                     @Bind("wakeup_at_utc") DateTime wakeUpTimeUTC,
                     @Bind("offset_millis") int timeZoneOffset
                     );


    @SqlQuery("SELECT * FROM sleep_label " +
            "WHERE account_id = :account_id AND date_utc = :date_utc AND offset_millis = :offset_millis LIMIT 1")
    @SingleValueResult(SleepLabel.class)
    Optional<SleepLabel> getByAccountAndDate(@Bind("account_id") Long account_id,
                                                         @Bind("date_utc") DateTime dateUTC,
                                                         @Bind("offset_millis") int timeZoneOffset);

    @SqlQuery("SELECT * FROM sleep_label " +
            "WHERE account_id = :account_id AND date_utc >= :start_date_utc AND date_utc < :end_date_utc ORDER BY id DESC LIMIT 1")
    @SingleValueResult(SleepLabel.class)
    Optional<SleepLabel> getByAccountAndDate(@Bind("account_id") Long account_id,
                                             @Bind("start_date_utc") DateTime startDateUTC,
                                             @Bind("end_date_utc") DateTime endDateUTC);

    @SqlQuery("SELECT * FROM sleep_label " +
            "WHERE account_id = :account_id AND date_utc >= :start_date_utc AND date_utc < :end_date_utc ORDER BY id DESC")
    ImmutableList<SleepLabel> getByAccountAndDates(@Bind("account_id") Long account_id,
                                             @Bind("start_date_utc") DateTime startDateUTC,
                                             @Bind("end_date_utc") DateTime endDateUTC);

    @SqlUpdate("UPDATE sleep_label " +
            "SET rating = :rating, sleep_at_utc = :sleep_at_utc, wakeup_at_utc = :wakeup_at_utc " +
            "WHERE id = :id")
    @TransactionIsolation(TransactionIsolationLevel.SERIALIZABLE)
    void updateBySleepLabelId(@Bind("id") Long sleepLabelId,
                              @Bind("rating") int rating,
                              @Bind("sleep_at_utc") DateTime sleep_at,
                              @Bind("wakeup_at_utc") DateTime wakeup_at);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO user_labels (account_id, email, label, night_date, utc_ts, local_utc_ts, tz_offset) " +
            "VALUES (:account_id, :email, CAST(:label AS USER_LABEL_TYPE), :night_date, " +
            ":utc_ts, :local_utc_ts, :tz_offset)")
    Long insertUserLabel(@Bind("account_id") long accountId,
                         @Bind("email") String email,
                         @Bind("label") String label,
                         @Bind("night_date") DateTime nightDate,
                         @Bind("utc_ts") DateTime UTCTimestamp,
                         @Bind("local_utc_ts") DateTime localUTCTimestamp,
                         @Bind("tz_offset") int tzOffset
                         );

    @SqlBatch("INSERT INTO user_labels (account_id, email, label, night_date, utc_ts, local_utc_ts, tz_offset) " +
            "VALUES (:account_id, :email, CAST(:label AS USER_LABEL_TYPE), :night_date, " +
                    ":utc_ts, :local_utc_ts, :tz_offset)")
    void batchInsertUserLabels(@Bind("account_id") List<Long> accountId,
                               @Bind("email") List<String> email,
                               @Bind("label") List<String> label,
                               @Bind("night_date") List<DateTime> nightDate,
                               @Bind("utc_ts") List<DateTime> UTCTimestamp,
                               @Bind("local_utc_ts") List<DateTime> localUTCTimestamp,
                               @Bind("tz_offset") List<Integer> tzOffset
    );
}
