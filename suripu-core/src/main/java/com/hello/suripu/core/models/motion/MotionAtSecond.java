package com.hello.suripu.core.models.motion;

import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

/**
 * Created by jakepiccolo on 12/15/15.
 */
public class MotionAtSecond {
    public final TrackerMotion trackerMotion;
    public final DateTime dateTime;
    public final Boolean didMove;

    public MotionAtSecond(final TrackerMotion trackerMotion, final DateTime dateTime, final Boolean didMove) {
        this.trackerMotion = trackerMotion;
        this.dateTime = dateTime;
        this.didMove = didMove;
    }
}
