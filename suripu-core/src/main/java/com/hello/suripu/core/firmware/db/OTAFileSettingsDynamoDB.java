package com.hello.suripu.core.firmware.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.firmware.FirmwareFile;
import com.hello.suripu.core.firmware.HardwareVersion;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.Map;

public class OTAFileSettingsDynamoDB implements OTAFileSettingsDAO {

    private enum AttributeName {
        HW_VERSION("hw_version"),
        LAST_UPDATED_AT("last_updated_at"),
        MD5_CHECKSUM("md5_checksum"),
        JSON_BLOB("json_blob");

        private String value;
        AttributeName(String value) {
            this.value = value;
        }
    }

    private final Table table;
    private final ObjectMapper mapper;

    private OTAFileSettingsDynamoDB(final Table table, ObjectMapper objectMapper) {
        this.table = table;
        this.mapper = objectMapper;
    }

    public static OTAFileSettingsDynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName, final ObjectMapper configuredObjectMapper) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new OTAFileSettingsDynamoDB(table, configuredObjectMapper);
    }


    private Map<String, FirmwareFile> itemToFirmwareFile(final Item item) {
        if (item == null) {
            return Maps.newHashMap();
        }

        if(!item.isNull(AttributeName.JSON_BLOB.value)) {
            final TypeFactory typeFactory = mapper.getTypeFactory();
            final MapType mapType = typeFactory.constructMapType(Map.class, String.class, FirmwareFile.class);


            try {
                return mapper.readValue(item.getString(AttributeName.JSON_BLOB.value), mapType);
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return Maps.newHashMap();
    }

    @Override
    public ImmutableMap<String, FirmwareFile> mappingForHardwareVersion(final HardwareVersion hardwareVersion) {
        final PrimaryKey key = new PrimaryKey(AttributeName.HW_VERSION.value, hardwareVersion.value);
        final Item item = table.getItem(key);
        return ImmutableMap.copyOf(itemToFirmwareFile(item));
    }

    @Override
    public boolean put(final HardwareVersion hardwareVersion, final Map<String, FirmwareFile> files) {
        try {
            final String jsonBlob = mapper.writeValueAsString(files);
            final String md5 = DigestUtils.md5Hex(jsonBlob);
            final String updated_at = DateTime.now(DateTimeZone.UTC).toString();
            final PrimaryKey primaryKey = new PrimaryKey(AttributeName.HW_VERSION.value, hardwareVersion.value);

            final Item item = new Item()
                    .withPrimaryKey(primaryKey)
                    .withString(AttributeName.JSON_BLOB.value, jsonBlob)
                    .withString(AttributeName.MD5_CHECKSUM.value, md5)
                    .withString(AttributeName.LAST_UPDATED_AT.value, updated_at);
            table.putItem(item);
            return true;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static TableDescription createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                        new KeySchemaElement().withAttributeName(AttributeName.HW_VERSION.value).withKeyType(KeyType.HASH)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(AttributeName.HW_VERSION.value).withAttributeType(ScalarAttributeType.N)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
        return table.getDescription();
    }
}
