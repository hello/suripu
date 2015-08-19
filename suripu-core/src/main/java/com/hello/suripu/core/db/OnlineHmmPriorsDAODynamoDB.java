package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.datascience.OnlineHmmProtos;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class OnlineHmmPriorsDAODynamoDB implements OnlineHmmPriorsDAO {
    private final static String HASH_KEY = "account_id";
    private final static String PAYLOAD_KEY_FOR_PARAMS = "model_params";
    private final static String PAYLOAD_KEY_FOR_SCRATCHPAD = "scratchpad";
    private final static Logger LOGGER = LoggerFactory.getLogger(OnlineHmmPriorsDAODynamoDB.class);

    private final GeneralProtobufDAODynamoDB dbDAO;


    public OnlineHmmPriorsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        final List<String> payloadColumnNames = Lists.newArrayList();
        payloadColumnNames.add(PAYLOAD_KEY_FOR_PARAMS);
        payloadColumnNames.add(PAYLOAD_KEY_FOR_SCRATCHPAD);
        dbDAO = new GeneralProtobufDAODynamoDB(LOGGER, dynamoDBClient, tableName, HASH_KEY, Optional.<String>absent(), payloadColumnNames);
    }


    @Override
    public Optional<OnlineHmmPriors> getModelPriorsByAccountId(Long accountId) {
        final Map<String, Map<String,byte[]>> results = dbDAO.getBySingleKeyNoRangeKey(accountId.toString());

        final Map<String,byte[]> allColumns = results.get(accountId.toString());

        if (allColumns == null) {
            //TODO logging
            return Optional.absent();
        }

        final byte [] data = allColumns.get(PAYLOAD_KEY_FOR_PARAMS);

        if (data == null) {
            //TODO logging
            return Optional.absent();
        }


        return OnlineHmmPriors.createFromProtoBuf(data);

    }

    @Override
    public boolean updateModelPriors(final Long accountId,final  OnlineHmmPriors priors) {
        final Map<String,byte []> payloads = Maps.newHashMap();

        payloads.put(PAYLOAD_KEY_FOR_PARAMS,priors.serializeToProtobuf());

        return dbDAO.put(accountId.toString(),"",payloads);
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient) {
        return GeneralProtobufDAODynamoDB.createTable(tableName, HASH_KEY, Optional.<String>absent(), dynamoDBClient, 1, 1);
    }
}
