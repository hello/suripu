package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/13/14.
 */
public class PartnerMotion {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerMotion.class);

    // TODO: tune these thresholds when we have real data
    final private static int PARTNER_DEPTH_THRESHOLD = 60; // large movement
    final private static int ACCOUNT_DEPTH_THRESHOLD = 90; // small movement
    final private static int CHECK_PRECEDING_MINS = 2; // make sure user has no movement in the previous 2 mins

    public static List<PartnerMotionEvent> getPartnerData(final List<MotionEvent> partnerMotionEvents,final List<MotionEvent> myMotionEvents, int threshold) {


        // convert list of TrackerMotion to hash map for easy lookup
        final Map<Long, MotionEvent> myMotionEventsMap = new HashMap<>();
        for (final MotionEvent myMotionEvent : myMotionEvents) {
            myMotionEventsMap.put(myMotionEvent.getStartTimestamp(), myMotionEvent);
        }

        final List<PartnerMotionEvent> affectedEvents = new ArrayList<>();
        for (final MotionEvent partnerMotionEvent : partnerMotionEvents) {
            if (!myMotionEventsMap.containsKey(partnerMotionEvent.getStartTimestamp())) {
                continue;
            }

            final MotionEvent myMotionEvent = myMotionEventsMap.get(partnerMotionEvent.getStartTimestamp());
            if(myMotionEvent == null){
                LOGGER.trace("My motion not found at time {}",
                        new DateTime(partnerMotionEvent.getStartTimestamp(),
                                DateTimeZone.forOffsetMillis(partnerMotionEvent.getTimezoneOffset())));
                continue;
            }

            final int mySleepDepth = myMotionEvent.getSleepDepth();
            final int partnerSleepDepth = partnerMotionEvent.getSleepDepth();
            if (mySleepDepth >= ACCOUNT_DEPTH_THRESHOLD ||  // original user not moving much, not affected.
                    partnerSleepDepth > PARTNER_DEPTH_THRESHOLD || // or, partner movement is not huge, no effects.
                    mySleepDepth < partnerSleepDepth) { // or, user movement is larger than partner's
                continue;
            }

            LOGGER.trace("user depth {}, partner depth {}", mySleepDepth, partnerSleepDepth);

            // check if there's any user movement in the preceding minutes
            boolean noPriorMovement = true;
            for (int i = 1; i <= CHECK_PRECEDING_MINS; i++) {
                final MotionEvent myPriorMotionEvent = myMotionEventsMap.get(partnerMotionEvent.getStartTimestamp() - i * DateTimeConstants.MILLIS_PER_MINUTE);
                if(myPriorMotionEvent == null){
                    continue;
                }

                if (myPriorMotionEvent.getSleepDepth() < ACCOUNT_DEPTH_THRESHOLD) {
                    LOGGER.trace("{} prior movement {} {}", partnerMotionEvent.getStartTimestamp(), myPriorMotionEvent.getStartTimestamp(), myPriorMotionEvent.getSleepDepth());
                    noPriorMovement = false;
                    break;
                }
            }

            if (noPriorMovement) {
                affectedEvents.add(new PartnerMotionEvent(myMotionEvent.getSleepPeriod(),
                        myMotionEvent.getStartTimestamp(),
                        myMotionEvent.getEndTimestamp(),
                        myMotionEvent.getTimezoneOffset(),
                        myMotionEvent.getSleepDepth()
                ));
            }

        }

        return ImmutableList.copyOf(affectedEvents);
    }
}
