package com.hello.suripu.coredw8.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import jersey.repackaged.com.google.common.base.MoreObjects;
import org.hibernate.validator.constraints.NotEmpty;

public class GraphiteConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("host")
    private String host;

    public String getHost() {
        return host;
    }

    @Valid
    @NotNull
    @JsonProperty("api_key")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Valid
    @Min(5)
    @Max(120)
    @JsonProperty("reporting_interval_in_seconds")
    private Integer reportingIntervalInSeconds;

    public Integer getReportingIntervalInSeconds() {
        return reportingIntervalInSeconds;
    }


    @NotEmpty
    @JsonProperty("include_metrics")
    private List<String> includeMetrics;


    public List<String> getIncludeMetrics() {
        return ImmutableList.copyOf(includeMetrics);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("host", host)
                .add("include_metrics", includeMetrics)
                .toString();
    }
}
