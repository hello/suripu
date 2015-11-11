package com.hello.suripu.core.db.dynamo;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jakepiccolo on 11/11/15.
 */
public class AttributeUtils {
    public static String expressionAttributeValue(final Attribute attribute) {
        return ":" + attribute.sanitizedName();
    }

    public static String expressionAttributeName(final Attribute attribute) {
        return "#" + attribute.sanitizedName();
    }

    public static String expressionAttributeValueStart(final Attribute attribute) {
        return expressionAttributeValue(attribute) + "_start";
    }

    public static String expressionAttributeValueEnd(final Attribute attribute) {
        return expressionAttributeValue(attribute) + "_end";
    }

    public static Map<String, String> getExpressionAttributeNames(final Collection<? extends Attribute> attributes) {
        final Map<String, String> names = Maps.newHashMapWithExpectedSize(attributes.size());
        for (final Attribute attribute: attributes) {
            names.put(expressionAttributeName(attribute), attribute.shortName());
        }
        return names;
    }

    public static String getProjectionExpression(final Collection<? extends Attribute> attributes) {
        return Joiner.on(", ").join(getExpressionAttributeNames(attributes).keySet());
    }
}
