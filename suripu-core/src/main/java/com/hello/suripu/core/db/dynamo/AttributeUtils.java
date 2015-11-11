package com.hello.suripu.core.db.dynamo;

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
}
