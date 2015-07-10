package com.hello.suripu.coredw.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;


import com.yammer.dropwizard.config.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class EmailConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("api_key")
    private String apiKey;
    public String apiKey() {
        return apiKey;
    }


    @Valid
    @JsonProperty("email_from")
    private String emailFrom = "support@hello.is";
    public String emailFrom() {
        return emailFrom;
    }

    @Valid
    @JsonProperty("name_from")
    private String nameFrom = "Hello";
    public String nameFrom() {
        return nameFrom;
    }

    @Valid
    @NotNull
    @JsonProperty("link_host")
    private String linkHost;
    public String linkHost() {
        return linkHost;
    }
}
