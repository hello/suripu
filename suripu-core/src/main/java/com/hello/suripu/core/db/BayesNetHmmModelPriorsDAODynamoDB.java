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
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.BayesNetHmmMultipleModelsPriors;
import com.hello.suripu.core.models.BayesNetHmmSingleModelPrior;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
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
public class BayesNetHmmModelPriorsDAODynamoDB implements BayesNetHmmModelPriorsDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(BayesNetHmmModelPriorsDAODynamoDB.class);

    public static final String HASH_KEY = "account_id";
    public static final String RANGE_KEY = "date";
    public static final String PAYLOAD_KEY = "prior";

    private final AmazonDynamoDB dynamoDBClient;

    private final String tableName;

    private final Collection<String> targetAttributeSet;

    public BayesNetHmmModelPriorsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        //put all attributes that you want back from the server in this thing
        targetAttributeSet = new HashSet<String>();

        Collections.addAll(targetAttributeSet,
                HASH_KEY,
                RANGE_KEY,
                PAYLOAD_KEY
        );
    }

    private QueryResult doQuery(final Long accountId,List<AttributeValue> matchKeys) {

        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final Condition selectRangeCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(matchKeys);

        queryConditions.put(HASH_KEY, selectAccountIdCondition);
        queryConditions.put(RANGE_KEY, selectRangeCondition);





        // Perform query
        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1);


        return this.dynamoDBClient.query(queryRequest);

    }

    @Override
    //this will return the prior for that date, or the "current" model priors
    public Optional<BayesNetHmmMultipleModelsPriors> getModelPriorsByAccountIdAndDate(final Long accountId,final DateTime dateLocalUTC) {

        final String dateString = DateTimeUtil.dateToYmdString(dateLocalUTC);

        final Map<Long, byte []> finalResult = new HashMap<>();

        final List<BayesNetHmmSingleModelPrior> results = Lists.newArrayList();

        final List<AttributeValue> attributeValueListForDateMatch = Lists.newArrayList();
        attributeValueListForDateMatch.add(new AttributeValue().withS(dateString));

        final List<AttributeValue> attributeValueListForCurrentMatch = Lists.newArrayList();
        attributeValueListForCurrentMatch.add(new AttributeValue().withS(CURRENT_RANGE_KEY));



        final QueryResult queryResult = doQuery(accountId,attributeValueListForDateMatch);

        Map<String,AttributeValue> item = null;

        //did initial query by date return anything?
        if (queryResult.getCount() == 0) {

            //nope.  so let's query for the "current" model for this user
            final QueryResult queryResultForCurrent = doQuery(accountId,attributeValueListForCurrentMatch);

            if (queryResultForCurrent.getCount() == 0) {
                //found nothing.  could be the first time ever for this user.
                LOGGER.info("did not find current nor date prior for user {} on date {}", accountId, dateString);
                return Optional.absent();
            }

            item = queryResultForCurrent.getItems().iterator().next();

        }
        else {
            item = queryResult.getItems().iterator().next();
        }

        if (item == null) {
            LOGGER.error("something impossible happened: item was null");
            return Optional.absent();
        }

        if (!item.keySet().containsAll(targetAttributeSet)) {
            LOGGER.error("Missing field in item {}", item);
            return Optional.absent();
        }

        final String rangeKey = item.get(RANGE_KEY).getS();
        final ByteBuffer byteBuffer = item.get(PAYLOAD_KEY).getB();

        results.addAll(BayesNetHmmSingleModelPrior.createListFromProtbuf(byteBuffer.array()));

        return Optional.of(new BayesNetHmmMultipleModelsPriors(results,rangeKey));


    }

    @Override
    public boolean updateModelPriorsByAccountIdForDate(final Long accountId,final DateTime dateLocalUTC, final List<BayesNetHmmSingleModelPrior> priors) {

        final String dateString = DateTimeUtil.dateToYmdString(dateLocalUTC);


        final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();

        keyValueMap.put(HASH_KEY, new AttributeValue().withN(String.valueOf(accountId)));
        keyValueMap.put(RANGE_KEY, new AttributeValue().withS(dateString));
        keyValueMap.put(PAYLOAD_KEY, new AttributeValue().withB(ByteBuffer.wrap(BayesNetHmmSingleModelPrior.listToProtobuf(priors))));

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
                new KeySchemaElement().withAttributeName(BayesNetHmmModelPriorsDAODynamoDB.HASH_KEY).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(BayesNetHmmModelPriorsDAODynamoDB.RANGE_KEY).withKeyType(KeyType.RANGE)
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
