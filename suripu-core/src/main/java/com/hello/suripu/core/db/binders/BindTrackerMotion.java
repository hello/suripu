package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.TrackerMotion;
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

@BindingAnnotation(BindTrackerMotion.BindTrackerMotionFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindTrackerMotion {

    public static class BindTrackerMotionFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindTrackerMotion, TrackerMotion>() {
                public void bind(final SQLStatement q, final BindTrackerMotion bind, final TrackerMotion arg) {
                    q.bind("id", arg.id);
                    q.bind("account_id", arg.accountId);
                    q.bind("tracker_id", arg.trackerId);
                    q.bind("svm_no_gravity", arg.value);
                    q.bind("ts", new DateTime(arg.timestamp, DateTimeZone.UTC));
                    q.bind("offset_millis", arg.offsetMillis);
                    q.bind("local_utc_ts", new DateTime(arg.timestamp, DateTimeZone.UTC).plusMillis(arg.offsetMillis));
                    q.bind("motion_range", arg.motionRange);
                    q.bind("kickoff_counts", arg.kickOffCounts);
                    q.bind("on_duration_seconds", arg.onDurationInSeconds);
                }
            };
        }
    }
}