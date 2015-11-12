package com.hello.suripu.core.db.dynamo.expressions;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public interface Expression {
    /**
     * @return Map that can be passed to QueryRequest.withExpressionAttributeValues
     *  (http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/QueryRequest.html#withExpressionAttributeValues(java.util.Map))
     */
    Map<String, AttributeValue> expressionAttributeValues();

    /**
     *
     * @return String that can be passed to QueryRequest's withFilterExpression or withKeyConditionExpression
     *  (http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/QueryRequest.html#withFilterExpression(java.lang.String))
     *  (http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/model/QueryRequest.html#withKeyConditionExpression(java.lang.String))
     */
    String expressionString();
}
