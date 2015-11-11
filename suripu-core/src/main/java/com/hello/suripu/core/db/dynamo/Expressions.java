package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.hello.suripu.core.db.dynamo.expressions.AndExpression;
import com.hello.suripu.core.db.dynamo.expressions.BetweenExpression;
import com.hello.suripu.core.db.dynamo.expressions.BinaryExpression;
import com.hello.suripu.core.db.dynamo.expressions.Expression;

import java.util.Arrays;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public class Expressions {
    public static BinaryExpression compare(final Attribute attribute, final String operator, final AttributeValue toValue) {
        return new BinaryExpression(attribute, operator, toValue);
    }

    public static AndExpression and(Expression... expressions) {
        return new AndExpression(Arrays.asList(expressions));
    }

    public static BetweenExpression between(final Attribute attribute, final AttributeValue lowerBound, final AttributeValue upperBound) {
        return new BetweenExpression(attribute, lowerBound, upperBound);
    }
}
