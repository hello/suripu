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
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.BayesNetHmmModelPrior;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
    public static final String RANGE_KEY = "date";
    public static final String PAYLOAD_KEY = "prior";
    public static final String CURRENT_RANGE_KEY = "current";

    private final AmazonDynamoDB dynamoDBClient;

    private final String tableName;

    public ModelPriorsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Override
    //this will return the prior for that date, or the "current" model priors
    public List<BayesNetHmmModelPrior> getModelPriorsByAccountIdAndDate(final Long accountId,final DateTime dateLocalUTC) {

        final String dateString = DateTimeUtil.dateToYmdString(dateLocalUTC);

        final Map<Long, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final List<BayesNetHmmModelPrior> results = Lists.newArrayList();

        final List<AttributeValue> attributeValueList = Lists.newArrayList();
        attributeValueList.add(new AttributeValue().withS(dateString));
        attributeValueList.add(new AttributeValue().withS(CURRENT_RANGE_KEY));


        final Condition selectModelConditions = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(attributeValueList);

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
                .withAttributesToGet(targetAttributeSet)
                .withLimit(2);


        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items == null) {
            LOGGER.error("DynamoDB query did not return anything for account_id {} on table {}",accountId,this.tableName);
            return results;
        }



        if (items.size() == 2) {
            //prior already exists for this day
            for(final Map<String, AttributeValue> item : items) {
                if (!item.keySet().containsAll(targetAttributeSet)) {
                    LOGGER.error("Missing field in item {}", item);
                    return results;
                }

                //skip current key, use the one for the day
                if (item.get(RANGE_KEY).equals(CURRENT_RANGE_KEY)) {
                    continue;
                }

                final ByteBuffer byteBuffer = item.get(PAYLOAD_KEY).getB();

                results.addAll(BayesNetHmmModelPrior.createListFromProtbuf(byteBuffer.array()));
            }
        }
        else if (items.size() == 1) {
            //only current exists for this day
            final Map<String, AttributeValue> item = items.get(0);

            if (!item.keySet().containsAll(targetAttributeSet)) {
                LOGGER.error("Missing field in item {}", item);
                return results;
            }

            final ByteBuffer byteBuffer = item.get(RANGE_KEY).getB();
            final String rangeKey = item.get(PAYLOAD_KEY).getS();

            if (rangeKey.equals(CURRENT_RANGE_KEY)) {
                results.addAll(BayesNetHmmModelPrior.createListFromProtbuf(byteBuffer.array()));
            }
            else {
                LOGGER.error("current range key not found when it should have been, instead it was {} for account_id {} requested for date {}",rangeKey,accountId,dateString);
            }
        }
        else if (items.size() == 0) {
            //first time ever for this user
            LOGGER.info("no model prior retrieved for account_id {} for date {}",accountId,dateString);
        }
        else {
            //this should never happen
            LOGGER.error("got more than zero, one, or two results for account_id {} requested for date {}",accountId,dateString);
        }

        return results;
    }

    @Override
    public boolean updateModelPriorsByAccountIdForDate(final Long accountId,final DateTime dateLocalUTC, final List<BayesNetHmmModelPrior> priors) {

        final String dateString = DateTimeUtil.dateToYmdString(dateLocalUTC);


        final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();

        keyValueMap.put(HASH_KEY, new AttributeValue().withN(String.valueOf(accountId)));
        keyValueMap.put(RANGE_KEY, new AttributeValue().withS(dateString));
        keyValueMap.put(PAYLOAD_KEY, new AttributeValue().withB(ByteBuffer.wrap(BayesNetHmmModelPrior.listToProtobuf(priors))));

        final PutItemRequest request = new PutItemRequest()
                .withTableName(this.tableName)
                .withItem(keyValueMap);

        try {
            final PutItemResult result = this.dynamoDBClient.putItem(request);
        } catch (AmazonServiceException awsException) {
            LOGGER.error("Server exception {} while saving {} result for account {}",
                    awsException.getMessage(),
                    dateLocalUTC,
                    accountId);
            return false;
        } catch (AmazonClientException acExp) {
            LOGGER.error("AmazonClientException exception {} while saving {} result for account {}",
                    acExp.getMessage(),
                    dateLocalUTC,
                    accountId);
            return false;

        }


        return true;

    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ModelPriorsDAODynamoDB.HASH_KEY).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(ModelPriorsDAODynamoDB.RANGE_KEY).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(HASH_KEY).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(RANGE_KEY).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(5L)
                .withWriteCapacityUnits(5L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
