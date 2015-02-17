package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/13/15.
 */
public class WakeProbabilityDistributions {

    public WakeProbabilityDistributions(final GaussianDistributionDataModel predictionBiasPrior,
                                        final GaussianDistributionDataModel wakeTimePrior,
                                        final GaussianDistributionDataModel predictionBiasPosterior,
                                        final GaussianDistributionDataModel wakeTimePosterior) {

        this.predictionBiasPrior = predictionBiasPrior;
        this.wakeTimePrior = wakeTimePrior;
        this.predictionBiasPosterior = predictionBiasPosterior;
        this.wakeTimePosterior = wakeTimePosterior;
    }

    public WakeProbabilityDistributions(final GaussianDistributionDataModel predictionBiasPrior,
                                        final GaussianDistributionDataModel wakeTimePrior) {


        this.predictionBiasPrior = predictionBiasPrior;
        this.wakeTimePrior = wakeTimePrior;
        this.predictionBiasPosterior = predictionBiasPrior;
        this.wakeTimePosterior = wakeTimePrior;
    }


    @JsonProperty("prediction_bias_prior")
    public final GaussianDistributionDataModel predictionBiasPrior;

    @JsonProperty("wake_time_prior")
    public final GaussianDistributionDataModel wakeTimePrior;

    @JsonProperty("prediction_bias_posterior")
    public final GaussianDistributionDataModel predictionBiasPosterior;

    @JsonProperty("wake_time_posterior")
    public final GaussianDistributionDataModel wakeTimePosterior;


}
