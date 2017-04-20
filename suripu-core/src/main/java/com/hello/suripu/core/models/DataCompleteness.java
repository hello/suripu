package com.hello.suripu.core.models;

/**
 * Created by jakepiccolo on 12/14/15.
 */
public enum DataCompleteness {
    NO_DATA(0),
    NOT_ENOUGH_DATA(1),
    ENOUGH_DATA(2);
    private int value;

    private DataCompleteness(int value){
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }
}
