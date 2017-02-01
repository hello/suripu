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
        final List<TrackerMotionWithPartnerMotion> removedTrackerMotionsWithPartnerMotion = new ArrayList<>();


        PartnerMotionAtSecond motionAtSecond;

        final ListIterator<TrackerMotionWithPartnerMotion> trackerMotionIterator = originalTrackerMotionTimeSeriesWithPartner.listIterator();
        TrackerMotionWithPartnerMotion previousTrackerMotionWithPartnerMotion = originalTrackerMotionTimeSeriesWithPartner.get(0);
        for (final TrackerMotionWithPartnerMotion trackerMotionWithPartner : originalTrackerMotionTimeSeriesWithPartner){
            boolean userMovement = false;
            boolean partnerMovedAtSecond = false;
            if (trackerMotionIterator.hasPrevious()){
                final int interMotionTimeDiffSeconds =(int) (trackerMotionWithPartner.trackerMotion.timestamp - previousTrackerMotionWithPartnerMotion.trackerMotion.timestamp )/ DateTimeConstants.MILLIS_PER_SECOND;
                if (interMotionTimeDiffSeconds <= 60 && interMotionTimeDiffSeconds > 1 && previousTrackerMotionWithPartnerMotion.partnerMotionAtSeconds.size() > interMotionTimeDiffSeconds - 1) {
                    partnerMovedAtSecond= previousTrackerMotionWithPartnerMotion.partnerMotionAtSeconds.get(interMotionTimeDiffSeconds - 1).didPartnerMove;
                }
            }
            if(trackerMotionWithPartner.partnerMotionAtSeconds.size()==0){
                filteredTrackerMotions.add(trackerMotionWithPartner.trackerMotion);
                continue;
            }
            PartnerMotionAtSecond motionAtNextSecond = trackerMotionWithPartner.partnerMotionAtSeconds.get(0);
            boolean partnerMovedAtNextSecond = motionAtNextSecond.didPartnerMove;


            for (int second = 0; second < 60;  second ++){
                motionAtSecond = motionAtNextSecond;
                if (trackerMotionWithPartner.partnerMotionAtSeconds.size() > second + UNCERTAINTY_SECS){
                    motionAtNextSecond = trackerMotionWithPartner.partnerMotionAtSeconds.get(second + UNCERTAINTY_SECS);
                    partnerMovedAtNextSecond = motionAtNextSecond.didPartnerMove;
                }else if(trackerMotionIterator.hasNext()){
                    TrackerMotionWithPartnerMotion nextTrackerMotionWithPartner = trackerMotionIterator.next();
                    final int interMotionTimeDiffSeconds =(int) (nextTrackerMotionWithPartner.trackerMotion.timestamp - trackerMotionWithPartner.trackerMotion.timestamp)/ DateTimeConstants.MILLIS_PER_SECOND;
                    if (interMotionTimeDiffSeconds <= 60 && interMotionTimeDiffSeconds > 0 && !nextTrackerMotionWithPartner.partnerMotionAtSeconds.isEmpty()) {
                        motionAtNextSecond = nextTrackerMotionWithPartner.partnerMotionAtSeconds.get(DateTimeConstants.SECONDS_PER_MINUTE - interMotionTimeDiffSeconds);
                        partnerMovedAtNextSecond = motionAtNextSecond.didPartnerMove;
                    }
                }
                Boolean partnerMovedAtPrevSecond = partnerMovedAtSecond;
                partnerMovedAtSecond = motionAtSecond.didPartnerMove;

                //did user move
                if(!motionAtSecond.didMove){
                    continue;
                }
                //did partnerMove
                if (partnerMovedAtNextSecond || partnerMovedAtSecond || partnerMovedAtPrevSecond){
                    continue;
                }

                userMovement = true;
            }


            if (userMovement){
                filteredTrackerMotions.add(trackerMotionWithPartner.trackerMotion);
            }else{
                removedTrackerMotionsWithPartnerMotion.add(trackerMotionWithPartner);
            }
            previousTrackerMotionWithPartnerMotion = trackerMotionWithPartner;

        }
        return filteredTrackerMotions;
    }
}
