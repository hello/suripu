package com.hello.suripu.core.util;

/**
 * Created by pangwu on 11/20/14.
 */
public class UInt32 {
    public static long getValue(final int value){
        return value & 0xFFFFFFFFL;
    }
}
