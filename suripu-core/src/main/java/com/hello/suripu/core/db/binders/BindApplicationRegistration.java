package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.db.util.SqlArray;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.*;
import java.util.HashSet;
import java.util.Set;

@BindingAnnotation(BindApplicationRegistration.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindApplicationRegistration {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindApplicationRegistration, ApplicationRegistration>() {
                public void bind(SQLStatement q, BindApplicationRegistration bind, ApplicationRegistration arg) {

                    // TODO : Make this nicer
                    int[] a = new int[arg.scopes.length];
                    Set<Integer> set = new HashSet<Integer>();
                    for(int i = 0; i < arg.scopes.length; i++) {
                        a[i] = (int) arg.scopes[i].getValue();
                        set.add(arg.scopes[i].getValue());
                    }

                    q.bind("name", arg.name);
                    q.bind("client_id", arg.clientId);
                    q.bind("client_secret", arg.clientSecret);
                    q.bind("redirect_uri", arg.redirectURI);

                    q.bind("scopes", new SqlArray<Integer>(Integer.class, set));
                    q.bind("dev_account_id", arg.developerAccountId);
                    q.bind("description", arg.description);
                    q.bind("created", arg.created);
                    q.bind("grant_type", arg.grantType.ordinal());
                }
            };
        }
    }
}
