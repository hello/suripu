package com.hello.suripu.core.store;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindStoreFeedback.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindStoreFeedback {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindStoreFeedback, StoreFeedback>() {
                public void bind(SQLStatement q, BindStoreFeedback bind, StoreFeedback arg) {

                    q.bind("account_id", arg.accountId);
                    q.bind("question", arg.question);
                    q.bind("response", arg.response);
                }
            };
        }
    }
}

