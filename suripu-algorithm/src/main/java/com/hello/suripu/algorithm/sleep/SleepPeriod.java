package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.sun.tools.javac.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by pangwu on 3/28/15.
 */
public class SleepPeriod extends Segment {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepPeriod.class);

    public static int ACCEPTABLE_QUIET_PERIOD_MILLIS = 90 * DateTimeConstants.MILLIS_PER_MINUTE;
    private final List<List<Segment>> voteSegments = new ArrayList<>();
    private final List<Pair<Long, Double>> votes = new ArrayList<>();

    private SleepPeriod(final Segment sleepPeriod){
        super(sleepPeriod.getStartTimestamp(), sleepPeriod.getEndTimestamp(), sleepPeriod.getOffsetMillis());

        long slotMillis = sleepPeriod.getStartTimestamp() - 30 * DateTimeConstants.MILLIS_PER_MINUTE;
        while(slotMillis < sleepPeriod.getEndTimestamp() + 30 * DateTimeConstants.MILLIS_PER_MINUTE){
            votes.add(new Pair<>(slotMillis, 0d));
            slotMillis += DateTimeConstants.MILLIS_PER_MINUTE;
        }
    }

    private void voteOnSegment(final Segment votingSegment){
        for(int i = 0; i < this.votes.size(); i++){
            final Pair<Long, Double> timeSlotMillis = this.votes.get(i);
            if(timeSlotMillis.fst >= votingSegment.getStartTimestamp() && timeSlotMillis.fst <= votingSegment.getEndTimestamp()){
                this.votes.set(i, new Pair<>(timeSlotMillis.fst, timeSlotMillis.snd + 1d));
            }
        }
    }


    public static SleepPeriod createFromSegment(final Segment segment){
        return new SleepPeriod(segment);
    }

    public void addVotingSegments(final List<Segment> votingSegment){
        if(votingSegment.size() == 0){
            return;
        }
        this.voteSegments.add(Lists.newArrayList(votingSegment));
        for(final Segment voteSegment:votingSegment){
            this.voteOnSegment(voteSegment);
        }
    }

    private boolean isAwake(final long startMillis, final long endMillis){
        if(endMillis - startMillis > DateTimeConstants.MILLIS_PER_MINUTE * 10){
            return true;
        }

        double maxVote = 0;
        for(int i = 0; i < this.votes.size(); i++){
            final Pair<Long, Double> vote = this.votes.get(i);
            if(vote.fst >= startMillis && vote.fst <= endMillis && vote.snd > maxVote){
                maxVote = vote.snd;
            }
        }

        if(maxVote >= 3){  // sure sure sure
            return true;
        }

        return false;
    }

    public List<Segment> getAwakePeriods(final boolean debug){
        long startMillis = 0;
        long endMillis = 0;
        final List<Segment> result = new ArrayList<>();

        for(int i = 0; i < this.votes.size(); i++){
            final Pair<Long, Double> vote = this.votes.get(i);
            if(vote.snd >= 2d){
                if(startMillis == 0){
                    startMillis = vote.fst;
                }
                endMillis = vote.fst;
            }else {
                if(isAwake(startMillis, endMillis)){
                    result.add(new Segment(startMillis, endMillis, this.getOffsetMillis()));
                    if(debug){
                        LOGGER.debug("User awake at {} - {}",
                                new DateTime(startMillis, DateTimeZone.forOffsetMillis(this.getOffsetMillis())),
                                new DateTime(endMillis, DateTimeZone.forOffsetMillis(this.getOffsetMillis())));
                    }
                }
                startMillis = 0;
                endMillis = 0;
            }
        }

        if(isAwake(startMillis, endMillis)){
            result.add(new Segment(startMillis, endMillis, this.getOffsetMillis()));
            if(debug){
                LOGGER.debug("User awake at {} - {}",
                        new DateTime(startMillis, DateTimeZone.forOffsetMillis(this.getOffsetMillis())),
                        new DateTime(endMillis, DateTimeZone.forOffsetMillis(this.getOffsetMillis())));
            }
        }
        return result;
    }


    private static boolean isUserInBed(final long startMillis, final long endMillis, final List<AmplitudeData> alignedMotion){
        if(endMillis - startMillis <= ACCEPTABLE_QUIET_PERIOD_MILLIS) {
            return true;
        }
        int motionCount = 0;
        for(int i = 0; i< alignedMotion.size(); i++) {
            final AmplitudeData motion = alignedMotion.get(i);
            if(motion.timestamp >= startMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE &&
                    motion.timestamp <= endMillis - 10 * DateTimeConstants.MILLIS_PER_MINUTE &&
                    motion.amplitude > 0) {
                motionCount += 1;
            }
        }

        // TODO: could be trained here, but so far I don't see it as necessary
        if(motionCount <= 2){
            return false;
        }
        return true;
    }

    private static Segment constructSegmentAndEmptyStack(final Stack<Segment> stack){
        long segmentStartMillis = 0;
        long segmentEndMillis = 0;
        int offsetMillis = 0;

        while(!stack.empty()){
            if(segmentEndMillis == 0){
                segmentEndMillis = stack.peek().getEndTimestamp();
            }
            segmentStartMillis = stack.peek().getStartTimestamp();
            offsetMillis = stack.peek().getOffsetMillis();
            stack.pop();
        }
        final Segment sleepSegment = new Segment(segmentStartMillis, segmentEndMillis, offsetMillis);
        return sleepSegment;
    }

    private static Optional<Segment> getMaxPeriod(final List<Segment> segments){
        if(segments.size() == 0){
            return Optional.absent();
        }
        Segment maxPeriod = segments.get(0);
        for(final Segment segment:segments){
            if(segment.getDuration() >= maxPeriod.getDuration()){
                maxPeriod = segment;
            }
        }

        return Optional.of(maxPeriod);
    }

    public static Optional<Segment> getSleepPeriod(final List<AmplitudeData> alignedMotionAmp, final List<Segment> motionClusters){
        if(alignedMotionAmp.size() == 0 || motionClusters.size() == 0){
            return Optional.absent();
        }
        final Stack<Segment> stack = new Stack<>();
        final List<Segment> sleepPeriods = new ArrayList<>();
        long startMillis = alignedMotionAmp.get(0).timestamp;
        long endMillis = motionClusters.get(0).getStartTimestamp();
        int offsetMillis = motionClusters.get(0).getOffsetMillis();
        if(isUserInBed(startMillis, endMillis, alignedMotionAmp)){
            stack.push(new Segment(startMillis, motionClusters.get(0).getEndTimestamp(), offsetMillis));
        }

        long lastMotionMillis = alignedMotionAmp.get(alignedMotionAmp.size() - 1).timestamp;

        for(int i = 1; i < motionClusters.size(); i++){
            startMillis = motionClusters.get(i - 1).getEndTimestamp();
            endMillis = motionClusters.get(i).getStartTimestamp();
            if(isUserInBed(startMillis, endMillis, alignedMotionAmp)){
                stack.push(new Segment(startMillis, motionClusters.get(i).getEndTimestamp(), motionClusters.get(i).getOffsetMillis()));
                continue;
            }

            final Segment sleepSegment = constructSegmentAndEmptyStack(stack);
            stack.push(new Segment(motionClusters.get(i).getStartTimestamp(),
                    motionClusters.get(i).getEndTimestamp(),
                    motionClusters.get(i).getOffsetMillis()));
            LOGGER.debug("In bed period {} - {}",
                    new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())),
                    new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())));
            sleepPeriods.add(sleepSegment);

        }

        final Segment lastSegment = motionClusters.get(motionClusters.size() - 1);
        if(lastMotionMillis != lastSegment.getEndTimestamp() &&
                isUserInBed(lastSegment.getEndTimestamp(), lastMotionMillis, alignedMotionAmp)){
            stack.push(new Segment(lastSegment.getEndTimestamp(), lastMotionMillis, lastSegment.getOffsetMillis()));
        }

        if(stack.peek() != null){
            final Segment sleepSegment = constructSegmentAndEmptyStack(stack);
            LOGGER.debug("In bed period {} - {}",
                    new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())),
                    new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())));
            sleepPeriods.add(sleepSegment);
        }

        // We can support multiple sleep now!
        // We can also release the assumption of a virtual day stars from 8pm to noon the next day.
        final Optional<Segment> sleepPeriod = getMaxPeriod(sleepPeriods);
        if(sleepPeriod.isPresent()){
            LOGGER.debug("Selected sleep period {} - {}",
                    new DateTime(sleepPeriod.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.get().getOffsetMillis())),
                    new DateTime(sleepPeriod.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.get().getOffsetMillis())));
        }
        return sleepPeriod;

    }
}
