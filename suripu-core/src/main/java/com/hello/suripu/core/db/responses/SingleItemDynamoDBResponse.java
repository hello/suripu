package com.hello.suripu.core.db.responses;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Optional;

import java.util.Map;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class SingleItemDynamoDBResponse extends Response<Optional<Map<String, AttributeValue>>> {
    public SingleItemDynamoDBResponse(Optional<Map<String, AttributeValue>> data, Status status, Optional<? extends Exception> exception) {
        super(data, status, exception);
    }
}
