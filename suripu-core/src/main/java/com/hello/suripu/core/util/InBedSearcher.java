package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by benjo on 2/5/16.
 *
 *
 * Input: my sleep time, my original in-bed time, motion data
 * Output: my actual in-bed time
 *
 * Example problem this solves:
 * 9pm - user gets on bed, reads a book
 * 10pm user leaves bed, go gets a burrito at Taco Bell
 * 12am user comes back, and goes to bed in a burrito-induced coma.
 *
 * We say that the user went to bed at 9pm, which is wrong.
 *
 * So we take the motion data and find clusters of motion with a very simple HMM.
 * The idea is to backtrack from sleep time to the beginning of the previous "motion cluster" and say that this was
 * in-bed.
 *
 */
public class InBedSearcher {
    final static Logger LOGGER = LoggerFactory.getLogger(InBedSearcher.class);
    final static double POISSON_MEAN_FOR_MOTION = 3.0;
    final static double POISSON_MEAN_FOR_NO_MOTION = 0.1;
    final static long MOTION_PERIOD_MILLIS = DateTimeConstants.MILLIS_PER_MINUTE * 5; //5 min

    public static Event getInBedPlausiblyBeforeSleep(final DateTime startTimeUTC, final DateTime endTimeUTC, final Event sleep, final Event inBed,final int numberOfMintesRequiredToFallAsleep, final ImmutableList<TrackerMotion> myTrackerMotion) {


        //construct 5 minute bins of pill on-durations
        final Long t0 = startTimeUTC.withZone(DateTimeZone.UTC).getMillis();
        final Long tf = endTimeUTC.withZone(DateTimeZone.UTC).getMillis();


        final int durationInIntervals = (int) ((tf - t0) / MOTION_PERIOD_MILLIS);


        final Double myMotionsBinned [] = new Double[durationInIntervals];
        Arrays.fill(myMotionsBinned,0.0);

        TrackerMotionUtils.fillBinsWithTrackerDurations(myMotionsBinned,t0,MOTION_PERIOD_MILLIS,myTrackerMotion,1,true);

        final int calculatedOnBedIndex = TrackerMotionUtils.getIndex(inBed.getStartTimestamp(),t0,MOTION_PERIOD_MILLIS,myMotionsBinned.length);
        final int calculatedFallAsleepIndex = TrackerMotionUtils.getIndex(sleep.getStartTimestamp(),t0,MOTION_PERIOD_MILLIS,myMotionsBinned.length);

        final double [] onDurationSeconds = Doubles.toArray(Arrays.asList(myMotionsBinned));
        final double [][] x = {onDurationSeconds};

        final double [][] A = {{0.99,0.01},{0.01,0.99}};
        final double [] pi = {0.5,0.5};
        final HmmPdfInterface [] obsModels = {new PoissonPdf(POISSON_MEAN_FOR_NO_MOTION,0),new PoissonPdf(POISSON_MEAN_FOR_MOTION,0)};

        final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH,2,A,pi,obsModels,0);


        final HmmDecodedResult result = hmm.decode(x,new Integer [] {0,1},1e-100);

        //check to make sure it found a motion cluster SOMEWHERE
        boolean valid = false;
        for (final Iterator<Integer> it = result.bestPath.iterator(); it.hasNext(); ) {
            if (it.next() == 1) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            LOGGER.warn("action=return_default_on_bed");
            return inBed;
        }


        boolean foundCluster = false;
        for (int i = calculatedFallAsleepIndex; i >= calculatedOnBedIndex; i--) {
           final Integer state = result.bestPath.get(i);

            if (state.equals(1)) {
                foundCluster = true;
                continue;
            }

            if (state.equals(0) && foundCluster) {


                long newInBedTime = t0 + (i + 1) * MOTION_PERIOD_MILLIS;
                final long numMillisToFallAsleep = (long)numberOfMintesRequiredToFallAsleep*(long)DateTimeConstants.MILLIS_PER_MINUTE;
                if (sleep.getStartTimestamp() - newInBedTime < numMillisToFallAsleep) {
                    newInBedTime = sleep.getStartTimestamp() - numMillisToFallAsleep;
                }

                final long tdiff = inBed.getEndTimestamp() - inBed.getStartTimestamp();



                final Event newInBedEvent =  Event.createFromType(Event.Type.IN_BED,newInBedTime , newInBedTime + tdiff, inBed.getTimezoneOffset(), Optional.of(English.IN_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent());

                LOGGER.info("action=adjusted_in_bed new_time={} dt_minutes={}",
                        new DateTime(newInBedEvent.getStartTimestamp()).withZone(DateTimeZone.forOffsetMillis(newInBedEvent.getTimezoneOffset())),
                        (newInBedEvent.getStartTimestamp() - inBed.getStartTimestamp()) / DateTimeConstants.MILLIS_PER_MINUTE);

                return newInBedEvent;
            }

        }

        return inBed;

    }

}
