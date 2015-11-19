package com.hello.suripu.core.db.dynamo.expressions;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public class AndExpression implements Expression {
    private final List<Expression> expressions;

    public AndExpression(final List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public Map<String, AttributeValue> expressionAttributeValues() {
        final ImmutableMap.Builder<String, AttributeValue> builder = new ImmutableMap.Builder<>();
        for (final Expression expression : expressions) {
            builder.putAll(expression.expressionAttributeValues());
        }
        return builder.build();
    }

    @Override
    public String expressionString() {
        final List<String> expStrings = Lists.newArrayListWithExpectedSize(expressions.size());
        for (final Expression expression: expressions) {
            expStrings.add(expression.expressionString());
        }
        return Joiner.on(" AND ").join(expStrings);
    }
}
