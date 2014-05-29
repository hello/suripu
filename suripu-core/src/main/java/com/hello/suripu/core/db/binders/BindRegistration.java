package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.Registration;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.*;

@BindingAnnotation(BindRegistration.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindRegistration {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindRegistration, Registration>() {
                public void bind(SQLStatement q, BindRegistration bind, Registration arg) {
                    q.bind("firstname", arg.firstname);
                    q.bind("lastname", arg.lastname);
                    q.bind("email", arg.email);
                    q.bind("password", arg.password);
                    q.bind("age", arg.age);
                    q.bind("height", arg.height);
                    q.bind("weight", arg.weight);
                    q.bind("tz", arg.timeZone.getID());
                    q.bind("created", arg.created);
                }
            };
        }
    }
}
