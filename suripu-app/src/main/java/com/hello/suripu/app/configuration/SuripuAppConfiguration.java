package com.hello.suripu.app.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.configuration.LibratoConfiguration;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.db.DatabaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SuripuAppConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DatabaseConfiguration database = new DatabaseConfiguration();

    public DatabaseConfiguration getDatabaseConfiguration() {
        return database;
    }

    @Valid
    @NotNull
    @JsonProperty("metrics_enabled")
    private Boolean metricsEnabled;

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }

    @Valid
    @NotNull
    @JsonProperty("librato")
    private LibratoConfiguration librato;

    public LibratoConfiguration getLibrato() {
        return librato;
    }
}
