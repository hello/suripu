package com.hello.suripu.core.algorithmintegration;

import com.hello.suripu.api.datascience.OnlineHmmProtos;

/**
 * Created by benjo on 10/18/15.
 */
public class ModelVotingInfo {
    public final Double prob;

    public ModelVotingInfo(final Double prob) {
        this.prob = prob;
    }

    public ModelVotingInfo(final OnlineHmmProtos.VotingInfo votingInfo) {
        this.prob = votingInfo.getProbabilityOfModel();
    }
}
