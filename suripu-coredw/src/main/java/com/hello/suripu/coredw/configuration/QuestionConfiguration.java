package com.hello.suripu.coredw.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class QuestionConfiguration extends Configuration{

    @Valid
    @NotNull
    @JsonProperty("num_skips")
    private int numSkips;

    public int getNumSkips(){
        return numSkips;
    }

}
