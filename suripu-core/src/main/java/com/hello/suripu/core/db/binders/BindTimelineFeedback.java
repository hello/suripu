package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.TimelineFeedback;
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



@BindingAnnotation(BindTimelineFeedback.BindTimelineFeedbackFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindTimelineFeedback{

    public static class BindTimelineFeedbackFactory implements BinderFactory {
        public Binder build(Annotation annotation) {

            final Long now = DateTime.now(DateTimeZone.UTC).getMillis();

            return new Binder<BindTimelineFeedback, TimelineFeedback>() {
                public void bind(SQLStatement q, BindTimelineFeedback bind, TimelineFeedback arg) {
                    q.bind("day", arg.day);
                    q.bind("event_type", arg.eventType.getValue());
                    q.bind("event_datetime", arg.eventDateTime);
                    q.bind("now", DateTime.now(DateTimeZone.UTC));
                }
            };
        }
    }
}
