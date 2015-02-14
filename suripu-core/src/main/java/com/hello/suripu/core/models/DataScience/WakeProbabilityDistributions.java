package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.DataScience.GaussianDistribution;

/**
 * Created by benjo on 2/13/15.
 */
public class WakeProbabilityDistributions {

    public WakeProbabilityDistributions(final GaussianDistribution prediction_bias_dist,
                                        final GaussianDistribution wake_time_dist,
                                        final GaussianDistribution out_of_bed_prediction_bias_dist) {
        this.prediction_bias_dist = prediction_bias_dist;
        this.wake_time_dist = wake_time_dist;
        this.out_of_bed_prediction_bias_dist = out_of_bed_prediction_bias_dist;
    }

    @JsonProperty("prediction_bias_dist")
    final GaussianDistribution prediction_bias_dist;

    @JsonProperty("wake_time_dist")
    final GaussianDistribution wake_time_dist;

    @JsonProperty("out_of_bed_prediction_bias_dist")
    final GaussianDistribution out_of_bed_prediction_bias_dist;


}
