package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.Registration;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindRegistration.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindRegistration {


    public static class BindRegistrationFactory implements BinderFactory {

        public Binder build(Annotation annotation) {
            return new Binder<BindRegistration, Registration>() {
                public void bind(SQLStatement q, BindRegistration bind, Registration arg) {
                    q.bind("name", arg.name);
                    q.bind("firstname", arg.firstname);
                    q.bind("lastname", arg.lastname);
                    q.bind("email", arg.email);
                    q.bind("password", arg.password);
                    q.bind("dob", arg.DOB);
                    q.bind("height", arg.height);
                    q.bind("weight", arg.weight);
                    q.bind("tz_offset", arg.tzOffsetMillis);
                    q.bind("created", arg.created);
                }
            };
        }
    }
}
