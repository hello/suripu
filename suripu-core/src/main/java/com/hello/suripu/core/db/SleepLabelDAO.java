package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.SleepLabel;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.customizers.TransactionIsolation;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;


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
            "WHERE account_id = :account_id AND date_utc = :date_utc AND offset_millis = :offset_millis LIMIT 1;")
    @SingleValueResult(SleepLabel.class)
    Optional<SleepLabel> getByAccountAndDate(@Bind("account_id") Long account_id,
                                                         @Bind("date_utc") DateTime dateUTC,
                                                         @Bind("offset_millis") int timeZoneOffset);

    @SqlUpdate("UPDATE sleep_label " +
            "SET rating = :rating, sleep_at_utc = :sleep_at_utc, wakeup_at_utc = :wakeup_at_utc " +
            "WHERE id = :id")
    @TransactionIsolation(TransactionIsolationLevel.SERIALIZABLE)
    void updateBySleepLabelId(@Bind("id") Long sleepLabelId,
                              @Bind("rating") int rating,
                              @Bind("sleep_at_utc") DateTime sleep_at,
                              @Bind("wakeup_at_utc") DateTime wakeup_at);
}
