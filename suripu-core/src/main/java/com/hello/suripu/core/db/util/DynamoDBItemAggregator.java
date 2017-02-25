package com.hello.suripu.core.db.util;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 10/20/15.
 */
public class DynamoDBItemAggregator {

    final List<Map<String, AttributeValue>> items;
    final double defaultValue;

    public DynamoDBItemAggregator(final List<Map<String, AttributeValue>> items) {
        this(items, 0.0);
    }

    public DynamoDBItemAggregator(final List<Map<String, AttributeValue>> items, final double defaultValue) {
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
        for (final Map<String, AttributeValue> item: items) {
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
        for (final Map<String, AttributeValue> item: items) {
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
        for (final Map<String, AttributeValue> item: items) {
            currMin = Math.min(currMin, toDouble(item.get(key)));
        }
        return currMin;
    }


    /**
     * Computes the median for a key or defaultValue if items are empty
     * @param key
     * @return
     */
    public double median(final String key) {
        if(items.isEmpty()) {
            return defaultValue;
        }
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        for(final Map<String, AttributeValue> item: items) {
            statistics.addValue(toDouble(item.get(key)));
        }
        return statistics.getPercentile(50);
    }
}
