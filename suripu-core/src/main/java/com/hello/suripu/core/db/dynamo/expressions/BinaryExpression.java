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
public class BinaryExpression implements Expression {
    private final Attribute attribute;
    private final String operator;
    private final AttributeValue compareToValue;

    public BinaryExpression(final Attribute attribute, final String operator, final AttributeValue attributeValue) {
        this.attribute = attribute;
        this.operator = operator;
        this.compareToValue = attributeValue;
    }

    @Override
    public Map<String, AttributeValue> getExpressionAttributeValues() {
        return ImmutableMap.of(AttributeUtils.expressionAttributeValue(attribute), compareToValue);
    }

    @Override
    public String getExpressionString() {
        return Joiner.on(" ").join(AttributeUtils.expressionAttributeName(attribute), operator, AttributeUtils.expressionAttributeValue(attribute));
    }
}
