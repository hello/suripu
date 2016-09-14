package com.hello.suripu.coredropwizard.db.binders;

import com.hello.suripu.coredropwizard.oauth.ExternalAuthorizationState;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindExternalAuthorizationState.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindExternalAuthorizationState {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindExternalAuthorizationState, ExternalAuthorizationState>() {
                public void bind(SQLStatement q, BindExternalAuthorizationState bind, ExternalAuthorizationState arg) {

                    q.bind("auth_state", arg.authState);
                    q.bind("created_at", arg.createdAt);
                    q.bind("device_id", arg.deviceId);
                    q.bind("app_id", arg.appId);
                }
            };
        }
    }
}
