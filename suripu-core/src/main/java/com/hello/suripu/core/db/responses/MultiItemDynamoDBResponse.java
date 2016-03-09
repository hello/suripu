package com.hello.suripu.core.db.responses;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 11/12/15.
 */
public class MultiItemDynamoDBResponse extends Response<List<Map<String, AttributeValue>>> {
    public MultiItemDynamoDBResponse(final List<Map<String, AttributeValue>> data, final Status status, final Optional<? extends Exception> exception) {
        super(data, status, exception);
    }
}