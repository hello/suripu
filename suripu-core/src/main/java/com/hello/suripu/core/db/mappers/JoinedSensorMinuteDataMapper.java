package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.JoinedSensorMinuteData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedSensorMinuteDataMapper implements ResultSetMapper<JoinedSensorMinuteData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoinedSensorMinuteDataMapper.class);

    @Override
    public JoinedSensorMinuteData map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Integer accountId = r.getInt("account_id");
        final Long ts = new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC).getMillis();
        final Integer ambientLight = r.getInt("ambient_light");
        final Integer audioPeakDisturbancesDb = r.getInt("audio_peak_disturbances_db");
        final Integer audioNumDisturbances = r.getInt("audio_num_disturbances");
        final Integer svmNoGravity = r.getInt("svm_no_gravity");
        final Integer kickoffCounts = r.getInt("kickoff_counts");

        return new JoinedSensorMinuteData(accountId, ts, ambientLight, audioPeakDisturbancesDb, audioNumDisturbances, svmNoGravity, kickoffCounts);
    }
}
