package com.hello.suripu.core.util;

import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.motion.PartnerMotionAtSecond;
import com.hello.suripu.core.models.motion.PartnerMotionTimeSeries;
import com.hello.suripu.core.models.motion.TrackerMotionWithPartnerMotion;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by jarredheinrich on 12/28/16.
 */
public class MotionMaskPartnerFilter {
    final private static int UNCERTAINTY_SECS = 1;
    //add uncertainty
    public static List<TrackerMotion> partnerFiltering(final List<TrackerMotion> trackerMotions,
                                                        final  List<TrackerMotion> partnerMotions){

        if (trackerMotions.isEmpty() || partnerMotions.isEmpty()){
            return trackerMotions;
        }
        //requires motion mask
        if(!trackerMotions.get(0).motionMask.isPresent() ||!partnerMotions.get(0).motionMask.isPresent()){
            return trackerMotions;
        }

        final PartnerMotionTimeSeries originalMotionTimeSeries= PartnerMotionTimeSeries.create(trackerMotions,partnerMotions);
        final List<TrackerMotionWithPartnerMotion> originalTrackerMotionTimeSeriesWithPartner = originalMotionTimeSeries.groupByLeft();
        final List<TrackerMotion> filteredTrackerMotions = new ArrayList<>();
        final ListIterator<TrackerMotionWithPartnerMotion> trackerMotionIterator = originalTrackerMotionTimeSeriesWithPartner.listIterator();

        TrackerMotionWithPartnerMotion previousTrackerMotionWithPartnerMotion = originalTrackerMotionTimeSeriesWithPartner.get(0);
        TrackerMotionWithPartnerMotion trackerMotionWithPartner = originalTrackerMotionTimeSeriesWithPartner.get(0);
        TrackerMotionWithPartnerMotion nextTrackerMotionWithPartner = originalTrackerMotionTimeSeriesWithPartner.get(0);
        boolean hasNext = true;


        //Iterates through TrackerMotionWithPartnerMotions, checks for unique user motion for each TrackerMotion using a +/- 1 second window.
        while(hasNext){
            if (trackerMotionIterator.hasPrevious()) {
                previousTrackerMotionWithPartnerMotion = trackerMotionWithPartner;
                trackerMotionWithPartner = nextTrackerMotionWithPartner;
            } else{
                trackerMotionIterator.next();
            }

            //check for motion at previous second
            boolean partnerMovedAtPreviousSecond = false;
            if (trackerMotionIterator.hasPrevious()){
                final int interMotionTimeDiffSeconds =(int) (trackerMotionWithPartner.trackerMotion.timestamp - previousTrackerMotionWithPartnerMotion.trackerMotion.timestamp )/ DateTimeConstants.MILLIS_PER_SECOND;
                if (interMotionTimeDiffSeconds <= DateTimeConstants.SECONDS_PER_MINUTE && interMotionTimeDiffSeconds > 1 && previousTrackerMotionWithPartnerMotion.partnerMotionAtSeconds.size() > interMotionTimeDiffSeconds - 1) {
                    partnerMovedAtPreviousSecond= previousTrackerMotionWithPartnerMotion.partnerMotionAtSeconds.get(interMotionTimeDiffSeconds - 1).didPartnerMove;
                }
            }

            //check for motion at next second
            boolean partnerMovedNextSecond = false;
            if(trackerMotionIterator.hasNext()){
                nextTrackerMotionWithPartner = trackerMotionIterator.next();
                final int interMotionTimeDiffSeconds = (int) (nextTrackerMotionWithPartner.trackerMotion.timestamp - trackerMotionWithPartner.trackerMotion.timestamp) / DateTimeConstants.MILLIS_PER_SECOND;
                if (interMotionTimeDiffSeconds <= 60 && interMotionTimeDiffSeconds > 0 && !nextTrackerMotionWithPartner.partnerMotionAtSeconds.isEmpty()) {
                    partnerMovedNextSecond= nextTrackerMotionWithPartner.partnerMotionAtSeconds.get(DateTimeConstants.SECONDS_PER_MINUTE - interMotionTimeDiffSeconds).didPartnerMove;
                }
            } else{
                //
                hasNext = false;
            }

            //no associated partner motion: add and continue.
            if(trackerMotionWithPartner.partnerMotionAtSeconds.isEmpty()){
                filteredTrackerMotions.add(trackerMotionWithPartner.trackerMotion);
                continue;
            }

            //otherwise check for a least 1 second of unique user motion
            final boolean hasUniqueUserMotion = hasUniqueMotionAtSecond(trackerMotionWithPartner, partnerMovedNextSecond, partnerMovedAtPreviousSecond);

            if (hasUniqueUserMotion){
                filteredTrackerMotions.add(trackerMotionWithPartner.trackerMotion);
            }
        }
        return filteredTrackerMotions;
    }


    private static boolean hasUniqueMotionAtSecond(final TrackerMotionWithPartnerMotion trackerMotionWithPartner,boolean partnerMovedNextMinuteFirstSecond, boolean partnerMovedAtPrevSecond) {
        PartnerMotionAtSecond motionAtCurrentSecond = trackerMotionWithPartner.partnerMotionAtSeconds.get(0);
        PartnerMotionAtSecond motionAtNextSecond = trackerMotionWithPartner.partnerMotionAtSeconds.get(1);
        boolean partnerMovedAtSecond = motionAtCurrentSecond.didPartnerMove;
        boolean partnerMovedAtNextSecond;

        for (int second = 0; second < DateTimeConstants.SECONDS_PER_MINUTE; second++) {
            if (second > 0) {
                partnerMovedAtPrevSecond = partnerMovedAtSecond;
                motionAtCurrentSecond = motionAtNextSecond;
            }
            
            if (second + UNCERTAINTY_SECS < DateTimeConstants.SECONDS_PER_MINUTE) {
                motionAtNextSecond = trackerMotionWithPartner.partnerMotionAtSeconds.get(second + UNCERTAINTY_SECS);
                partnerMovedAtNextSecond = motionAtNextSecond.didPartnerMove;
            } else {
                partnerMovedAtNextSecond = partnerMovedNextMinuteFirstSecond;
            }
            partnerMovedAtSecond = motionAtCurrentSecond.didPartnerMove;

            //did user move
            if (!motionAtCurrentSecond.didMove) {
                continue;
            }
            //did partnerMove
            if (partnerMovedAtNextSecond || partnerMovedAtSecond || partnerMovedAtPrevSecond) {
                continue;
            }

            //user moved with not associated partner motion
            return true;
        }
        //no unique partner motions at this point
        return false;
    }
}
