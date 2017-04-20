package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.models.Account;
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

@BindingAnnotation(BindAccount.BindAccountFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindAccount {

    public static class BindAccountFactory implements BinderFactory {
        public Binder build(Annotation annotation) {

            final Long now = DateTime.now(DateTimeZone.UTC).getMillis();

            return new Binder<BindAccount, Account>() {
                public void bind(SQLStatement q, BindAccount bind, Account arg) {
                    q.bind("name", arg.name());
                    q.bind("firstname", arg.firstname);
                    q.bind("lastname", arg.lastname.orNull());
                    q.bind("email", arg.email);
                    q.bind("password", arg.password);
                    q.bind("gender", arg.gender.toString());
                    q.bind("gender_name", arg.genderName());
                    q.bind("dob", arg.DOB);
                    q.bind("height", arg.height);
                    q.bind("weight", arg.weight);
                    q.bind("tz_offset", arg.tzOffsetMillis);
                    q.bind("last_modified", arg.lastModified);
                    q.bind("now", now);
                }
            };
        }
    }
}
