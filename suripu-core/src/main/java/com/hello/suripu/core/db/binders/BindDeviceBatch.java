package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.db.util.SqlArray;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindDeviceBatch.BindDeviceBatchFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindDeviceBatch{
    public static class BindDeviceBatchFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindDeviceBatch, DeviceBatch>() {
                public void bind(final SQLStatement q, final BindDeviceBatch bind, final DeviceBatch batch) {

                    q.bind("account_id", batch.accountId);
                    q.bind("ambient_temp", new SqlArray<Integer>(Integer.class, batch.ambientTemp));
                    q.bind("ambient_light", new SqlArray<Integer>(Integer.class, batch.ambientLight));
                    q.bind("ambient_humidity", new SqlArray<Integer>(Integer.class, batch.ambientHumidity));
                    q.bind("ambient_air_quality", new SqlArray<Integer>(Integer.class,batch.ambientAirQuality));

                    q.bind("ts", batch.dateTime);
                    q.bind("offset_millis", batch.offsetMillis);
                }
            };
        }
    }
}
