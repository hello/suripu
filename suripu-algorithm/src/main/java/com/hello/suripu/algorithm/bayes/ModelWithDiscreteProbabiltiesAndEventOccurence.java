package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by benjo on 6/15/15.
 */
public interface ModelWithDiscreteProbabiltiesAndEventOccurence {

    public void inferModelGivenObservedProbabilities(final ImmutableList<Double> label);
    public ImmutableList<Double> inferProbabilitiesGivenModel(final ImmutableList<Double> prior);

    //TODO serialize and deserialize to JSON or protobuf SOMEHOW

}
