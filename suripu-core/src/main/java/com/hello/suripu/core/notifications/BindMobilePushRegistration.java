package com.hello.suripu.core.notifications;

import com.hello.suripu.core.models.MobilePushRegistration;
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

@BindingAnnotation(BindMobilePushRegistration.BindAccountFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindMobilePushRegistration{

    public static class BindAccountFactory implements BinderFactory {
        public Binder build(Annotation annotation) {

            final Long now = DateTime.now(DateTimeZone.UTC).getMillis();

            return new Binder<BindMobilePushRegistration, MobilePushRegistration>() {
                public void bind(SQLStatement q, BindMobilePushRegistration bind, MobilePushRegistration arg) {
                    q.bind("account_id", arg.accountId.get());
                    q.bind("os", arg.os);
                    q.bind("version", arg.version);
                    q.bind("app_version", arg.appVersion);
                    q.bind("device_token", arg.deviceToken);
                    q.bind("oauth_token", arg.oauthToken.get());
                    q.bind("endpoint", arg.endpoint.get());
                }
            };
        }
    }
}

