package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/13/15.
 */
public class WakeProbabilityDistributions {

    public WakeProbabilityDistributions(final GaussianDistributionDataModel prediction_bias_prior,
                                        final GaussianDistributionDataModel wake_time_prior,
                                        final GaussianDistributionDataModel prediction_bias_posterior,
                                        final GaussianDistributionDataModel wake_time_posterior) {

        this.prediction_bias_prior = prediction_bias_prior;
        this.wake_time_prior = wake_time_prior;
        this.prediction_bias_posterior = prediction_bias_posterior;
        this.wake_time_posterior = wake_time_posterior;
    }

    public WakeProbabilityDistributions(final GaussianDistributionDataModel prediction_bias_prior,
                                        final GaussianDistributionDataModel wake_time_prior) {


        this.prediction_bias_prior = prediction_bias_prior;
        this.wake_time_prior = wake_time_prior;
        this.prediction_bias_posterior = prediction_bias_prior;
        this.wake_time_posterior = wake_time_prior;
    }


    @JsonProperty("prediction_bias_prior")
    public final GaussianDistributionDataModel prediction_bias_prior;

    @JsonProperty("wake_time_prior")
    public final GaussianDistributionDataModel wake_time_prior;

    @JsonProperty("prediction_bias_posterior")
    public final GaussianDistributionDataModel prediction_bias_posterior;

    @JsonProperty("wake_time_posterior")
    public final GaussianDistributionDataModel wake_time_posterior;


}
