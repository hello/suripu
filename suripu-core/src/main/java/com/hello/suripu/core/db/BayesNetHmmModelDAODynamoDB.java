package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.HmmBayesNetData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



public class BayesNetHmmModelDAODynamoDB implements BayesNetModelDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(BayesNetHmmModelDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    private static final String HASH_KEY = "account_id";
    private static final String RANGE_KEY = "create_date";
    private static final String PAYLOAD_KEY = "model";

    private static final long DEFAULT_ACCOUNT_ID = -1;

    private static final long DEFAULT_ACCOUNT_HASH_RANGE = 5;


    public BayesNetHmmModelDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    //return range between -1 and -DEFAULT_ACCOUNT_HASH_RANGE
    //most users will be on default account, so this is how we spread out the load on the queries
    private static long getDefaultAccountId(final long accountId) {
        return DEFAULT_ACCOUNT_ID - (accountId % DEFAULT_ACCOUNT_HASH_RANGE);
    }

    @Override
    public HmmBayesNetData getLatestModelForDate(final Long accountId, final DateTime dateTimeLocalUTC, final Optional<UUID> uuidForLogger) {

        final String dateString = DateTimeUtil.dateToYmdString(dateTimeLocalUTC);
        byte [] protobufData = null;

        //get with my account ID
        ImmutableMap<Long, byte []> queryResult = this.getLatestModelForDateInternal(accountId,dateString);

        if (queryResult.containsKey(accountId)) {
            protobufData = queryResult.get(accountId);
        }
        else {
            //if that failed, then try with default account ID
            final long defaultAccountId = getDefaultAccountId(accountId);


            queryResult = this.getLatestModelForDateInternal(defaultAccountId,dateString);

            if (queryResult.containsKey(defaultAccountId)) {
                protobufData = queryResult.get(defaultAccountId);
            }
            else {
                LOGGER.error("could not find entry for accounts {} and {}. This should never have happened",defaultAccountId,accountId);
            }
        }


        if (protobufData != null) {
            //decode blob if it exists
            final HmmBayesNetData deserialization = new HmmBayesNetData(uuidForLogger);

            deserialization.deserialize(protobufData);

            return deserialization;

        }

        //default object will be invalid
        return new HmmBayesNetData(uuidForLogger);


    }

    /*
     * Get most up-to-date model for a given date
     */
    private ImmutableMap<Long, byte []> getLatestModelForDateInternal(long accountId, final String dateString){

        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = Maps.newHashMap();

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LE.toString())
                .withAttributeValueList(new AttributeValue().withS(dateString));

        queryConditions.put(RANGE_KEY, selectDateCondition);

        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(HASH_KEY, selectAccountIdCondition);

        //put all attributes that you want back from the server in this thing
        final Collection<String> targetAttributeSet = Sets.newHashSet();

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
                .withScanIndexForward(false)
                .withLimit(1);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items == null) {
            LOGGER.error("DynamoDB query did not return anything for account_id {} on table {}",accountId,this.tableName);
            return ImmutableMap.copyOf(finalResult);
        }


        //iterate through items
        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final Long accountID = Long.valueOf(item.get(HASH_KEY).getN());

            final ByteBuffer byteBuffer = item.get(PAYLOAD_KEY).getB();
            final byte[] protoData = byteBuffer.array();

            finalResult.put(accountID,protoData);
        }

        return ImmutableMap.copyOf(finalResult);
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(BayesNetHmmModelDAODynamoDB.HASH_KEY).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(BayesNetHmmModelDAODynamoDB.RANGE_KEY).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(BayesNetHmmModelDAODynamoDB.HASH_KEY).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(BayesNetHmmModelDAODynamoDB.RANGE_KEY).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(5L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }
}
