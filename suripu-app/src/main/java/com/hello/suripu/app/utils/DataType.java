package com.hello.suripu.app.utils;

/**
 * Created by pangwu on 5/27/14.
 */
public enum DataType {
    MOTION(0),
    SOUND(1);

    private int value;
    public int getValue(){
        return this.value;
    }

    private DataType(int value){
        this.value = value;
    }

    public DataType fromInteger(int value){
        switch (value){
            case 0:
                return MOTION;
            case 1:
                return SOUND;
            default:
                return MOTION;
        }
    }
}
