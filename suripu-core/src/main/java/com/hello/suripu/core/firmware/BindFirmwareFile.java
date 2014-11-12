package com.hello.suripu.core.firmware;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@BindingAnnotation(BindFirmwareFile.BindDeviceDataFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BindFirmwareFile {
    public static class BindDeviceDataFactory implements BinderFactory {
        public Binder build(Annotation annotation) {
            return new Binder<BindFirmwareFile, FirmwareFile>() {
                public void bind(final SQLStatement q, final BindFirmwareFile bind, final FirmwareFile model) {

                    q.bind("s3_bucket", model.s3Bucket);
                    q.bind("s3_key", model.s3Key);
                    q.bind("copy_to_serial_flash", model.copyToSerialFlash);
                    q.bind("reset_network_processor", model.resetNetworkProcessor);
                    q.bind("reset_application_processor", model.resetApplicationProcessor);
                    q.bind("serial_flash_filename", model.serialFlashFilename);
                    q.bind("serial_flash_path", model.serialFlashPath);
                    q.bind("sd_card_filename", model.sdCardFilename);
                    q.bind("sd_card_path", model.sdCardPath);
                    q.bind("sha1", model.sha1);
                }
            };
        }
    }
}

