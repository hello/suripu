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

    public final GaussianPriorPosteriorPair getCopy() {
        return new GaussianPriorPosteriorPair(this.prior.getCopy(),this.posterior.getCopy());
    }

    public final GaussianPriorPosteriorPair getCopyWithPosteriorAsPrior() {
        return new GaussianPriorPosteriorPair(this.posterior.getCopy(),this.posterior.getCopy());
    }

    @JsonProperty("prior")
    public final GaussianDistributionDataModel prior;

    @JsonProperty("posterior")
    public final GaussianDistributionDataModel posterior;
}
