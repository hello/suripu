package com.hello.suripu.core.db.dynamo;

/**
 * Collection of utilities for working with types implementing Attribute
 */
public class AttributeUtils {
    public static String expressionAttributeValue(final Attribute attribute) {
        return ":" + attribute.sanitizedName();
    }

    public static String expressionAttributeName(final Attribute attribute) {
        return "#" + attribute.sanitizedName();
    }

    /**
     * @param attribute
     * @return Expression attribute value for the start of a BETWEEN query.
     */
    public static String expressionAttributeValueStart(final Attribute attribute) {
        return expressionAttributeValue(attribute) + "_start";
    }

    /**
     * @param attribute
     * @return Expression attribute value for the end of a BETWEEN query.
     */
    public static String expressionAttributeValueEnd(final Attribute attribute) {
        return expressionAttributeValue(attribute) + "_end";
    }
}
