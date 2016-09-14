package com.hello.suripu.coredropwizard.db.binders;

import com.hello.suripu.coredropwizard.oauth.ExternalApplicationData;

import org.postgresql.util.PGobject;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;

@BindingAnnotation(BindExternalApplicationData.JsonBinderFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindExternalApplicationData {

    public static class JsonBinderFactory implements BinderFactory {
        @Override
        public Binder build(Annotation annotation) {
            return new Binder<BindExternalApplicationData, ExternalApplicationData>() {
                @Override
                public void bind(SQLStatement q, BindExternalApplicationData bind, ExternalApplicationData arg) {
                    try {
                        PGobject data = new PGobject();
                        data.setType("jsonb");
                        data.setValue(arg.data);
                        q.bind("data", data);
                        q.bind("device_id", arg.deviceId);
                        q.bind("app_id", arg.appId);
                    } catch (SQLException ex) {
                        throw new IllegalStateException("Error Binding JSON",ex);
                    }
                }
            };
        }
    }
}

