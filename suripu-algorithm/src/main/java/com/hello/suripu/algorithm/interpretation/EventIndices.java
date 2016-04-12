package com.hello.suripu.algorithm.interpretation;

import java.util.List;

/**
 * Created by benjo on 4/11/16.
 */
public class EventIndices {
    public final int iInBed;
    public final int iSleep;
    public final int iWake;
    public final int iOutOfBed;

    final List<IdxPair> skippedOverWakePeriods;

    public EventIndices(int iInBed, int iSleep, int iWake, int iOutOfBed,final List<IdxPair> skippedOverWakePeriods) {
        this.iInBed = iInBed;
        this.iSleep = iSleep;
        this.iWake = iWake;
        this.iOutOfBed = iOutOfBed;
        this.skippedOverWakePeriods = skippedOverWakePeriods;
    }
}