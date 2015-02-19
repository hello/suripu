package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/18/15.
 */
public class GaussianPriorPosteriorPair {

    public GaussianPriorPosteriorPair(GaussianDistributionDataModel prior, GaussianDistributionDataModel posterior) {
        this.prior = prior;
        this.posterior = posterior;
    }

    @JsonProperty("prior")
    public final GaussianDistributionDataModel prior;

    @JsonProperty("posterior")
    public final GaussianDistributionDataModel posterior;
}
