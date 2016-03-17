package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * Created by jakepiccolo on 3/17/16.
 */
public class UtilTest {

    @Test
    public void testGetLogFormattedAttributes() throws Exception {
        final Map<String, AttributeValue> map = ImmutableMap.of(
                "Bool", new AttributeValue().withBOOL(true),
                "N", new AttributeValue().withN("100"),
                "S", new AttributeValue().withS("astring"),
                "B", new AttributeValue().withB(ByteBuffer.wrap("123".getBytes()))
        );
        final String logMessage = Util.getLogFormattedAttributes(map);
        Assert.assertThat(logMessage, containsString("Bool=true"));
        Assert.assertThat(logMessage, containsString("N=100"));
        Assert.assertThat(logMessage, containsString("S=astring"));
        Assert.assertThat(logMessage, containsString("B=123"));
    }
}