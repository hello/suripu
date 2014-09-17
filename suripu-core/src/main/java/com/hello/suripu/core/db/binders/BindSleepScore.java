package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.SleepScore;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindSleepScore.BindSleepScoreFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindSleepScore {

    public static class BindSleepScoreFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindSleepScore, SleepScore>() {
                public void bind(final SQLStatement q, final BindSleepScore bind, final SleepScore arg) {
                    q.bind("account_id", arg.accountId);
                    q.bind("pill_id", arg.pillID);
                    q.bind("date_bucket_utc", arg.dateBucketUTC);
                    q.bind("offset_millis", arg.timeZoneOffset);
                    q.bind("sleep_duration", arg.sleepDuration);
                    q.bind("custom", arg.custom);
                    q.bind("bucket_score", arg.bucketScore);
                    q.bind("agitation_num", arg.agitationNum);
                    q.bind("agitation_tot", arg.agitationTot);
                    q.bind("updated", arg.updated);
                }
            };
        }
    }
}