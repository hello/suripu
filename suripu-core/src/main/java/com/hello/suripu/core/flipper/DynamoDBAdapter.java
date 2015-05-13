package com.hello.suripu.core.flipper;

import com.google.common.collect.Sets;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.models.Feature;
import com.librato.rollout.RolloutAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DynamoDBAdapter implements RolloutAdapter{

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBAdapter.class);
    private final AtomicReference<Map<String, Feature>> features = new AtomicReference<Map<String, Feature>>();
    private ScheduledFuture scheduledFuture;
    private final Integer pollingIntervalInSeconds;
    private final FeatureStore featureStore;

    public DynamoDBAdapter(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        start();
    }

    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private void startPolling() {
        scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final Map<String, Feature> temp = featureStore.getData();
                features.set(temp);
            }
        } , pollingIntervalInSeconds, pollingIntervalInSeconds, TimeUnit.SECONDS);
    }


    public void start() {
        final Map<String, Feature> temp = featureStore.getData();
        features.set(temp);
        LOGGER.info("Starting polling");
        startPolling();
    }

    public void stop() {
        scheduledFuture.cancel(true);
        LOGGER.warn("Stopped polling");
        executorService.shutdown();
        LOGGER.warn("ThreadPool shutdown");
    }

    @Override
    public boolean userFeatureActive(final String feature, final long userId, final List<String> userGroups) {
        return featureActive(feature, String.valueOf(userId), userId, userGroups);
    }

    @Override
    public boolean deviceFeatureActive(final String feature, final String deviceId, final List<String> deviceGroups) {
        return featureActive(feature,deviceId,deviceId.hashCode(),deviceGroups);
    }


    /**
     * Checks if a user/device has the named feature enabled
     * @param feature
     * @param entityId
     * @param hashId
     * @param groups
     * @return boolean indicating whether feature is active
     */
    private boolean featureActive(final String feature, final String entityId, final long hashId, List<String> groups) {
        final Set<String> userGroupsSet = new HashSet<>();
        userGroupsSet.addAll(groups);

        LOGGER.trace("Feature name = {}", feature);

        final Feature f = features.get().get(feature);
        if(f == null) {
            LOGGER.warn("Feature {} is null", feature);
            return false;
        }

        if(f.ids.contains(String.valueOf(entityId))) {
            LOGGER.trace("User id in userIds");
            return true;
        }

        if (f.groups.contains("all")) {
            LOGGER.trace("feature groups contains all");
            return true;
        }

        if (userGroupsSet != null && !userGroupsSet.isEmpty()) {
            if(!Sets.intersection(userGroupsSet, f.groups).isEmpty()) {
                LOGGER.trace("User group is contained in feature groups");
                return true;
            }
        }

        if(f.percentage == 0) {
            LOGGER.trace("Percentage is 0, turning off for everyone");
            return false;
        }

        // Next, check percentage
        if (Math.abs(hashId) % 100 < f.percentage) {
            LOGGER.trace("Included in percentage");
            return true;
        }

        LOGGER.trace("Feature is NOT active");
        return false;
    }
}
