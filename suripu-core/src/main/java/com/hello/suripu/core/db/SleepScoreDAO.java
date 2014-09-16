package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.SleepScoreMapper;
import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

@RegisterMapper(SleepScoreMapper.class)
public interface SleepScoreDAO  {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO sleep_score " +
            "(account_id, date_hour_utc, pill_id, offset_millis, sleep_duration, custom, " +
            "total_hour_score, sax_symbols, agitation_num, agitation_tot, updated) " +
            "VALUES(:account_id, :date_hour_utc, :pill_id, :offset_millis, :sleep_duration, :custom, " +
            ":total_hour_score, :sax_symbols, :agitation_num, :agitation_tot, :updated)")
    long insert(@Bind("account_id") long accountId,
                @Bind("pill_id") long pillID,
                @Bind("date_hour_utc") DateTime dateHourUTC,
                @Bind("offset_millis") int timeZoneOffset,
                @Bind("sleep_duration") int sleepDuration,
                @Bind("custom") boolean custom,
                @Bind("total_hour_score") int totalHourScore,
                @Bind("sax_symbols") String saxSymbols,
                @Bind("agitation_num") int agitationNum,
                @Bind("agitation_tot") long agitationTot,
                @Bind("updated") DateTime updated

    );


    @SqlQuery("SELECT * FROM sleep_score " +
            "WHERE account_id = :account_id AND date_hour_utc = :date_hour_utc AND offset_millis = :offset_millis LIMIT 1;")
    @SingleValueResult(SleepScore.class)
    Optional<SleepScore> getByAccountAndDateHour(@Bind("account_id") Long account_id,
                                             @Bind("date_hour_utc") DateTime dateHourUTC,
                                             @Bind("offset_millis") int timeZoneOffset);

    @SqlQuery("SELECT * FROM sleep_score " +
            "WHERE account_id = :account_id AND " +
            "date_hour_utc >= :sleep_utc AND date_hour_utc < :awake_utc")
    ImmutableList<SleepScore> getByAccountBetweenDateHour(@Bind("account_id") Long account_id,
                                                 @Bind("sleep_utc") DateTime sleepUTC,
                                                 @Bind("awake_utc") DateTime awakeUTC);

    @SqlUpdate("UPDATE sleep_score SET total_hour_score = :total_hour_score, " +
            "sleep_duration = :sleep_duration, " +
            "agitation_num = :agitation_num, " +
            "agitation_tot = :agitation_tot " +
            "WHERE id = :id")
    long updateSleepScoreById(@Bind("id") Long sleepLabelId,
                              @Bind("total_hour_score") int totalHourScore,
                              @Bind("sleep_duration") int sleepDuration,
                              @Bind("agitation_num") int agitationNum,
                              @Bind("agitation_tot") long agitationTot);

    @SqlUpdate("UPDATE sleep_score " +
            "SET total_hour_score = :total_hour_score " +
            "sleep_duration = :sleep_duration, " +
            "agitation_num = :agitation_num, " +
            "agitation_tot = :agitation_tot " +
            "WHERE pill_id = :pill_id AND date_hour_utc = :date_hour_utc")
    long updateSleepScoreByDateHour(@Bind("pill_id") long pillID,
                              @Bind("total_hour_score") int totalHourScore,
                              @Bind("sleep_duration") int sleepDuration,
                              @Bind("agitation_num") int agitationNum,
                              @Bind("agitation_tot") long agitationTot,
                              @Bind("date_hour_utc") DateTime dateHourUTC);

    @SqlUpdate("UPDATE sleep_score SET " +
            "total_hour_score = total_hour_score + :total_hour_score, " +
            "sleep_duration = sleep_duration + :sleep_duration, " +
            "agitation_num = agitation_num + :agitation_num, " +
            "agitation_tot = agitation_tot + :agitation_tot, " +
            "updated = :updated " +
            "WHERE pill_id = :pill_id AND date_hour_utc = :date_hour_utc")
    long incrementSleepScoreByPillDateHour(
            @Bind("pill_id") Long pillID,
            @Bind("date_hour_utc") DateTime dateHourUTC,
            @Bind("total_hour_score") int totalHourScore,
            @Bind("sleep_duration") int sleepDuration,
            @Bind("agitation_num") int agitationNum,
            @Bind("agitation_tot") long agitationTot,
            @Bind("updated") DateTime updated);


}
