package com.hello.suripu.core.models.motion;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

/**
 * Created by jakepiccolo on 12/16/15.
 */
public class PartnerMotionAtSecond {
    public final Boolean didMove;
    public final Boolean didPartnerMove;
    public final DateTime dateTime;
    public final Optional<TrackerMotion> partnerMotion;

    protected PartnerMotionAtSecond(final Boolean didMove,
                                    final Boolean didPartnerMove,
                                    final DateTime dateTime,
                                    final Optional<TrackerMotion> partnerMotion)
    {
        this.didMove = didMove;
        this.didPartnerMove = didPartnerMove;
        this.dateTime = dateTime;
        this.partnerMotion = partnerMotion;
    }
}
