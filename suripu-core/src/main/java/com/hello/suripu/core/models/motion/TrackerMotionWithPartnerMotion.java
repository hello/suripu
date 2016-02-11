package com.hello.suripu.core.models.motion;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.List;

/**
 * Created by jakepiccolo on 12/16/15.
 */
public class TrackerMotionWithPartnerMotion {
    public final TrackerMotion trackerMotion;
    public final List<PartnerMotionAtSecond> partnerMotionAtSeconds;

    protected TrackerMotionWithPartnerMotion(final TrackerMotion trackerMotion,
                                             final List<PartnerMotionAtSecond> partnerMotionAtSeconds)
    {
        this.trackerMotion = trackerMotion;
        this.partnerMotionAtSeconds = partnerMotionAtSeconds;
    }

}
