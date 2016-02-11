package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 1/21/16.
 */
public class TimeSeriesDAODynamoDBTest {
    @Test
    public void testAttributeValueMapToLogString() {
        assertThat(
                TimeSeriesDAODynamoDB.attributeValueMapToLogString(
                        ImmutableMap.of(
                                "string", new AttributeValue().withS("s"),
                                "bool", new AttributeValue().withBOOL(true))),
                is("string=s"));
        assertThat(
                TimeSeriesDAODynamoDB.attributeValueMapToLogString(
                        ImmutableMap.of(
                                "number", new AttributeValue().withS("1"),
                                "bool", new AttributeValue().withBOOL(true))),
                is("number=1"));
    }
}