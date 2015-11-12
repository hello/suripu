package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

/**
 * Created by jakepiccolo on 11/12/15.
 */
public class Item {
    public final Map<String, AttributeValue> attributes;

    public Item(final Map<String, AttributeValue> attributes) {
        this.attributes = attributes;
    }

    public AttributeValue get(final String key) {
        return attributes.get(key);
    }
}
