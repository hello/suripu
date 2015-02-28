package com.hello.suripu.algorithm.sleep;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 2/27/15.
 */
public class SleepEvents<T> {
    public final T goToBed;
    public final T fallAsleep;
    public final T wakeUp;
    public final T outOfBed;

    private SleepEvents(final T goToBed, final T fallAsleep, final T wakeUp, final T outOfBed){
        this.goToBed = goToBed;
        this.fallAsleep = fallAsleep;
        this.wakeUp = wakeUp;
        this.outOfBed = outOfBed;
    }

    public static <T> SleepEvents<T> create(final T goToBed, final T fallAsleep, final T wakeUp, final T outOfBed){
        return new SleepEvents(goToBed, fallAsleep, wakeUp, outOfBed);
    }

    public List<T> toList(){
        final ArrayList<T> list = new ArrayList<>();
        list.add(this.goToBed);
        list.add(this.fallAsleep);
        list.add(this.wakeUp);
        list.add(this.outOfBed);

        return list;
    }
}
