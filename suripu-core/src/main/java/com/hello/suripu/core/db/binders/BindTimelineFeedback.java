package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.TimelineFeedback;
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


            return new Binder<BindTimelineFeedback, TimelineFeedback>() {
                public void bind(SQLStatement q, BindTimelineFeedback bind, TimelineFeedback arg) {

                    final Integer delta = arg.getDeltaInMinutes();

                    q.bind("date_of_night", arg.dateOfNight);
                    q.bind("sleep_period", arg.sleepPeriod);
                    q.bind("event_type", arg.eventType.getValue());
                    q.bind("old_time", arg.oldTimeOfEvent);
                    q.bind("new_time", arg.newTimeOfEvent);
                    q.bind("created", arg.created);
                    q.bind("is_correct", arg.isNewTimeCorrect);
                    q.bind("adjustment_delta_minutes",delta);
                }
            };
        }
    }
}
