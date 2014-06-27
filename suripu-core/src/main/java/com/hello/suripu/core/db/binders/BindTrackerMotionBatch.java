package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.db.util.SqlArray;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 6/23/14.
 */
@BindingAnnotation(BindTrackerMotionBatch.BindTrackerMotionBatchFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindTrackerMotionBatch {

    public static class BindTrackerMotionBatchFactory implements BinderFactory {

        public Binder build(Annotation annotation) {
            return new Binder<BindTrackerMotionBatch, TrackerMotion.Batch>() {

                @Override
                public void bind(final SQLStatement sqlStatement, final BindTrackerMotionBatch bindTrackerMotionBatch, final TrackerMotion.Batch batch) {
                    sqlStatement.bind("account_id", batch.accountId);

                    final List<Integer> amplitudes = new ArrayList<Integer>();
                    for(int i = 0; i < batch.motionData.size(); i++){
                        amplitudes.add(batch.motionData.get(i).value);
                    }
                    sqlStatement.bind("amplitudes", SqlArray.<Integer>arrayOf(Integer.class, amplitudes));
                    sqlStatement.bind("ts", new DateTime(batch.firstElementTimestamp, DateTimeZone.UTC));
                    sqlStatement.bind("offset_millis", batch.offsetMillis);
                }
            };
        }
    }
}
