package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.yammer.dropwizard.config.Configuration;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 6/15/15.
 */
public class ConfigurationDAODynamoDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationDAODynamoDB.class);
    private static final String NAME_ATTRIBUTE_NAME = "name";
    private static final String NAMESPACE_ATTRIBUTE_NAME = "ns";
    private static final String VALUE_ATTRIBUTE_NAME = "config_values";
    private static final String TIMESTAMP_ATTRIBUTE_NAME = "timestamp";
    public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    public final Configuration configuration;
    private final String namespace;
    private final Table table;

    public ConfigurationDAODynamoDB(final Configuration configuration, final AmazonDynamoDB amazonDynamoDB, final String tableName, final String namespace) {
        this.configuration = configuration;
        this.table = new DynamoDB(amazonDynamoDB).getTable(tableName);
        this.namespace =  namespace;
    }

    public Configuration getData() {
        LOGGER.trace("Calling getData");

        final GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("ns", namespace, "name", configuration.getClass().getSimpleName());
        spec.withProjectionExpression(VALUE_ATTRIBUTE_NAME);

        final ObjectMapper mapper = new ObjectMapper();
        try
        {
            final String storedConfigString = table.getItem(spec).getJSON(VALUE_ATTRIBUTE_NAME);
            final ObjectReader reader = mapper.reader(configuration.getClass());
            return reader.readValue(storedConfigString);
        } catch (JsonGenerationException e) {
            LOGGER.error("Failed to get data.");
        } catch (IOException ioe) {
            LOGGER.error("Failed to get data.");
        } catch (NullPointerException npe) {
            LOGGER.error("getData() failed. NPE.");
        }

        //No stored config exists, persist the loaded config to DynamoDB
        LOGGER.info("Configuration read from Dynamo failed. Attempting to persist loaded configuration values.");

        final String[] ignorableFieldNames = { "httpConfiguration", "loggingConfiguration", "http", "logging" };
        FilterProvider filters = new SimpleFilterProvider()
                .addFilter("filter properties by name",
                        SimpleBeanPropertyFilter.serializeAllExcept(
                                ignorableFieldNames));
        final ObjectWriter objWriter = mapper.writer(filters);
        try {
            final String configJSON  = objWriter.writeValueAsString(configuration);
            put(configJSON);
        } catch (JsonProcessingException jpe) {
            LOGGER.error("Failed to serialize configuration.");
        }

        return configuration;
    }

    public void put(final String jsonString) {
        Item item = new Item()
                .withPrimaryKey("ns", namespace, "name", configuration.getClass().getSimpleName())
                .withJSON(VALUE_ATTRIBUTE_NAME, jsonString);

        PutItemOutcome outcome = table.putItem(item);
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(NAMESPACE_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(NAME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(NAMESPACE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(NAME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }
}

