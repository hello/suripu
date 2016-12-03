package com.hello.suripu.coredropwizard.metrics;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.dynamodbv2.waiters.AmazonDynamoDBWaiters;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.List;
import java.util.Map;

public class InstrumentedDynamoDBClient implements AmazonDynamoDB {

    static final MetricRegistry metrics = new MetricRegistry();
    private final Timer getItem;
    private final Timer putItem;
    private final Timer deleteItem;
    private final Timer batchGetItem;
    private final Timer query;
    private final AmazonDynamoDB client;

    public InstrumentedDynamoDBClient(final AmazonDynamoDB amazonDynamoDB, final Class<?> klass) {

        this.client = amazonDynamoDB;
        this.getItem = metrics.timer(MetricRegistry.name(klass, "getItem"));
        this.putItem = metrics.timer(MetricRegistry.name(klass, "putItem"));
        this.deleteItem = metrics.timer(MetricRegistry.name(klass, "deleteItem"));
        this.batchGetItem = metrics.timer(MetricRegistry.name(klass, "batchGetItem"));
        this.query = metrics.timer(MetricRegistry.name(klass, "query"));
    }

    @Override
    public GetItemResult getItem(GetItemRequest getItemRequest) throws AmazonServiceException, AmazonClientException {
        final Timer.Context context = getItem.time();
        try {
            return client.getItem(getItemRequest);
        } finally {
            context.stop();
        }
    }

    @Override
    public PutItemResult putItem(PutItemRequest putItemRequest) throws AmazonServiceException, AmazonClientException {
        final Timer.Context context = putItem.time();
        try {
            return client.putItem(putItemRequest);
        } finally {
            context.stop();
        }
    }

    @Override
    public DeleteItemResult deleteItem(DeleteItemRequest deleteItemRequest) throws AmazonServiceException, AmazonClientException {
        final Timer.Context context = deleteItem.time();
        try {
            return client.deleteItem(deleteItemRequest);
        } finally {
            context.stop();
        }
    }

    @Override
    public void setEndpoint(String s) throws IllegalArgumentException {
        client.setEndpoint(s);
    }

    @Override
    public void setRegion(Region region) throws IllegalArgumentException {
        client.setRegion(region);
    }

    @Override
    public ScanResult scan(ScanRequest scanRequest) throws AmazonServiceException, AmazonClientException {
        return client.scan(scanRequest);
    }

    @Override
    public UpdateTableResult updateTable(UpdateTableRequest updateTableRequest) throws AmazonServiceException, AmazonClientException {
        throw new AmazonClientException("updateTable: Operation not supported");
    }

    @Override
    public DeleteTableResult deleteTable(DeleteTableRequest deleteTableRequest) throws AmazonServiceException, AmazonClientException {
        throw new AmazonClientException("deleteTable: Operation not supported");
    }

    @Override
    public BatchWriteItemResult batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) throws AmazonServiceException, AmazonClientException {
        return client.batchWriteItem(batchWriteItemRequest);
    }

    @Override
    public DescribeTableResult describeTable(DescribeTableRequest describeTableRequest) throws AmazonServiceException, AmazonClientException {
        return client.describeTable(describeTableRequest);
    }


    @Override
    public CreateTableResult createTable(CreateTableRequest createTableRequest) throws AmazonServiceException, AmazonClientException {
        return client.createTable(createTableRequest);
    }

    @Override
    public QueryResult query(QueryRequest queryRequest) throws AmazonServiceException, AmazonClientException {
        final Timer.Context context = query.time();
        try {
            return client.query(queryRequest);
        }   finally {
            context.stop();
        }

    }

    @Override
    public ListTablesResult listTables(ListTablesRequest listTablesRequest) throws AmazonServiceException, AmazonClientException {
        return client.listTables(listTablesRequest);
    }

    @Override
    public UpdateItemResult updateItem(UpdateItemRequest updateItemRequest) throws AmazonServiceException, AmazonClientException {
        return client.updateItem(updateItemRequest);
    }

    @Override
    public BatchGetItemResult batchGetItem(BatchGetItemRequest batchGetItemRequest) throws AmazonServiceException, AmazonClientException {
        return client.batchGetItem(batchGetItemRequest);
    }

    @Override
    public ListTablesResult listTables() throws AmazonServiceException, AmazonClientException {
        return client.listTables();
    }

    @Override
    public ScanResult scan(String tableName, List<String> attributesToGet) throws AmazonServiceException, AmazonClientException {
        return client.scan(tableName, attributesToGet);
    }

    @Override
    public ScanResult scan(String tableName, Map<String,Condition> scanFilter) throws AmazonServiceException, AmazonClientException {
        return client.scan(tableName, scanFilter);
    }

    @Override
    public ScanResult scan(String tableName, List<String> attributesToGet, Map<String,Condition> scanFilter) throws AmazonServiceException, AmazonClientException {
        return client.scan(tableName, attributesToGet, scanFilter);
    }

    @Override
    public UpdateTableResult updateTable(String tableName, ProvisionedThroughput provisionedThroughput) throws AmazonServiceException, AmazonClientException {
        return client.updateTable(tableName, provisionedThroughput);
    }

    @Override
    public DeleteTableResult deleteTable(String tableName) throws AmazonServiceException, AmazonClientException {
        return client.deleteTable(tableName);
    }

    @Override
    public DescribeLimitsResult describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        return client.describeLimits(describeLimitsRequest);
    }

    @Override
    public BatchWriteItemResult batchWriteItem(Map<String,List<WriteRequest>> requestItems) throws AmazonServiceException, AmazonClientException {
        return client.batchWriteItem(requestItems);
    }

    @Override
    public DescribeTableResult describeTable(String tableName) throws AmazonServiceException, AmazonClientException {
        return client.describeTable(tableName);
    }

    @Override
    public GetItemResult getItem(String tableName, Map<String,AttributeValue> key) throws AmazonServiceException, AmazonClientException {
        return client.getItem(tableName, key);
    }

    @Override
    public GetItemResult getItem(String tableName, Map<String,AttributeValue> key, Boolean consistentRead) throws AmazonServiceException, AmazonClientException {
        return client.getItem(tableName, key, consistentRead);
    }

    @Override
    public DeleteItemResult deleteItem(String tableName, Map<String,AttributeValue> key) throws AmazonServiceException, AmazonClientException {
        return client.deleteItem(tableName, key);
    }

    @Override
    public DeleteItemResult deleteItem(String tableName, Map<String,AttributeValue> key, String returnValues) throws AmazonServiceException, AmazonClientException {
        return client.deleteItem(tableName, key, returnValues);
    }

    @Override
    public CreateTableResult createTable(List<AttributeDefinition> attributeDefinitions, String tableName, List<KeySchemaElement> keySchema, ProvisionedThroughput provisionedThroughput) throws AmazonServiceException, AmazonClientException {
        return client.createTable(attributeDefinitions, tableName, keySchema, provisionedThroughput);
    }

    @Override
    public PutItemResult putItem(String tableName, Map<String,AttributeValue> item) throws AmazonServiceException, AmazonClientException {
        return client.putItem(tableName, item);
    }

    @Override
    public PutItemResult putItem(String tableName, Map<String,AttributeValue> item, String returnValues) throws AmazonServiceException, AmazonClientException {
        return client.putItem(tableName, item, returnValues);
    }

    @Override
    public ListTablesResult listTables(String exclusiveStartTableName) throws AmazonServiceException, AmazonClientException {
        return client.listTables(exclusiveStartTableName);
    }

    @Override
    public ListTablesResult listTables(String exclusiveStartTableName, Integer limit) throws AmazonServiceException, AmazonClientException {
        return client.listTables(exclusiveStartTableName, limit);
    }

    @Override
    public ListTablesResult listTables(Integer limit) throws AmazonServiceException, AmazonClientException {
        return client.listTables(limit);
    }

    @Override
    public UpdateItemResult updateItem(String tableName, Map<String,AttributeValue> key, Map<String,AttributeValueUpdate> attributeUpdates) throws AmazonServiceException, AmazonClientException {
        return client.updateItem(tableName, key, attributeUpdates);
    }

    @Override
    public UpdateItemResult updateItem(String tableName, Map<String,AttributeValue> key, Map<String,AttributeValueUpdate> attributeUpdates, String returnValues) throws AmazonServiceException, AmazonClientException {
        return client.updateItem(tableName, key, attributeUpdates, returnValues);
    }

    @Override
    public BatchGetItemResult batchGetItem(Map<String,KeysAndAttributes> requestItems, String returnConsumedCapacity) throws AmazonServiceException, AmazonClientException {
        return client.batchGetItem(requestItems, returnConsumedCapacity);
    }

    @Override
    public BatchGetItemResult batchGetItem(Map<String, KeysAndAttributes> requestItems) throws AmazonServiceException, AmazonClientException {
        final Timer.Context context = batchGetItem.time();
        try {
            return client.batchGetItem(requestItems);
        } finally {
            context.stop();
        }
    }

    @Override
    public void shutdown() {
        client.shutdown();
    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return client.getCachedResponseMetadata(amazonWebServiceRequest);
    }

    @Override
    public AmazonDynamoDBWaiters waiters() {
        return client.waiters();
    }
}
