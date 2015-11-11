package com.hello.suripu.core.db.dynamo.expressions;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.AttributeUtils;

import java.util.Map;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public class BetweenExpression implements Expression {
    private final Attribute attribute;
    private final AttributeValue lowerBound;
    private final AttributeValue upperBound;

    public BetweenExpression(final Attribute attribute, final AttributeValue lowerBound, final AttributeValue upperBound) {
        this.attribute = attribute;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public Map<String, AttributeValue> getExpressionAttributeValues() {
        return ImmutableMap.of(
                AttributeUtils.expressionAttributeValueStart(attribute), lowerBound,
                AttributeUtils.expressionAttributeValueEnd(attribute), upperBound);
    }

    @Override
    public String getExpressionString() {
        return Joiner.on(" ").join(
                AttributeUtils.expressionAttributeName(attribute), "BETWEEN",
                AttributeUtils.expressionAttributeValueStart(attribute), "AND", AttributeUtils.expressionAttributeValueEnd(attribute));
    }
}
