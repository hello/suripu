package com.hello.suripu.core.sense.metadata.sql;

import com.hello.suripu.core.sense.metadata.SenseMetadata;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@BindingAnnotation(BindSenseMetadata.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindSenseMetadata {

    class Factory implements BinderFactory {
        public Binder build(Annotation annotation) {

            final DateTime now = DateTime.now(DateTimeZone.UTC);

            return new Binder<BindSenseMetadata, SenseMetadata>() {
                public void bind(SQLStatement q, BindSenseMetadata bind, SenseMetadata arg) {
                    q.bind("sense_id", arg.senseId());
                    q.bind("color", arg.color().name());
                    q.bind("hw_version", arg.hardwareVersion().value);
                    q.bind("last_updated_at", now);
                }
            };
        }
    }
}

