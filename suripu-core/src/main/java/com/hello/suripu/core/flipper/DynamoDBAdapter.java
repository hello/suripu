package com.hello.suripu.core.flipper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.librato.rollout.RolloutAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

    private static final Splitter splitter = Splitter.on('|');
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBAdapter.class);
    private final AtomicReference<Map<String, Feature>> features = new AtomicReference<Map<String, Feature>>();
    private ScheduledFuture scheduledFuture;
    private final Integer pollingIntervalInSeconds;
    private final AmazonDynamoDBClient client;
    private final String namespace;

    public DynamoDBAdapter(final AmazonDynamoDBClient client, final Integer pollingIntervalInSeconds, final String namespace) {
        this.client = client;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        this.namespace = namespace;
        start();
    }

    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private void startPolling() {
        scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final Map<String, Feature> temp = getData();
                features.set(temp);
            }
        } , pollingIntervalInSeconds, pollingIntervalInSeconds, TimeUnit.SECONDS);
    }


    public void start() {
        final Map<String, Feature> temp = getData();
        features.set(temp);
        LOGGER.info("Starting polling");
        startPolling();
    }

    public void stop() {
        scheduledFuture.cancel(true);
        LOGGER.info("Stopped polling");
        executorService.shutdown();
        LOGGER.info("ThreadPool shutdown");
    }

    private Map<String, Feature> getData() {
        LOGGER.trace("Calling getData");

        final Map<String, Condition> conditions = new HashMap<>();
        conditions.put("ns", new Condition()
            .withComparisonOperator(ComparisonOperator.EQ)
            .withAttributeValueList(new AttributeValue().withS(namespace)));
        conditions.put("name", new Condition()
            .withComparisonOperator(ComparisonOperator.BEGINS_WITH)
            .withAttributeValueList(new AttributeValue().withS("o")));

        final int queryLimit = 100;

        final QueryRequest query = new QueryRequest("features")
            .withKeyConditions(conditions)
            .withLimit(queryLimit)
            .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);


        final QueryResult results = client.query(query);


        final Map<String, Feature> finalMap = new HashMap<>();

        for(final Map<String, AttributeValue> map : results.getItems()) {
            final String name = map.get("name").getS();
            final String value = map.get("value").getS();
            try {
                finalMap.put(name, convertToFeature(name, value));
            } catch (RolloutException e) {
                LOGGER.error("Failed to parse feature: {} reason: {}", value, e.getMessage());
            }
        }

        return finalMap;
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
            LOGGER.error("Feature is null");
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

        if (hashId % 10 < f.percentage / 10) {
            LOGGER.trace("Included in percentage");
            return true;
        }

        LOGGER.trace("Feature is NOT active");
        return false;
    }


    /**
     * Converts a String percentage|user_id,user_id,...|group,_group
     * to a Feature
     * @param featureName
     * @param value
     * @return immutable Feature object
     */
    private Feature convertToFeature(final String featureName, final String value) {
        final String[] splitResult = Iterables.toArray(splitter.split(value), String.class);
        if (splitResult.length != 3) {
            LOGGER.error("Invalid format: {}, (length {})", value, splitResult.length);
            throw new RolloutException("Invalid format for feature");
        }

        final List<String> groups = Arrays.asList(splitResult[2].split(","));
        final List<String> userIds = Arrays.asList(splitResult[1].split(","));
        final int percentage = Integer.parseInt(splitResult[0]);
        return new Feature(featureName, userIds, groups, percentage);
    }

    /**
     * Feature object
     */
    private class Feature {
        public final String name;
        public final Set<String> ids;
        public final Set<String> groups;
        public final Integer percentage;

        private Feature(final String name, final Collection<String> ids, final Collection<String> groups, final Integer percentage) {
            this.name = name;
            this.ids = ImmutableSet.copyOf(ids);
            this.groups = ImmutableSet.copyOf(groups);
            this.percentage = percentage;
        }
    }

    private class RolloutException extends RuntimeException {
        public RolloutException(final String message) {
            super(message);
        }
    }
}
