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
                    q.bind("ambient_humidity", model.ambientHumidity);
                    q.bind("ambient_air_quality", model.ambientAirQuality);

                    q.bind("ts", model.dateTimeUTC);
                    q.bind("local_utc_ts", model.dateTimeUTC.plusMillis(model.offsetMillis));
                    q.bind("offset_millis", model.offsetMillis);
                }
            };
        }
    }
}
