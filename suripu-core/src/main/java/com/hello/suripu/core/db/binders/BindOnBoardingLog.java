package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.OnBoardingLog;
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

/**
 * Created by pangwu on 5/7/15.
 */
@BindingAnnotation(BindDeviceData.BindDeviceDataFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindOnBoardingLog {
    public static class BindOnBoardingLogFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindOnBoardingLog, OnBoardingLog>() {
                public void bind(final SQLStatement q, final BindOnBoardingLog bind, final OnBoardingLog arg) {
                    q.bind("account_id", arg.accountIdOptional.isPresent() ? arg.accountIdOptional.get() : null);
                    q.bind("sense_id", arg.senseId);
                    q.bind("pill_id", arg.pillIdOptional.isPresent() ? arg.pillIdOptional.get() : null);
                    q.bind("utc_ts", new DateTime(arg.timestampMillis, DateTimeZone.UTC));
                    q.bind("info", arg.info);
                    q.bind("result", arg.result.toString());
                    q.bind("operation", arg.pairAction.toString());
                    q.bind("ip", arg.ip);
                }
            };
        }
    }
}
