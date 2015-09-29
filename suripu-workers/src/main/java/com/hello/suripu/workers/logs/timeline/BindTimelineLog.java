package com.hello.suripu.workers.logs.timeline;

import com.hello.suripu.api.logging.LoggingProtos;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;

@BindingAnnotation(BindTimelineLog.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindTimelineLog {

    class Factory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindTimelineLog, LoggingProtos.TimelineLog>() {
                public void bind(SQLStatement q, BindTimelineLog bind, LoggingProtos.TimelineLog arg) {
                    // TODO elegantly handle optional fields
                    q.bind("account_id", arg.getAccountId());
                    q.bind("date_of_night", String.valueOf(arg.getNightOfTimeline())); // TODO replace by arg.getNightOf() when prod has this new property
                    q.bind("algorithm", arg.getAlgorithm().getNumber());
                    q.bind("error", arg.getError().getNumber());
                    if (arg.hasTimestampWhenLogGenerated()) {
                        q.bind("created_at", new Timestamp(arg.getTimestampWhenLogGenerated()));
                    } else {
                        q.bind("created_at", (Timestamp) null);
                    }
                }
            };
        }
    }
}

