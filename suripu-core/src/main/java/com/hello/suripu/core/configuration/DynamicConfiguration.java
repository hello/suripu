package com.hello.suripu.core.configuration;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.db.ConfigurationDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.Team;
import com.yammer.dropwizard.config.Configuration;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 6/15/15.
 */
public class DynamicConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfiguration.class);
    private ScheduledFuture scheduledFuture;
    private final Integer pollingIntervalInSeconds;
    private final ConfigurationDAODynamoDB configDAO;
    private Configuration configuration;

    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    public DynamicConfiguration(ConfigurationDAODynamoDB configDAO, final Integer pollingIntervalInSeconds) {
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        this.configDAO = configDAO;
        start();
    }

    private void startPolling() {
        scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                configuration = getData();
            }
        } , pollingIntervalInSeconds, pollingIntervalInSeconds, TimeUnit.SECONDS);
    }

    public void start() {
        LOGGER.info("Starting polling config");
        configuration = getData();
        startPolling();
    }

    public void stop() {
        scheduledFuture.cancel(true);
        LOGGER.info("Stopped polling config");
        executorService.shutdown();
        LOGGER.info("ThreadPool shutdown");
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    synchronized private Configuration getData() {
        LOGGER.debug("Calling getData");

        try {
            final Configuration config = configDAO.getData();
            LOGGER.debug(config.toString());
            return config;
        } catch (Exception e) {
            //TODO: REMOVE THIS DEBUG CODE
            final String jsonString = "{\"non_peak_hour_upper_bound\": \"20\", \"week_days_only\": \"true\", \"long_interval\": \"7\", \"short_interval\": \"5\"}";
            configDAO.put(jsonString);
            LOGGER.debug(jsonString);
        }

        return new Configuration();
    }
}
