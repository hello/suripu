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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.yammer.dropwizard.json.GuavaExtrasModule;
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
 * Created by benjo on 2/24/15.
 */

/*
   dynamo db table has
   account_id, model_create_date, model_protobuf_blob

   Hash Attribute Name: account_id
   Range Attribute Name: model_create_date







 */

public class SleepHmmDAODynamoDB implements SleepHmmDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    private static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    private static final String CREATED_DATE_ATTRIBUTE_NAME = "create_date";
    private static final String DATA_BLOB_ATTRIBUTE_NAME = "model";

    private static final long DEFAULT_ACCOUNT_ID = -1;

    private static final long DEFAULT_ACCOUNT_HASH_RANGE = 5;

    private final String JSON_CHARSET = "UTF-8";

    private static ObjectMapper mapper = new ObjectMapper();

    public SleepHmmDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new GuavaExtrasModule());
        mapper.registerModule(new JodaModule());
    }

    //return range between -1 and -DEFAULT_ACCOUNT_HASH_RANGE
    //most users will be on default account, so this is how we spread out the load on the queries
    private static long getDefaultAccountId(final long accountId) {
        return DEFAULT_ACCOUNT_ID - (accountId % DEFAULT_ACCOUNT_HASH_RANGE);
    }

    @Override
    public Optional<SleepHmmWithInterpretation> getLatestModelForDate(final long accountId, final long timeOfInterestMillis) {

        byte [] sleepHmmBlob = null;

        //get with my account ID
        ImmutableMap<Long, byte []> queryResult = this.getLatestModelForDateInternal(accountId,timeOfInterestMillis);

        if (queryResult.containsKey(accountId)) {
            sleepHmmBlob = queryResult.get(accountId);
        }
        else {
            //if that failed, then try with default account ID
            final long defaultAccountId = getDefaultAccountId(accountId);

            LOGGER.info("attempting to get hmm for default account {} from account {}",defaultAccountId,accountId);

            queryResult = this.getLatestModelForDateInternal(defaultAccountId,timeOfInterestMillis);

            if (queryResult.containsKey(defaultAccountId)) {
                sleepHmmBlob = queryResult.get(defaultAccountId);
                LOGGER.info("got hmm for default account {} from account {}",defaultAccountId,accountId);
            }
        }


        Optional<SleepHmmWithInterpretation> optionalModel = Optional.absent();

        if (sleepHmmBlob != null) {
            //decode blob if it exists

            try {

                final SleepHmmProtos.SleepHmmModelSet hmmModelData = SleepHmmProtos.SleepHmmModelSet.parseFrom(sleepHmmBlob);

                optionalModel = SleepHmmWithInterpretation.createModelFromProtobuf(hmmModelData);

            }
            catch (InvalidProtocolBufferException e) {
                LOGGER.error(e.toString());
            }
        }

        return optionalModel;


    }

    /*
     * Get most up-to-date model for a given date
     */
    private ImmutableMap<Long, byte []> getLatestModelForDateInternal(long accountId, long timeOfInterestMillis){

        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(timeOfInterestMillis)));

        queryConditions.put(CREATED_DATE_ATTRIBUTE_NAME, selectDateCondition);

        // AND account_id = :account_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        //put all attributes that you want back from the server in this thing
        final Collection<String> targetAttributeSet = new HashSet<String>();

        Collections.addAll(targetAttributeSet,
                ACCOUNT_ID_ATTRIBUTE_NAME,
                CREATED_DATE_ATTRIBUTE_NAME,
                DATA_BLOB_ATTRIBUTE_NAME
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
            return ImmutableMap.copyOf(finalResult);
        }


        //iterate through items
        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final Long dateInMillis = Long.valueOf(item.get(CREATED_DATE_ATTRIBUTE_NAME).getN());
            final Long accountID = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());

            final ArrayList<Timeline> eventsWithAllTypes = new ArrayList<>();

            final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();
            final byte[] protoData = byteBuffer.array();

            finalResult.put(accountID,protoData);


        }



        return ImmutableMap.copyOf(finalResult);
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SleepHmmDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(SleepHmmDAODynamoDB.CREATED_DATE_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SleepHmmDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(SleepHmmDAODynamoDB.CREATED_DATE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
