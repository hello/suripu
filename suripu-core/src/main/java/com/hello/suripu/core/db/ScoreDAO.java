package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Score;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;


@RegisterMapper(ScoreMapper.class)
public interface ScoreDAO {

    @SqlQuery("SELECT * FROM account_scores WHERE account_id = : account_id ORDER BY id desc LIMIT 7;")
    ImmutableList<Score> getRecentScores(@Bind("account_id") Long accountId);

    //TODO : Add sound score!
    @SqlUpdate("INSERT INTO account_scores (account_id, ambient_temp, ambient_humidity, ambient_air_quality, ambient_light, ts, offset_millis) VALUES " +
            "(:account_id, :ambient_temp, :ambient_humidity, :ambient_air_quality, :ambient_light, :ts, :offset_millis);")
    void insertScore(@BindScore Score score);


}
