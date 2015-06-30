package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.PillClassification;
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
 * Created by pangwu on 6/23/15.
 */
@BindingAnnotation(BindPillClassification.BindPillClassificationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindPillClassification {
    public static class BindPillClassificationFactory implements BinderFactory {
        public Binder build(final Annotation annotation) {
            return new Binder<BindPillClassification, PillClassification>() {
                public void bind(final SQLStatement q, final BindPillClassification bind, final PillClassification arg) {
                    q.bind("id", arg.id);
                    q.bind("internal_pill_id", arg.internalPillId);
                    q.bind("pill_id", arg.pillId);
                    q.bind("last_24pt_window_ts", arg.last24PointWindowStartTime);
                    q.bind("last_72pt_window_ts", arg.last72PointWindowStartTime);
                    q.bind("last_update_batt", PillClassification.floatToInt(arg.lastClassificationBatteryLevel));
                    q.bind("max_24hr_diff", PillClassification.floatToInt(arg.max24HoursBatteryDelta));
                    q.bind("max_72hr_diff", PillClassification.floatToInt(arg.max72HoursBatteryDelta));
                    q.bind("class", arg.status.toInt());
                }
            };
        }
    }
}
