package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 10/9/15.
 */
public class GeneratedModel {

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("num_days")
    public final Integer numDays;

    @JsonProperty("num_models_generated")
    public final Integer numModelsGenerated;

    @JsonProperty("model_protobuf")
    public final String modelProtobuf;


    public GeneratedModel(final Long accountId,final String date,final Integer numDays,final Integer numModelsGenerated,final String modelProtobuf) {
        this.accountId = accountId;
        this.date = date;
        this.numDays = numDays;
        this.numModelsGenerated = numModelsGenerated;
        this.modelProtobuf = modelProtobuf;
    }
}
