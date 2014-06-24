package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.BatchSensorData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BatchSensorDataMapper implements ResultSetMapper<BatchSensorData> {
    @Override
    public BatchSensorData map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Long accountId = r.getLong("account_id");
        final DateTime dateTime = new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC);
        final Integer offsetMillis = r.getInt("offset_millis");
        final Array temp = r.getArray("ambient_temp");
        final List<Integer> ambientTemp = fromArray(temp);

        final Array light = r.getArray("ambient_light");
        final List<Integer> ambientLight = fromArray(light);

        final Array humidity = r.getArray("ambient_humidity");
        final List<Integer> ambientHumidity = fromArray(humidity);

        final Array airQuality = r.getArray("ambient_air_quality");
        final List<Integer> ambientAirQuality = fromArray(temp);

        final String deviceId = r.getString("device_id");

        final BatchSensorData batchSensorData = new BatchSensorData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withDateTime(dateTime)
                .withOffsetMillis(offsetMillis)
                .withAmbientTemp(ambientTemp)
                .withAmbientLight(ambientLight)
                .withAmbientHumidity(ambientHumidity)
                .withAmbientAirQuality(ambientAirQuality)
                .build();

        return batchSensorData;
    }


    private List<Integer> fromArray(final Array array) throws SQLException {
        final Integer[] a = (Integer[]) array.getArray();

        final List<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < a.length; i ++) {
            list.add(a[i]);
        }

        return list;
    }
}
