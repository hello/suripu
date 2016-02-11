package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceDataMapper implements ResultSetMapper<DeviceData>{
    @Override
    public DeviceData map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        // convert light from raw counts to lux -- DVT units or later
        final DateTime dateTime = new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC);
        final int rawLight = r.getInt("ambient_light");
        int lux = rawLight;
        float fLux = (float)rawLight;

        if (dateTime.getYear() > 2014) {
            fLux = DataUtils.convertLightCountsToLux(lux);
            lux = (int)fLux;
        }

        final Integer audioPeakEnergyDB = r.getInt("audio_peak_energy_db");

        final DeviceData deviceData = new DeviceData(
                r.getLong("account_id"),
                r.getLong("device_id"),
                "",
                r.getInt("ambient_temp"),
                r.getInt("ambient_humidity"),
                r.getInt("ambient_air_quality"),
                r.getInt("ambient_air_quality_raw"),
                r.getInt("ambient_dust_variance"),
                r.getInt("ambient_dust_min"),
                r.getInt("ambient_dust_max"),
                lux,
                fLux,
                r.getInt("ambient_light_variance"),
                r.getInt("ambient_light_peakiness"),
                dateTime,
                //new DateTime(r.getTimestamp("local_utc_ts"), DateTimeZone.UTC),
                r.getInt("offset_millis"),
                r.getInt("firmware_version"),
                r.getInt("wave_count"),
                r.getInt("hold_count"),
                r.getInt("audio_num_disturbances"),
                r.getInt("audio_peak_disturbances_db"),
                r.getInt("audio_peak_background_db"),
                audioPeakEnergyDB != null ? audioPeakEnergyDB : 0
        );
        return deviceData;
    }
}
