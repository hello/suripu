package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 5/5/15.
 */
public class BaseReourceTestHelper {
    public static void stubKeyFromKeyStore(final KeyStore keyStore, final String deviceId, final Optional<byte[]> returnValue){
        when(keyStore.get(deviceId)).thenReturn(returnValue);
    }

    public static void stubGetHeader(final HttpServletRequest httpServletRequest, final String header, final String returnValue){
        when(httpServletRequest.getHeader(header)).thenReturn(returnValue);
    }

    public static void stubInject(final ObjectGraphRoot objectGraphRoot, final Class targetClass){
        when(objectGraphRoot.inject(targetClass)).thenReturn(Mockito.<Class>mock(targetClass));
    }

    public static void kinesisLoggerFactoryStubGet(final KinesisLoggerFactory kinesisLoggerFactory,
                                                   final QueueName queueName, final DataLogger returnValueForGet){
        when(kinesisLoggerFactory.get(queueName)).thenReturn(returnValueForGet);
    }
}
