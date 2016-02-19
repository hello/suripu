package com.hello.suripu.core.db.sleep_sounds;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.mappers.sleep_sounds.DurationMapper;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jakepiccolo on 2/18/16.
 */
@RegisterMapper(DurationMapper.class)
public abstract class DurationDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DurationDAO.class);

    @SqlQuery("SELECT * FROM sleep_sound_durations WHERE id = :id LIMIT 1;")
    @SingleValueResult(Duration.class)
    public abstract Optional<Duration> getById(@Bind("id") final Long id);

    @SqlQuery("SELECT * FROM sleep_sound_durations ORDER BY sort_key;")
    public abstract List<Duration> all();

    @SqlQuery("SELECT * FROM sleep_sound_durations WHERE duration_seconds = :duration_seconds LIMIT 1;")
    @SingleValueResult(Duration.class)
    public abstract Optional<Duration> getByDurationSeconds(@Bind("duration_seconds") final Long durationSeconds);
}
