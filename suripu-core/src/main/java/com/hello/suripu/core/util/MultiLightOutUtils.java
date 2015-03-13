package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/10/15.
 */
public class MultiLightOutUtils {
    public final static int DEFAULT_SMOOTH_GAP_MIN = 20;
    public final static int DEFAULT_LIGHT_DELTA_WINDOW_MIN = 15;

    private final static int LIGHT_MOTION_CORRELATION_COUNT_THRESHOLD = 1;
    private final static int LATEST_LIGHT_EVENT_HOUROFDAY = 5;
    private final static int EARLIEST_LIGHT_EVENT_HOUROFDAY = 20;

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
                    Optional.fromNullable(lastEvent.getDescription()),
                    Optional.fromNullable(lastEvent.getSoundInfo()),
                    Optional.fromNullable(lastEvent.getSleepDepth())
            ));
        }
        return smoothed;
    }

    private static int getHourOfLightOut(final Event lightEvent){
        final DateTime lightOutDateTime = new DateTime(lightEvent.getEndTimestamp(),
                DateTimeZone.forOffsetMillis(lightEvent.getTimezoneOffset()));
        return lightOutDateTime.getHourOfDay();
    }

    public static List<Event> getValidLightOuts(final List<Event> lightEvents, final List<TrackerMotion> rawMotion, final int deltaWindowMin){
        final ArrayList<Event> multiLightOuts = new ArrayList<>();

        for(final Event lightEvent:lightEvents) {
            final long windowStartTimestamp = lightEvent.getEndTimestamp() - deltaWindowMin * DateTimeConstants.MILLIS_PER_MINUTE;
            final long windowEndTimestamp = lightEvent.getEndTimestamp() + deltaWindowMin * DateTimeConstants.MILLIS_PER_MINUTE;
            int motionCount = 0;

            final int hourOfLightOut = getHourOfLightOut(lightEvent);

            // Light event happens after 5am and before 8 pm is going to be ignore.
            if(hourOfLightOut >= LATEST_LIGHT_EVENT_HOUROFDAY && hourOfLightOut < EARLIEST_LIGHT_EVENT_HOUROFDAY){
                continue;
            }

            for(final TrackerMotion motion:rawMotion) {
                final long timestamp = motion.timestamp;
                if(timestamp < windowStartTimestamp || timestamp > windowEndTimestamp){
                    continue;
                }

                motionCount++;

            }
            if(motionCount > LIGHT_MOTION_CORRELATION_COUNT_THRESHOLD) {
                multiLightOuts.add(lightEvent);
            }
        }

        if(multiLightOuts.size() <= 1){
            return multiLightOuts;
        }

        // Light out profiling.
        // I know it could be trained, but let's don't overkill the problem before we found it necessary.
        final Event lastLightOut = multiLightOuts.get(multiLightOuts.size() - 1);
        final int hourOfLastLightOut = getHourOfLightOut(lastLightOut);
        if(hourOfLastLightOut >= 1 && hourOfLastLightOut < LATEST_LIGHT_EVENT_HOUROFDAY){
            // Keep all lights out, might have an edge case, user can light out too early.
            return multiLightOuts;
        }

        // Only one light out, and it's before 00:00, keep it.
        return Lists.newArrayList(lastLightOut);
    }

    public static List<DateTime> getLightOutTimes(final List<Event> validLightOuts){
        final ArrayList<DateTime> lightOutTimes = new ArrayList<>();
        for(final Event lightOut:validLightOuts){
            lightOutTimes.add(new DateTime(lightOut.getEndTimestamp(), DateTimeZone.forOffsetMillis(lightOut.getTimezoneOffset())));
        }

        return lightOutTimes;
    }
}
