package com.hello.suripu.coredw8.db.binders;

import com.hello.suripu.core.db.util.SqlArray;
import com.hello.suripu.coredw8.oauth.AccessToken;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

@BindingAnnotation(BindAccessToken.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindAccessToken {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindAccessToken, AccessToken>() {
                public void bind(SQLStatement q, BindAccessToken bind, AccessToken arg) {


                    int[] a = new int[arg.scopes.length];
                    Set<Integer> set = new HashSet<Integer>();
                    for(int i = 0; i < arg.scopes.length; i++) {
                        a[i] = (int) arg.scopes[i].getValue();
                        set.add(arg.scopes[i].getValue());
                    }

                    q.bind("access_token", arg.token);
                    q.bind("refresh_token", arg.refreshToken);
                    q.bind("expires_in", arg.expiresIn);
                    q.bind("created_at", arg.createdAt);
                    q.bind("account_id", arg.accountId);
                    q.bind("app_id", arg.appId);
                    q.bind("scopes", new SqlArray<Integer>(Integer.class, set));
                }
            };
        }
    }
}
