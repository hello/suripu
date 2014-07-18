package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.Account;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindAccount.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindAccount {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindAccount, Account>() {
                public void bind(SQLStatement q, BindAccount bind, Account arg) {
                    q.bind("id", arg.id);
                    q.bind("name", arg.name);
                    q.bind("email", arg.email);
                    q.bind("password", arg.password);
//q.bind("age", arg.age); // TODO: add age
                    q.bind("height", arg.height);
                    q.bind("weight", arg.weight);
                    q.bind("tz", arg.tzOffsetMillis);
                }
            };
        }
    }
}
