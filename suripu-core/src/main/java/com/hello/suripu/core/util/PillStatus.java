package com.hello.suripu.core.util;

/**
 * Created by pangwu on 6/23/15.
 */
public enum PillStatus {
    UNKNOWN,
    BAD,
    NORMAL;

    public static PillStatus fromInt(final int value) {
        switch (value){
            case 1:
                return BAD;
            case 0:
                return NORMAL;
            default:
                return UNKNOWN;
        }
    }

    public int toInt(){
        switch (this){
            case BAD:
                return 1;
            case NORMAL:
                return 0;
            default:
                return -1;
        }
    }
}
