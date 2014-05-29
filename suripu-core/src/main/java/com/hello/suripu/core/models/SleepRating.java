package com.hello.suripu.core.models;


public enum SleepRating {
    POOR(0),
    GOOD(1),
    EXCELLENT(2);

    private int value;

    private SleepRating(int value) {
        this.value = value;
    }

    public static SleepRating fromInteger(int value){
        switch (value){
            case 0:
                return POOR;
            case 1:
                return GOOD;
            case 2:
                return EXCELLENT;
            default:
                return GOOD;
        }
    }


    public int getValue(){
        return this.value;
    }
}
