package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 4/6/15.
 */
public class TimelineLogDAODynamoDB implements  TimelineLogDAO{

    /* hash key = accountId
     * range_key = target_date + alg name
      * */
    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "accountId";
    public static final String DATEALG_ATTRIBUTE_NAME = "date_and_alg";
    public static final String TIMELINE_DATA_ATTRIBUTE_NAME = "timeline_data";
    public static final String VERSION_ATTRIBUTE_NAME = "version";
    public static final String CREATEDATE_ATTRIBUTE_NAME = "created_date";


    public static final long DEFAULT_ACCOUNT_ID = -1;

    public final String JSON_CHARSET = "UTF-8";

    private static ObjectMapper mapper = new ObjectMapper();

    public TimelineLogDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new GuavaExtrasModule());
        mapper.registerModule(new JodaModule());
    }


    @Override
    public ImmutableList<TimelineLog> getLogsForUserAndDay(long accountId, DateTime day,Optional<Integer> numDaysAfterday) {


        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final String dayString = DateTimeUtil.dateToYmdString(day);
        String tomorrowString = DateTimeUtil.dateToYmdString(day.plusDays(1));

        if (numDaysAfterday.isPresent()) {
            tomorrowString = DateTimeUtil.dateToYmdString(day.plusDays(numDaysAfterday.get()));
        }

        //////////////////////////////////////////////////////
        // building the query
        //accountId == accountId :: HASH KEY
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);



        //date_and_alg, really just date range
        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(dayString),
                        new AttributeValue().withS(tomorrowString));

        queryConditions.put(DATEALG_ATTRIBUTE_NAME, selectDateCondition);


        //////////////////////////////////////////////
        //put all attributes that you want back from the server in this thing
        final Collection<String> targetAttributeSet = new HashSet<String>();

        Collections.addAll(targetAttributeSet,
                ACCOUNT_ID_ATTRIBUTE_NAME,
                DATEALG_ATTRIBUTE_NAME,
                CREATEDATE_ATTRIBUTE_NAME,
                TIMELINE_DATA_ATTRIBUTE_NAME,
                VERSION_ATTRIBUTE_NAME
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
            LOGGER.error("DynamoDB query did not return anything for accountId {} on table {}",accountId,this.tableName);
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        List<TimelineLog> result = new ArrayList<>();

        //iterate through items
        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final String dateAndAlg = item.get(DATEALG_ATTRIBUTE_NAME).getS();
            final Long resultAccountId = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
            final String createDateString = item.get(CREATEDATE_ATTRIBUTE_NAME).getS();
            final String version = item.get(VERSION_ATTRIBUTE_NAME).getS();

            final DateTime createdDate = DateTimeUtil.ymdStringToDateTime(createDateString);

            //date_alg ---> date,  alg
            final String [] dateAndAlgList = dateAndAlg.split("_");

            if (dateAndAlgList.length < 2) {
                continue;
            }



            final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(dateAndAlgList[0]);
            final String algorithm = dateAndAlgList[1];

            result.add(new TimelineLog(resultAccountId,algorithm,createdDate,targetDate,version));

        }


        return ImmutableList.copyOf(result);
    }

    @Override
    public boolean putTimelineLog(final TimelineLog logdata) {
        final HashMap<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now();

        final String dateString = DateTimeUtil.dateToYmdString(logdata.targetDate);
        final String createdString = DateTimeUtil.dateToYmdString(logdata.createdDate);
        final String dateAlgString = String.format("%s_%s",dateString,logdata.algorithm);

        final HashMap<String, AttributeValue> keys = new HashMap<>();
        keys.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(logdata.accountId)));
        keys.put(DATEALG_ATTRIBUTE_NAME, new AttributeValue().withS(dateAlgString));
        keys.put(CREATEDATE_ATTRIBUTE_NAME,new AttributeValue().withS(createdString));
        keys.put(VERSION_ATTRIBUTE_NAME,new AttributeValue().withS(logdata.version));
        keys.put(TIMELINE_DATA_ATTRIBUTE_NAME,new AttributeValue().withS("{}"));


        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(this.tableName)
                .withKey(keys)
                .withAttributeUpdates(items)
                .withReturnValues(ReturnValue.ALL_NEW);

        try {
            final UpdateItemResult result = this.dynamoDBClient.updateItem(updateItemRequest);
        }catch (AmazonServiceException awsException){
            LOGGER.error("Server exception {} while saving {} result for account {}",
                    awsException.getMessage(),
                    dateAlgString,
                    logdata.accountId);
            return false;
        }catch (AmazonClientException acExp){
            LOGGER.error("AmazonClientException exception {} while saving {} result for account {}",
                    acExp.getMessage(),
                    dateAlgString,
                    logdata.accountId);
            return false;
        }

        return true;

    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(TimelineLogDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TimelineLogDAODynamoDB.DATEALG_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(TimelineLogDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TimelineLogDAODynamoDB.DATEALG_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
