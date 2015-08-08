package com.hello.suripu.core.configuration;

import com.hello.suripu.core.db.ConfigurationDAODynamoDB;
import com.yammer.dropwizard.config.Configuration;
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
        LOGGER.info("Starting polling config: {}", configDAO.configuration.getClass().getSimpleName());
        configuration = getData();
        startPolling();
    }

    public void stop() {
        scheduledFuture.cancel(true);
        LOGGER.info("Stopped polling config: {}", configDAO.configuration.getClass().getSimpleName());
        executorService.shutdown();
        LOGGER.info("ThreadPool shutdown");
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    synchronized private Configuration getData() {
        LOGGER.debug("Polling dynamic config: {}", configDAO.configuration.getClass().getSimpleName());

        try {
            final Configuration config = configDAO.getData();
            return config;
        } catch (Exception e) {
            LOGGER.error("Dynamic Config DAO method 'getData()' failed.");
        }
        return configuration;
    }
}
