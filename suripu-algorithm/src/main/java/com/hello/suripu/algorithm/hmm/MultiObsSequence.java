package com.hello.suripu.algorithm.hmm;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Map;

/**
 * Created by benjo on 8/17/15.
 */
public class MultiObsSequence {

    public MultiObsSequence(Map<String, double[][]> rawmeasurements, Map<Integer, Integer> labels, Multimap<Integer, Transition> forbiddenTransitions) {
        this.rawmeasurements = rawmeasurements;
        this.labels = labels;
        this.forbiddenTransitions = forbiddenTransitions;
    }

    public final Map<String,double [][]> rawmeasurements;
    public final Map<Integer,Integer> labels;
    public final Multimap<Integer,Transition> forbiddenTransitions;



    public static MultiObsSequence createModelPathsToMultiObsSequence(final Map<String,ImmutableList<Integer>> features,
                                                                final Optional< Map<Integer, Integer>> labelsOptional) {

        //empty forbidden transitions
        final Multimap<Integer, Transition> forbiddenTransitions = ArrayListMultimap.create();

        return createModelPathsToMultiObsSequence(features, forbiddenTransitions, labelsOptional);
    }

    public static MultiObsSequence createModelPathsToMultiObsSequence(final Map<String,ImmutableList<Integer>> features,
                                                                final Multimap<Integer, Transition> forbiddenTransitions,
                                                                final Optional< Map<Integer, Integer>> labelsOptional) {

        final Map<String, double[][]> rawmeasurements = Maps.newHashMap();

        for (final String modelId : features.keySet()) {
            final ImmutableList<Integer> featureAlphabet = features.get(modelId);

            final double [][] x = new double [1][featureAlphabet.size()];

            for (int i = 0; i < featureAlphabet.size(); i++) {
                x[0][i] = featureAlphabet.get(i);
            }

            rawmeasurements.put(modelId,x);
        }

        Map<Integer, Integer> labels = Maps.newHashMap(); //no labels

        if (labelsOptional.isPresent()) {
            labels = labelsOptional.get();
        }

        return new MultiObsSequence(rawmeasurements,labels,forbiddenTransitions);

    }

}
