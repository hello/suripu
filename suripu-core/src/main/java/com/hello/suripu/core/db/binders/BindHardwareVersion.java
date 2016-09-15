package com.hello.suripu.core.db.binders;

import com.hello.suripu.core.firmware.HardwareVersion;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation(BindHardwareVersion.BindRegistrationFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindHardwareVersion {

    public static class BindRegistrationFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindHardwareVersion, HardwareVersion>() {
                public void bind(SQLStatement q, BindHardwareVersion bind, HardwareVersion arg) {
                    q.bind("hardware_version", arg.value);
                }
            };
        }
    }
}
