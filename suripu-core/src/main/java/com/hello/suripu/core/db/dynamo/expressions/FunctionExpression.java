package com.hello.suripu.core.db.dynamo.expressions;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.AttributeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 12/30/15.
 */
public class FunctionExpression implements Expression {
    private final Attribute attribute;
    private final String functionName;
    private final List<AttributeValue> arguments;

    public FunctionExpression(final String functionName, final Attribute attribute, final List<AttributeValue> arguments) {
        this.attribute = attribute;
        this.functionName = functionName;
        this.arguments = arguments;
    }

    public static FunctionExpression create(final String functionName,
                                            final Attribute attribute,
                                            final AttributeValue... arguments)
    {
        return new FunctionExpression(functionName, attribute, Arrays.asList(arguments));
    }

    @Override
    public Map<String, AttributeValue> expressionAttributeValues() {
        final Map<String, AttributeValue> map = Maps.newHashMap();
        for (int i = 0; i < arguments.size(); i++) {
            map.put(expressionAttributeValue(i), arguments.get(i));
        }
        return map;
    }

    @Override
    public String expressionString() {
        final List<String> allArguments = Lists.newArrayList();
        allArguments.add(AttributeUtils.expressionAttributeName(attribute));
        for (int i = 0; i < arguments.size(); i++) {
            allArguments.add(expressionAttributeValue(i));
        }

        return String.format("%s(%s)", functionName, Joiner.on(", ").join(allArguments));
    }

    private String expressionAttributeValue(final int index) {
        return AttributeUtils.expressionAttributeValue(attribute) + "_arg_" + index;
    }
}
