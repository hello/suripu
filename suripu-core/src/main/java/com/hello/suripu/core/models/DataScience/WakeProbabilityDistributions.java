package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/13/15.
 */
public class WakeProbabilityDistributions {

    public WakeProbabilityDistributions(final GaussianDistributionDataModel prediction_bias_dist,
                                        final GaussianDistributionDataModel wake_time_dist) {
        this.prediction_bias_dist = prediction_bias_dist;
        this.wake_time_dist = wake_time_dist;
    }

    @JsonProperty("prediction_bias_dist")
    public final GaussianDistributionDataModel prediction_bias_dist;

    @JsonProperty("wake_time_dist")
    public final GaussianDistributionDataModel wake_time_dist;



}
