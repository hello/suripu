package com.hello.suripu.core.db.util;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.hello.suripu.core.db.dynamo.Item;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 10/20/15.
 */
public class DynamoDBItemAggregator {

    final List<Item> items;
    final double defaultValue;

    public DynamoDBItemAggregator(final List<Item> items) {
        this(items, 0.0);
    }

    public DynamoDBItemAggregator(final List<Item> items, final double defaultValue) {
        this.items = items;
        this.defaultValue = defaultValue;
    }

    private double toDouble(final AttributeValue attributeValue) {
        if (attributeValue == null) {
            return defaultValue;
        }
        return Double.valueOf(attributeValue.getN());
    }

    /**
     * Sum elements for key.
     * @param key
     * @return
     */
    public double sum(final String key) {
        double total = 0;
        for (final Item item: items) {
            total += toDouble(item.get(key));
        }
        return total;
    }

    /**
     * Get the mean of elements for key, or defaultValue if items are empty.
     * @param key
     * @return
     */
    public double mean(final String key) {
        if (items.isEmpty()) {
            return defaultValue;
        }
        return sum(key) / items.size();
    }

    /**
     * Get the mean for key, rounded to the nearest long.
     * @param key
     * @return
     */
    public long roundedMean(final String key) {
        return Math.round(mean(key));
    }

    /**
     * Get the maximum value for key, or defaultValue if items are empty.
     * @param key
     * @return
     */
    public double max(final String key) {
        if (items.isEmpty()) {
            return defaultValue;
        }
        double currMax = Double.NEGATIVE_INFINITY;
        for (final Item item: items) {
            currMax = Math.max(currMax, toDouble(item.get(key)));
        }
        return currMax;
    }

    /**
     * Get the minimum value for key, or defaultValue if items are empty.
     * @param key
     * @return
     */
    public double min(final String key) {
        if (items.isEmpty()) {
            return defaultValue;
        }
        double currMin = Double.POSITIVE_INFINITY;
        for (final Item item: items) {
            currMin = Math.min(currMin, toDouble(item.get(key)));
        }
        return currMin;
    }
}
