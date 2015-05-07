package com.hello.suripu.service.utils;

import com.hello.suripu.core.logging.DataLogger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 5/5/15.
 */
public class DataLoggerTestHelper {
    public static DataLogger mockDataLogger(){
        final DataLogger logger = mock(DataLogger.class);
        return logger;
    }

    public static DataLogger stubPutAnyException(final DataLogger logger, final String deviceId, final Exception exception){
        when(logger.put(eq(deviceId), (byte[])any())).thenThrow(exception);
        return logger;
    }

    public static DataLogger stubPut(final DataLogger logger, final String deviceId, final byte[] content, final String returnValue){
        when(logger.put(deviceId, content)).thenReturn(returnValue);
        return logger;
    }
}
