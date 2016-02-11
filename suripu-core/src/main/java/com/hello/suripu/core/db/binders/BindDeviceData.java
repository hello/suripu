package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.DeviceData;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by pangwu on 6/26/14.
 */
@BindingAnnotation(BindDeviceData.BindDeviceDataFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindDeviceData {
    public static class BindDeviceDataFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindDeviceData, DeviceData>() {
                public void bind(final SQLStatement q, final BindDeviceData bind, final DeviceData model) {

                    q.bind("account_id", model.accountId);
                    q.bind("device_id", model.deviceId);
                    q.bind("ambient_temp", model.ambientTemperature);
                    q.bind("ambient_light", model.ambientLight);
                    q.bind("ambient_light_variance", model.ambientLightVariance);
                    q.bind("ambient_light_peakiness", model.ambientLightPeakiness);
                    q.bind("ambient_humidity", model.ambientHumidity);
                    q.bind("ambient_air_quality", model.ambientAirQuality);
                    q.bind("ambient_air_quality_raw", model.ambientAirQualityRaw);
                    q.bind("ambient_dust_variance", model.ambientDustVariance);
                    q.bind("ambient_dust_min", model.ambientDustMin);
                    q.bind("ambient_dust_max", model.ambientDustMax);
                    q.bind("ts", model.dateTimeUTC);
                    q.bind("local_utc_ts", model.localTime());
                    q.bind("offset_millis", model.offsetMillis);
                    q.bind("firmware_version", model.firmwareVersion);
                    q.bind("wave_count", model.waveCount);
                    q.bind("hold_count", model.holdCount);
                    q.bind("audio_num_disturbances", model.audioNumDisturbances);
                    q.bind("audio_peak_disturbances_db", model.audioPeakDisturbancesDB);
                    q.bind("audio_peak_background_db", model.audioPeakBackgroundDB);
                    q.bind("audio_peak_energy_db", model.audioPeakEnergyDB);
                }
            };
        }
    }
}
