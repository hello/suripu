package com.hello.suripu.core.db.mappers.sleep_sounds;

import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jakepiccolo on 2/18/16.
 */
public class SoundMapper implements ResultSetMapper<Sound> {
    @Override
    public Sound map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Sound.create(
                r.getLong("id"),
                r.getString("preview_url"),
                r.getString("name"),
                r.getString("file_path"),
                r.getString("url"));
    }
}
