package com.hello.suripu.core.models;

/**
 * Created by pangwu on 11/22/14.
 */
public class MotionEvent extends Event {
    public final double amplitude;
    public final double maxAmplitude;

    public MotionEvent(final long startTimestamp,
                       final long endTimestamp,
                       final int offsetMillis,
                       final double amplitude,
                       final double maxAmplitude){
        super(Type.MOTION, startTimestamp, endTimestamp, offsetMillis);
        this.amplitude = amplitude;
        this.maxAmplitude = maxAmplitude;
    }

    @Override
    public String getDescription(){
        return "Motion detected";
    }

}
