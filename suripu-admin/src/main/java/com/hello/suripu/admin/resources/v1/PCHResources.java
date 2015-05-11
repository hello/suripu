package com.hello.suripu.admin.resources.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

@Path("/v1/pch")
public class PCHResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(PCHResources.class);

    private final AmazonDynamoDB amazonDynamoDB;
    private final String senseKeyStoreTableName;
    private final String pillKeyStoreTableName;

    public PCHResources(final AmazonDynamoDB amazonDynamoDB, final String senseKeyStoreTableName, final String pillKeyStoreTableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.senseKeyStoreTableName = senseKeyStoreTableName;
        this.pillKeyStoreTableName = pillKeyStoreTableName;
    }

    @POST
    @Path("/check/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> checkIfSerialNumbersExists(@Scope(OAuthScope.PCH_READ) final AccessToken accessToken, @Valid @NotNull Set<String> snToChecks) {

        LOGGER.warn("Checking {} Sense SNs", snToChecks.size());

        final Map<String, String> store = Maps.newHashMap();
        Map<String, AttributeValue> lastKeyEvaluated = null;
        int i = 0;
        do {
            LOGGER.info("Scanning table: {}. Iteration # {}. Store size: {}", senseKeyStoreTableName, i, store.size());
            final ScanRequest scanRequest = new ScanRequest()
                    .withTableName(senseKeyStoreTableName)
                    .withAttributesToGet("device_id", "aes_key", "metadata")
                    .withExclusiveStartKey(lastKeyEvaluated);

            final ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            LOGGER.info("Last key: {}", lastKeyEvaluated);
            for (final Map<String, AttributeValue> map : scanResult.getItems()) {
                String deviceID = map.get("device_id").getS().trim();
                String metadata = (map.containsKey("metadata")) ? map.get("metadata").getS() : "";
                if(metadata.startsWith("9")) {
                    store.put(metadata.toUpperCase(), deviceID);
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // Fail silently if exception on Thread.Sleep
            }

        } while(lastKeyEvaluated != null);

        final Set<String> missingSerialNumbers = snToChecks;
        for(final String sn: snToChecks) {
            if(!store.containsKey(sn.trim())) {
                missingSerialNumbers.add(sn);
            }
        }

        LOGGER.warn("{} SNs were not found", missingSerialNumbers.size());
        LOGGER.warn("{}", missingSerialNumbers);
        return missingSerialNumbers;
    }



    @POST
    @Path("/check/pill")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String checkIfPillDeviceIdExists(@Scope(OAuthScope.PCH_READ) final AccessToken accessToken, final String body) {

        final String[] pillDeviceIds = body.split("\n");
        final Set<String> pills = Sets.newHashSet();
        for(final String pill : pillDeviceIds) {
            pills.add(pill.trim());
        }

        final Set<String> store = Sets.newHashSet();
        Map<String, AttributeValue> lastKeyEvaluated = null;
        int i = 0;
        do {
            LOGGER.info("Scanning table: {}. Iteration # {}. Store size: {}", pillKeyStoreTableName, i, store.size());

            final ScanRequest scanRequest = new ScanRequest()
                    .withTableName(pillKeyStoreTableName)
                    .withAttributesToGet("device_id", "aes_key")
                    .withExclusiveStartKey(lastKeyEvaluated);

            final ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            LOGGER.info("Last key: {}", lastKeyEvaluated);
            for (final Map<String, AttributeValue> map : scanResult.getItems()) {
                final String deviceID = map.get("device_id").getS().trim();
                if(map.containsKey("aes_key")) {
                    store.add(deviceID);
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOGGER.error("{}", e.getMessage());
                return body;
            }
            i++;
        } while(lastKeyEvaluated != null);

        LOGGER.info("Store size: {}", store.size());
        final Set<String> missingPills = Sets.difference(pills, store);
        final StringBuilder sb = new StringBuilder();
        for(final String missingPill : missingPills) {
            sb.append(missingPill + "\n");
        }

        return sb.toString();
    }
}
