package com.hello.suripu.coredropwizard.db.binders;

import com.hello.suripu.coredropwizard.oauth.ExternalToken;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindExternalToken.BindExternalTokenFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindExternalToken {

    public static class BindExternalTokenFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindExternalToken, ExternalToken>() {
                public void bind(SQLStatement q, BindExternalToken bind, ExternalToken arg) {

                    q.bind("access_token", arg.accessToken);
                    q.bind("refresh_token", arg.refreshToken);
                    q.bind("access_expires_in", arg.accessExpiresIn);
                    q.bind("refresh_expires_in", arg.refreshExpiresIn);
                    q.bind("created_at", arg.createdAt);
                    q.bind("device_id", arg.deviceId);
                    q.bind("app_id", arg.appId);
                }
            };
        }
    }
}
