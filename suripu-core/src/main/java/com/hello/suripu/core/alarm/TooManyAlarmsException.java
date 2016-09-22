package com.hello.suripu.core.alarm;

/**
 * Created by pangwu on 3/11/15.
 */
public class TooManyAlarmsException extends RuntimeException {
    public TooManyAlarmsException(final String error){
        super(error);
    }
}
