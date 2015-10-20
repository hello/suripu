package com.hello.suripu.core.db.util;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

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

    public double sum(final String key) {
        double total = 0;
        for (final Map<String, AttributeValue> item: items) {
            total += toDouble(item.get(key));
        }
        return total;
    }

    public double mean(final String key) {
        return sum(key) / items.size();
    }

    public long roundedMean(final String key) {
        return Math.round(mean(key));
    }

    public double max(final String key) {
        double currMax = Double.NEGATIVE_INFINITY;
        for (final Map<String, AttributeValue> item: items) {
            currMax = Math.max(currMax, toDouble(item.get(key)));
        }
        return currMax;
    }

    public double min(final String key) {
        double currMin = Double.POSITIVE_INFINITY;
        for (final Map<String, AttributeValue> item: items) {
            currMin = Math.min(currMin, toDouble(item.get(key)));
        }
        return currMin;
    }
}
