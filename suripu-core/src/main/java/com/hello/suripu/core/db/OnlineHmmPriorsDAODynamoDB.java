package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.OnlineHmmProtos;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class OnlineHmmPriorsDAODynamoDB implements OnlineHmmPriorsDAO {
    private final static String HASH_KEY = "account_id";
    private final static String PAYLOAD_KEY = "model_params";
    private final static Logger LOGGER = LoggerFactory.getLogger(OnlineHmmPriorsDAODynamoDB.class);

    private final GeneralProtobufDAODynamoDB dbDAO;


    public OnlineHmmPriorsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        dbDAO = new GeneralProtobufDAODynamoDB(LOGGER, dynamoDBClient, tableName, HASH_KEY, Optional.<String>absent(), PAYLOAD_KEY);
    }


    @Override
    public Optional<OnlineHmmPriors> getModelPriorsByAccountId(Long accountId) {
        final Map<String, byte[]> results = dbDAO.getBySingleKeyNoRangeKey(accountId.toString());

        final byte [] data = results.get(accountId.toString());

        if (data == null) {
            return Optional.absent();
        }

        return OnlineHmmPriors.createFromProtoBuf(data);
        
    }

    @Override
    public boolean updateModelPriors(final Long accountId,final  OnlineHmmPriors priors) {
        return dbDAO.put(accountId.toString(),"",priors.serializeToProtobuf());
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient) {
        return GeneralProtobufDAODynamoDB.createTable(tableName, HASH_KEY, Optional.<String>absent(), dynamoDBClient, 1, 1);
    }
}
