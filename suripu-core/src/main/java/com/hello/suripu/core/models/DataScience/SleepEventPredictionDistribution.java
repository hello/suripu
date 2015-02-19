package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/13/15.
 */
public class SleepEventPredictionDistribution {

    public SleepEventPredictionDistribution(final DateTime prediction,
                                            final GaussianPriorPosteriorPair biasDistributions,
                                            final GaussianPriorPosteriorPair eventTimeDistributions) {


        this.biasDistributions = biasDistributions;
        this.eventTimeDistributions = eventTimeDistributions;
        this.prediction = prediction;
    }


    @JsonProperty("prediction_bias_distributions")
    public final GaussianPriorPosteriorPair biasDistributions;

    @JsonProperty("event_time_distributions")
    public final GaussianPriorPosteriorPair eventTimeDistributions;

    @JsonProperty("event_prediction_time")
    final DateTime prediction;
}
