package com.hello.suripu.core.db.dynamo;

/**
 * Representation of an attribute (column) name in a DynamoDB table,
 * where the short name (compact name stored in DynamoDB to minimize space) may be different from the full,
 * more descriptive name.
 */
public interface Attribute {
    /**
     * A compact name that will be stored in DynamoDB. May be a DynamoDB reserved word or contain special characters.
     * List of DynamoDB reserved words: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html
     */
    String shortName();

    /**
     * A full name that contains no special characters or DynamoDB reserved words. This is only used for substitutions,
     * i.e. Expression Attribute Names (http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ExpressionPlaceholders.html#ExpressionAttributeNames).
     */
    String sanitizedName();
}
