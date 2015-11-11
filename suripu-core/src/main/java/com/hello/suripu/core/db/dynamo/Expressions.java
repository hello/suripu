package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.expressions.AndExpression;
import com.hello.suripu.core.db.dynamo.expressions.BetweenExpression;
import com.hello.suripu.core.db.dynamo.expressions.BinaryExpression;
import com.hello.suripu.core.db.dynamo.expressions.Expression;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Helper utilities for working with DynamoDB expressions.
 *
 * See http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference
 */
public class Expressions {

    /**
     * Example: compare(MY_ATTRIBUTE, ">", new AttributeValue().withN("20")) is equivalent to "my_attribute > 20"
     * @param attribute The Attribute to compare
     * @param comparator Valid values are the "comparators" here: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.SpecifyingConditions.html#ConditionExpressionReference
     * @param toValue AttributeValue to compare Attribute to using
     * @return An Expression that satisfies the condition: "<attribute> <comparator> <toValue>".
     */
    public static Expression compare(final Attribute attribute, final String comparator, final AttributeValue toValue) {
        return new BinaryExpression(attribute, comparator, toValue);
    }

    /**
     * Compound Expression that takes the "and" of multiple sub-expressions.
     * and(expression1, expression2, expression3) => "<expression1> AND <expression2> AND <expression3>"
     * @param expressions
     * @return
     */
    public static Expression and(Expression... expressions) {
        return new AndExpression(Arrays.asList(expressions));
    }

    /**
     * A "BETWEEN" expression.
     * between(MY_ATTRIBUTE, new AttributeValue().withN("10"), new AttributeValue().withN("20")) =>
     *      "my_attribute BETWEEN 10 AND 20"
     * @param attribute Attribute to compare
     * @param lowerBound Lower AttributeValue (inclusive)
     * @param upperBound Upper AttributeValue (exclusive)
     * @return
     */
    public static Expression between(final Attribute attribute, final AttributeValue lowerBound, final AttributeValue upperBound) {
        return new BetweenExpression(attribute, lowerBound, upperBound);
    }

    /**
     * @param attributes
     * @return Map that can be passed to QueryRequest.withExpressionAttributeNames
     *  (http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/QueryRequest.html#withExpressionAttributeNames(java.util.Map))
     */
    public static Map<String, String> getExpressionAttributeNames(final Collection<? extends Attribute> attributes) {
        final Map<String, String> names = Maps.newHashMapWithExpectedSize(attributes.size());
        for (final Attribute attribute: attributes) {
            names.put(AttributeUtils.expressionAttributeName(attribute), attribute.shortName());
        }
        return names;
    }

    /**
     * @param attributes
     * @return String that can be passed to QueryRequest.withProjectionExpression
     *  (http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/QueryRequest.html#withProjectionExpression(java.lang.String))
     */
    public static String getProjectionExpression(final Collection<? extends Attribute> attributes) {
        return Joiner.on(", ").join(getExpressionAttributeNames(attributes).keySet());
    }
}
