package com.hello.suripu.core.alerts;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindAlert.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindAlert{

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindAlert, Alert>() {
                public void bind(SQLStatement q, BindAlert bind, Alert arg) {

                    q.bind("account_id", arg.accountId);
                    q.bind("created_at", arg.createdAt);
                    q.bind("title", arg.title);
                    q.bind("body", arg.body);
                }
            };
        }
    }
}

