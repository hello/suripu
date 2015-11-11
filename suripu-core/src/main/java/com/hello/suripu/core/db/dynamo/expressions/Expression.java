package com.hello.suripu.core.db.dynamo.expressions;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public interface Expression {
    Map<String, AttributeValue> getExpressionAttributeValues();
    String getExpressionString();
}
