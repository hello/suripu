package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.Response;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 11/12/15.
 */
public class DynamoDBResponse extends Response<List<Map<String, AttributeValue>>> {
    public DynamoDBResponse(final List<Map<String, AttributeValue>> data, final Status status, final Optional<? extends Exception> exception) {
        super(data, status, exception);
    }
}