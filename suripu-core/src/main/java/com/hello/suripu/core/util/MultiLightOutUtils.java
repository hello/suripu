package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/10/15.
 */
public class MultiLightOutUtils {
    public final static int DEFAULT_SMOOTH_GAP_MIN = 20;
    public final static int DEFAULT_LIGHT_DELTA_WINDOW_MIN = 15;

    private static long getEndTimestampFromLightEvent(final Event lightEvent){
        switch (lightEvent.getType()){

            case LIGHTS_OUT:
                return lightEvent.getStartTimestamp();
            default:
                return lightEvent.getEndTimestamp();
        }
    }
    public static List<Event> smoothLight(final List<Event> lightEvents, final int smoothGapMin){
        final ArrayList<Event> smoothed = new ArrayList<>();

        long lastEndTimestamp = 0;
        long lastStartTimestamp = 0;
        Event lastEvent = null;
        for(final Event lightEvent:lightEvents) {
            if(lightEvent.equals(lightEvents.get(0))) {
                lastStartTimestamp = lightEvent.getStartTimestamp();
                lastEndTimestamp = getEndTimestampFromLightEvent(lightEvent);
                lastEvent = lightEvent;
                continue;
            }
            if(lightEvent.getStartTimestamp() - lastEndTimestamp <= smoothGapMin * DateTimeConstants.MILLIS_PER_MINUTE) {
                lastEndTimestamp = getEndTimestampFromLightEvent(lightEvent);
                lastEvent = lightEvent;
                continue;
            }

            smoothed.add(Event.createFromType(lastEvent.getType(), lastStartTimestamp,
                    lastEndTimestamp,
                    lightEvent.getTimezoneOffset(),
                    Optional.fromNullable(lastEvent.getDescription()),
                    Optional.fromNullable(lastEvent.getSoundInfo()),
                    Optional.fromNullable(lastEvent.getSleepDepth())
            ));

            lastStartTimestamp = lightEvent.getStartTimestamp();
            lastEndTimestamp = getEndTimestampFromLightEvent(lightEvent);
            lastEvent = lightEvent;

        }

        if(lastEvent != null){
            lastEndTimestamp = getEndTimestampFromLightEvent(lastEvent);
            smoothed.add(Event.createFromType(lastEvent.getType(), lastStartTimestamp,
                    lastEndTimestamp,
                    lastEvent.getTimezoneOffset(),
                    Optional.<String>absent(),
                    Optional.<SleepSegment.SoundInfo>absent(),
                    Optional.<Integer>absent()
            ));
        }
        return smoothed;
    }

    public static List<Event> getValidLightOuts(final List<Event> lightEvents, final List<AmplitudeData> rawMotion, final int deltaWindowMin){
        final ArrayList<Event> multiLightOuts = new ArrayList<>();

        for(final Event lightEvent:lightEvents) {
            final long windowStartTimestamp = lightEvent.getEndTimestamp() - deltaWindowMin * DateTimeConstants.MILLIS_PER_MINUTE;
            final long windowEndTimestamp = lightEvent.getEndTimestamp() + deltaWindowMin * DateTimeConstants.MILLIS_PER_MINUTE;
            int motionCount = 0;
            for(final AmplitudeData motion:rawMotion) {
                final long timestamp = motion.timestamp;
                if(timestamp < windowStartTimestamp || timestamp > windowEndTimestamp){
                    continue;
                }

                motionCount++;

            }
            if(motionCount > 5) {
                multiLightOuts.add(lightEvent);
            }
        }

        return multiLightOuts;
    }
}
