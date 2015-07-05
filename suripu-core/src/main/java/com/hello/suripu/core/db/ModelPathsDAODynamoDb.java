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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.ModelPathsDAO;
import com.hello.suripu.core.models.BayesNetHmmModelResult;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
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
public class ModelPathsDAODynamoDb implements ModelPathsDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(ModelPathsDAODynamoDb.class);

    public static final String HASH_KEY = "account_id";
    public static final String RANGE_KEY = "date";
    public static final String PAYLOAD_KEY = "model_result";

    private final AmazonDynamoDB dynamoDBClient;

    private final String tableName;

    public ModelPathsDAODynamoDb(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Override
    public Optional<BayesNetHmmModelResult> getResultByAccountAndDay(final Long accountId, final DateTime dateLocalUTCMidnight) {

        final String dateAsString = DateTimeUtil.dateToYmdString(dateLocalUTCMidnight);

        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withS(dateAsString));

        queryConditions.put(RANGE_KEY, selectDateCondition);

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
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items == null) {
            LOGGER.error("DynamoDB query did not return anything for account_id {} on table {}",accountId,this.tableName);
            return Optional.absent();
        }


        //get first item
        byte[] protoData = null;

        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final Long accountID = Long.valueOf(item.get(HASH_KEY).getN());

            final ArrayList<Timeline> eventsWithAllTypes = new ArrayList<>();

            final ByteBuffer byteBuffer = item.get(PAYLOAD_KEY).getB();

            protoData = byteBuffer.array();

            break;
        }

        if (protoData == null) {
            return Optional.absent();
        }

        return BayesNetHmmModelResult.createFromProtobuf(protoData);
    }

    @Override
    public boolean setResultByAccountAndDay(Long accountId, DateTime dateLocalUTCMidnight, BayesNetHmmModelResult modelResult) {
        final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();

        final String date = DateTimeUtil.dateToYmdString(dateLocalUTCMidnight);
        keyValueMap.put(HASH_KEY, new AttributeValue().withN(String.valueOf(accountId)));
        keyValueMap.put(RANGE_KEY, new AttributeValue().withS(date));
        keyValueMap.put(PAYLOAD_KEY, new AttributeValue().withB(ByteBuffer.wrap(modelResult.toProtobuf())));

        final PutItemRequest request = new PutItemRequest()
                .withTableName(this.tableName)
                .withItem(keyValueMap);

        try {
            final PutItemResult result = this.dynamoDBClient.putItem(request);
        }catch (AmazonServiceException awsException){
            LOGGER.error("Server exception {} while saving {} result for account {}",
                    awsException.getMessage(),
                    date,
                    accountId);
            return false;
        }catch (AmazonClientException acExp){
            LOGGER.error("AmazonClientException exception {} while saving {} result for account {}",
                    acExp.getMessage(),
                    date,
                    accountId);
            return false;

        }

        return true;

    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ModelPathsDAODynamoDb.HASH_KEY).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(ModelPathsDAODynamoDb.RANGE_KEY).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(HASH_KEY).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(RANGE_KEY).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(2L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
