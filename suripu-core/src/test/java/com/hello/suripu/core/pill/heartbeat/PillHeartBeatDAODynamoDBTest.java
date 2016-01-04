package com.hello.suripu.core.pill.heartbeat;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PillHeartBeatDAODynamoDBTest {

    final AmazonDynamoDB client = mock(AmazonDynamoDB.class);
    final PillHeartBeatDAODynamoDB db = PillHeartBeatDAODynamoDB.create(client, "my_table");

    @Test
    public void testQueryNoData() {
        final AmazonDynamoDB client = mock(AmazonDynamoDB.class);
        Map<String, AttributeValue> item = Maps.newHashMap();
        final QueryResult queryResult = new QueryResult().withItems(Lists.newArrayList(item));
        when(client.query(any(QueryRequest.class))).thenReturn(queryResult);

        final PillHeartBeatDAODynamoDB db = PillHeartBeatDAODynamoDB.create(client, "my_table");
        final Optional<PillHeartBeat> pillHeartBeatOptional = db.get("ABC");
        assertThat(pillHeartBeatOptional.isPresent(), is(false));
    }

    @Test
    public void testQueryMultipleResults() {

        final Map<String, AttributeValue> item = ImmutableMap.of(
                "pill_id", new AttributeValue().withS("ABC"),
                "utc_dt", new AttributeValue().withS("2015-10-15 00:00:00"),
                "battery_level", new AttributeValue().withN("10"),
                "uptime", new AttributeValue().withN("10000"),
                "fw_version", new AttributeValue().withN("2")
        );

        final QueryResult queryResult = new QueryResult().withItems(Lists.newArrayList(item));
        when(client.query(any(QueryRequest.class))).thenReturn(queryResult);

        final Optional<PillHeartBeat> pillHeartBeatOptional = db.get("ABC");
        assertThat(pillHeartBeatOptional.isPresent(), is(true));
    }
}
