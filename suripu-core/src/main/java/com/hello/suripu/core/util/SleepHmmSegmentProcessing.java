package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.Segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by benjo on 5/25/15.
 */
public class SleepHmmSegmentProcessing {

    public static  ImmutableList<SegmentPairWithGaps> filterSleepSegmentPairsByHeuristic(final ImmutableList<SegmentPairWithGaps> pairs) {
        //heuristic is -- filter out a duration 1 sleep segment, but only if it's before the "main" segment, which is defined as 1 hour of sleep or more.

        final List<SegmentPairWithGaps> filteredResults = new ArrayList<SegmentPairWithGaps>();
        boolean isFoundMainSleepSegment = false;

        for (int i = 0; i < pairs.size(); i++) {
            final SegmentPairWithGaps pair = pairs.get(i);
            final boolean isLast = (i == pairs.size() - 1);
            final int pairDuration = pair.bounds.i2 - pair.bounds.i1 + 1;

            if (pairDuration > 4) {
                isFoundMainSleepSegment = true;
            }

            //haven't found main segment, and is orphan
            if (pairDuration <= 1 && !isFoundMainSleepSegment) {
                continue;
            }


            //is last segment, and is orphan.  Get rid of it!
            if (pairDuration <= 1 && isLast) {
                continue;
            }

            filteredResults.add(pair);
        }

        return ImmutableList.copyOf(filteredResults);

    }

    public static ImmutableList<SegmentPairWithGaps> filterSegmentPairsByDuration(final ImmutableList<SegmentPairWithGaps> pairs,final  int durationThreshold) {

        final List<SegmentPairWithGaps> filteredResults = new ArrayList<SegmentPairWithGaps>();

        for (final SegmentPairWithGaps pair : pairs) {
            final int pairDuration = pair.bounds.i2 - pair.bounds.i1;

            if (pairDuration >= durationThreshold) {
                filteredResults.add(pair);
            }
        }

        return ImmutableList.copyOf(filteredResults);

    }

    public static ImmutableList<SegmentPair> filterPairsByDuration(final ImmutableList<SegmentPair> pairs,final  int durationThreshold) {

        final List<SegmentPair> filteredResults = new ArrayList<SegmentPair>();

        for (final SegmentPair pair : pairs) {
            final int pairDuration = pair.i2 - pair.i1;

            if (pairDuration >= durationThreshold) {
                filteredResults.add(pair);
            }
        }

        return ImmutableList.copyOf(filteredResults);

    }

    protected static class SegmentPairWithCondition implements Comparable<SegmentPairWithCondition>{
        public  SegmentPairWithCondition(int i1, int i2, boolean condition) {
            this.seg = new SegmentPair(i1,i2);
            this.condition = condition;
        }
        public final SegmentPair seg;
        public final boolean condition;

        @Override
        public int compareTo(final SegmentPairWithCondition o) {

            if (o.seg.i1 > seg.i2) {
                return -1;
            }

            if (o.seg.i2 < seg.i1) {
                return 1;
            }

            return 0;
        }
    }

    //looking for uncond, cond, uncond
    //or just uncond
    //but where i2 == i1 - 1
    protected  static class SegmentPairWithConditionState {
        List<SegmentPair> merged;
        LinkedList<SegmentPairWithCondition> states;

        public SegmentPairWithConditionState() {
            merged = new ArrayList<>();
            states = new LinkedList<SegmentPairWithCondition>();
        }

        public void setNext(final SegmentPairWithCondition seg) {
            states.add(seg);

            if (states.size() < 3) {
                return;
            }

            SegmentPairWithCondition arr[] = new SegmentPairWithCondition[states.size()];
            arr = states.toArray(arr);

            //looking for uncond, cond, uncond where the segments are contiguous
            if (arr[0].condition == false && arr[1].condition == true && arr[2].condition == false) {
                if (arr[0].seg.i2 == arr[1].seg.i1 - 1 && arr[1].seg.i2 == arr[2].seg.i1 - 1) {
                    merged.add(new SegmentPair(arr[0].seg.i1,arr[2].seg.i2));
                    states.clear();
                    return;
                }
            }


            //anything that didn't match the above pattern but is unconditional
            //keep it
            if (states.getFirst().condition == false) {
                merged.add(seg.seg);
            }

            states.removeFirst();

        }

        public ImmutableList<SegmentPair> getMergedStates() {
            //process what is left in the states
            Iterator<SegmentPairWithCondition> it = states.iterator();

            while (it.hasNext()) {
                final SegmentPairWithCondition seg = it.next();

                if (seg.condition == false) {
                    //unconditional! add it.
                    merged.add(seg.seg);
                }
            }

            return ImmutableList.copyOf(merged);
        }


    }


    static public ImmutableList<SegmentPair> mergeConditionalAndUnconditionalPairs(final ImmutableList<SegmentPair> unconditionalBoundaries, final ImmutableList<SegmentPair> conditionalBoundaries) {

        List<SegmentPairWithCondition> biglist = new LinkedList<>();

        for (final SegmentPair seg : unconditionalBoundaries) {
            biglist.add(new SegmentPairWithCondition(seg.i1,seg.i2,false));
        }

        for (final SegmentPair seg : conditionalBoundaries) {
            biglist.add(new SegmentPairWithCondition(seg.i1,seg.i2,true));
        }

        Collections.sort(biglist);

        SegmentPairWithConditionState state = new SegmentPairWithConditionState();
        Iterator<SegmentPairWithCondition> iSeg = biglist.iterator();

        while (iSeg.hasNext()) {
            state.setNext(iSeg.next());
        }

        return state.getMergedStates();
    }

    //Returns the boundary indices (i.e. a segment) that is in the int array
    //if there is only a termination in the set, then we have the segment t0UTC - t2
    //if there is only a beginning in the set, then we have the segment t1 - tfinal
    public static  ImmutableList<SegmentPair> getSetBoundaries(final ImmutableList<Integer> path, final Set<Integer> inSet) {
        boolean foundBeginning = false;


        int t1 = 0;
        int t2 = 0;

        List<SegmentPair> pairList = new ArrayList<SegmentPair>();

        for (int i = 1; i < path.size(); i++) {
            int prev = path.get(i - 1);
            int current = path.get(i);


            if (inSet.contains(current) && !inSet.contains(prev)) {
                foundBeginning = true;
                t1 = i;
            }

            if (!inSet.contains(current) && inSet.contains(prev)) {
                foundBeginning = false;

                pairList.add(new SegmentPair(t1, i - 1));
            }
        }


        if (foundBeginning) {
            pairList.add(new SegmentPair(t1, path.size()));
        }


        return ImmutableList.copyOf(pairList);
    }

}
