package com.hello.suripu.core.models.timeline.v2;

/**
 * Created by ksg on 01/21/16
 */

public enum  SleepState {
    AWAKE,
    SOUND,
    MEDIUM,
    LIGHT;

    public static SleepState from(final Integer sleepDepth) {
        if(sleepDepth < 5) {
            return AWAKE;
        } else if(sleepDepth < 10) {
            return LIGHT;
        } else if (sleepDepth < 70) {
            return MEDIUM;
        }

        return SOUND;

    }
}
