package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.api.datascience.OnlineHmmProtos;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by benjo on 8/25/15.
 *
 *   generate motion transition restrictions
 *   The general rule is this -- only allow transitions from sleep to post-sleep if there are two-consecutive 5-minute periods of motion
 *   
 */
public class MotionTransitionRestriction implements TransitionRestriction {

    private final String motionModelId;
    private final Set<Integer> nonMotionStates;
    private final List<Transition> forbiddenTransitions;


    public MotionTransitionRestriction(String motionModelId, Set<Integer> nonMotionStates, List<Transition> forbiddenTransitions) {
        this.motionModelId = motionModelId;
        this.nonMotionStates = nonMotionStates;
        this.forbiddenTransitions = forbiddenTransitions;
    }

    public OnlineHmmProtos.MotionModelRestriction toProtobuf() {
        final OnlineHmmProtos.MotionModelRestriction.Builder builder = OnlineHmmProtos.MotionModelRestriction.newBuilder() ;

        builder.setMotionModelId(motionModelId);

        for (final Transition transition : forbiddenTransitions) {
            builder.addForbiddedenMotionTransitions(OnlineHmmProtos.Transition.newBuilder().setFrom(transition.fromState).setTo(transition.toState).build());
        }

        builder.addAllNonMotionStates(nonMotionStates);

        return builder.build();
    }

    static public Optional<MotionTransitionRestriction> createFromProtobuf(OnlineHmmProtos.MotionModelRestriction protobuf) {

        if (!protobuf.hasMotionModelId() || protobuf.getNonMotionStatesCount() == 0 || protobuf.getForbiddedenMotionTransitionsCount() == 0) {
            return Optional.absent();
        }

        final String motionModelId = protobuf.getMotionModelId();

        final Set<Integer> nonMotionStates = Sets.newHashSet();

        for (int i = 0; i < protobuf.getNonMotionStatesCount(); i++) {
            nonMotionStates.add(protobuf.getNonMotionStates(i));
        }

        final List<Transition> forbiddenTransitions = Lists.newArrayList();
        for (int i = 0; i < protobuf.getForbiddedenMotionTransitionsCount(); i++) {
            final OnlineHmmProtos.Transition transition = protobuf.getForbiddedenMotionTransitions(i);
            forbiddenTransitions.add(new Transition(transition.getFrom(),transition.getTo()));
        }

        return Optional.of(new MotionTransitionRestriction(motionModelId,nonMotionStates,forbiddenTransitions));

    }


    boolean isMotion(final Integer feature) {
        return !nonMotionStates.contains(feature);
    }

    @Override
    public Multimap<Integer, Transition> getRestrictions(final Map<String, ImmutableList<Integer>> features) {

        final Multimap<Integer, Transition> forbiddenTransitionsByTimeIndex = ArrayListMultimap.create();


        final ImmutableList<Integer> motionFeatures =  features.get(motionModelId);

        if (motionFeatures == null) {
            //TODO log this as an error
            return forbiddenTransitionsByTimeIndex;

        }

        for (int t = 0; t < motionFeatures.size() - 1; t++) {

            //if not two consecutive motion indicies, then FORBIDDEN!!!!!
            if ( !(isMotion(motionFeatures.get(t)) && isMotion(motionFeatures.get(t+1))) ) {

                for (final Transition forbidden : forbiddenTransitions) {
                    forbiddenTransitionsByTimeIndex.put(t,forbidden);
                }
            }
        }

        return forbiddenTransitionsByTimeIndex;
    }
}
