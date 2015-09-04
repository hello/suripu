package com.hello.suripu.core.db;


import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WifiInfoDAODynamoDBTest {
    @Test
    public void testCreateWifiInfofromDynamoDBItem() {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        final String senseId = "t-sense";
        final String ssid = "t-ssid";
        final Integer rssi = -84;
        final DateTime dt = new DateTime(12345679000L);

        item.put(WifiInfoDynamoDB.SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        item.put(WifiInfoDynamoDB.SSID_ATTRIBUTE_NAME, new AttributeValue().withS(ssid));
        item.put(WifiInfoDynamoDB.RSSI_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(rssi)));
        item.put(WifiInfoDynamoDB.LAST_UPDATED_ATTRIBUTE_NAME, new AttributeValue().withS(dt.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT))));

        final WifiInfo wifiInfo = WifiInfoDynamoDB.createWifiInfofromDynamoDBItem(item, senseId).get();

        assertThat(wifiInfo.senseId, is(senseId));
        assertThat(wifiInfo.ssid, is(ssid));
        assertThat(wifiInfo.rssi, is(rssi));
        assertThat(wifiInfo.lastUpdated, is(dt));
    }

    @Test
    public void testCreateDynamoDBItemFromWifiInfoWith() {
        final WifiInfo wifiInfo = WifiInfo.create("t-sense", "t-ssid", -90, new DateTime(12345679000L));
        final Map<String, AttributeValue> item = WifiInfoDynamoDB.createDynamoDBItemFromWifiInfo(wifiInfo);
        
        assertThat(wifiInfo.senseId, is(item.get(WifiInfoDynamoDB.SENSE_ATTRIBUTE_NAME).getS()));
        assertThat(wifiInfo.ssid, is(item.get(WifiInfoDynamoDB.SSID_ATTRIBUTE_NAME).getS()));
        assertThat(wifiInfo.rssi, is(Integer.valueOf(item.get(WifiInfoDynamoDB.RSSI_ATTRIBUTE_NAME).getN())));
        assertThat(wifiInfo.lastUpdated, is(DateTime.parse(item.get(WifiInfoDynamoDB.LAST_UPDATED_ATTRIBUTE_NAME).getS(), DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT))));
    }

    @Test
    public void testCreateWifiInfofromNull() {
        final String senseId = "t-sense";
        final Optional<WifiInfo> wifiInfoOptional = WifiInfoDynamoDB.createWifiInfofromDynamoDBItem(null, senseId);
        assertThat(wifiInfoOptional.isPresent(), is(false));
    }

    @Test
    public void testCreateWifiInfofromDynamoDBItemWithInsufficientAttr() {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        final String senseId = "t-sense";

        item.put(WifiInfoDynamoDB.SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final Optional<WifiInfo> wifiInfoOptional = WifiInfoDynamoDB.createWifiInfofromDynamoDBItem(item, senseId);

        assertThat(wifiInfoOptional.isPresent(), is(false));
    }

    @Test
    public void testCreateWifiInfofromDynamoDBItemWithMissingLastUpdated() {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        final String senseId = "t-sense";
        final String ssid = "t-ssid";
        final Integer rssi = -87;

        item.put(WifiInfoDynamoDB.SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        item.put(WifiInfoDynamoDB.SSID_ATTRIBUTE_NAME, new AttributeValue().withS(ssid));
        item.put(WifiInfoDynamoDB.RSSI_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(rssi)));

        final WifiInfo wifiInfo = WifiInfoDynamoDB.createWifiInfofromDynamoDBItem(item, senseId).get();

        assertThat(wifiInfo.senseId, is(senseId));
        assertThat(wifiInfo.ssid, is(ssid));
        assertThat(wifiInfo.rssi, is(rssi));
        assertThat(wifiInfo.lastUpdated, is(DateTime.now(DateTimeZone.UTC)));
    }
}
