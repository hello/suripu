package com.hello.suripu.core.db;

import com.hello.suripu.core.Score;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindScore.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindScore {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindScore, Score>() {
                public void bind(SQLStatement q, BindScore bind, Score arg) {

                    q.bind("account_id", arg.accountId);
                    q.bind("ambient_temp", arg.temperature);
                    q.bind("ambient_humidity", arg.humidity);
                    q.bind("ambient_light", arg.light);
                    q.bind("ambient_air_quality", arg.airQuality);
                    q.bind("ts", arg.dateTime);
                    q.bind("offset_millis", 0); // TODO: fix this
                }
            };
        }
    }
}
