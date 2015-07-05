package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.BayesNetHmmModelPrior;
import com.hello.suripu.core.models.BayesNetHmmModelResult;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 7/5/15.
 */
public class ModelPriorsDAODynamoDB implements ModelPriorsDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(ModelPriorsDAODynamoDB.class);

    public static final String HASH_KEY = "account_id";
    public static final String RANGE_KEY = "model_id";
    public static final String PAYLOAD_KEY = "prior";

    private final AmazonDynamoDB dynamoDBClient;

    private final String tableName;

    public ModelPriorsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Override
    public List<BayesNetHmmModelPrior> getModelPriorsByAccountId(final Long accountId, final List<String> modelNames) {

        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final List<BayesNetHmmModelPrior> results = Lists.newArrayList();

        List<AttributeValue> modelNameList = Lists.newArrayList();
        for (final String name : modelNames) {
            modelNameList.add(new AttributeValue().withS(name));
        }

        final Condition selectModelConditions = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(modelNameList);

        queryConditions.put(RANGE_KEY, selectModelConditions);

        // AND account_id = :account_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(HASH_KEY, selectAccountIdCondition);

        //put all attributes that you want back from the server in this thing
        final Collection<String> targetAttributeSet = new HashSet<String>();

        Collections.addAll(targetAttributeSet,
                HASH_KEY,
                RANGE_KEY,
                PAYLOAD_KEY
        );



        // Perform query
        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet);


        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items == null) {
            LOGGER.error("DynamoDB query did not return anything for account_id {} on table {}",accountId,this.tableName);
            return results;
        }


        //get first item
        byte[] protoData = null;

        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final ByteBuffer byteBuffer = item.get(PAYLOAD_KEY).getB();

            results.addAll(BayesNetHmmModelPrior.createListFromProtbuf(byteBuffer.array()));
        }

        return results;
    }

    @Override
    public boolean updateModelPriorsByAccountId(final Long accountId,final List<BayesNetHmmModelPrior> priors) {

        final Map<String, byte[]> protobufs = BayesNetHmmModelPrior.getProtobufsByModelId(priors);


        //TODO replace with a batch put
        for (final String modelId : protobufs.keySet()) {

            final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();

            keyValueMap.put(HASH_KEY, new AttributeValue().withN(String.valueOf(accountId)));
            keyValueMap.put(RANGE_KEY, new AttributeValue().withS(modelId));
            keyValueMap.put(PAYLOAD_KEY, new AttributeValue().withB(ByteBuffer.wrap(protobufs.get(modelId))));

            final PutItemRequest request = new PutItemRequest()
                    .withTableName(this.tableName)
                    .withItem(keyValueMap);

            try {
                final PutItemResult result = this.dynamoDBClient.putItem(request);
            } catch (AmazonServiceException awsException) {
                LOGGER.error("Server exception {} while saving {} result for account {}",
                        awsException.getMessage(),
                        modelId,
                        accountId);
                return false;
            } catch (AmazonClientException acExp) {
                LOGGER.error("AmazonClientException exception {} while saving {} result for account {}",
                        acExp.getMessage(),
                        modelId,
                        accountId);
                return false;

            }
        }

        return true;

    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ModelPathsDAODynamoDB.HASH_KEY).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(ModelPathsDAODynamoDB.RANGE_KEY).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(HASH_KEY).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(RANGE_KEY).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(5L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
