package com.hello.suripu.core.db.util;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 10/20/15.
 */
public class DynamoDBItemAggregatorTest {

    DynamoDBItemAggregator aggregator;
    List<Map<String, AttributeValue>> items;

    private AttributeValue toAttributeValue(final double x) {
        return new AttributeValue().withN(String.valueOf(x));
    }

    private Map makeItem(final double x, final double y) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put("x", toAttributeValue(x));
        item.put("y", toAttributeValue(y));
        return item;
    }

    @Before
    public void setUp() throws Exception {
        this.items = Lists.newArrayList();
        items.add(makeItem(0, 100));
        items.add(makeItem(-3, 200));
        items.add(makeItem(-6, 10));
        items.add(makeItem(20, 1000));
        this.aggregator = new DynamoDBItemAggregator(items);
    }

    @Test
    public void testSum() throws Exception {
        final double xSum = aggregator.sum("x");
        final double ySum = aggregator.sum("y");
        assertThat(xSum, is(11.0));
        assertThat(ySum, is(1310.0));
    }

    @Test
    public void testMean() throws Exception {
        final double xMean = aggregator.mean("x");
        final double yMean = aggregator.mean("y");
        assertThat(xMean, is(2.75));
        assertThat(yMean, is(327.5));
    }

    @Test
    public void testRoundedMean() throws Exception {
        final long xMean = aggregator.roundedMean("x");
        final long yMean = aggregator.roundedMean("y");
        assertThat(xMean, is(3L));
        assertThat(yMean, is(328L));
    }

    @Test
    public void testMax() throws Exception {
        final double xMax = aggregator.max("x");
        final double yMax = aggregator.max("y");
        assertThat(xMax, is(20.0));
        assertThat(yMax, is(1000.0));
    }

    @Test
    public void testMin() throws Exception {
        final double xMin = aggregator.min("x");
        final double yMin = aggregator.min("y");
        assertThat(xMin, is(-6.0));
        assertThat(yMin, is(10.0));
    }

    @Test
    public void testMissingKey() {
        final double zMean = aggregator.mean("z");
        assertThat(zMean, is(0.0));
    }

    @Test
    public void testEmptyItems() {
        final List<Map<String, AttributeValue>> items = Lists.newArrayList();
        DynamoDBItemAggregator aggregator = new DynamoDBItemAggregator(items);
        assertThat(aggregator.sum("x"), is(0.0));
        assertThat(aggregator.mean("x"), is(0.0));
        assertThat(aggregator.min("x"), is(0.0));
        assertThat(aggregator.max("x"), is(0.0));
    }
}