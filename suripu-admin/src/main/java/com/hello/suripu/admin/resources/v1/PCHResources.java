package com.hello.suripu.admin.resources.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
    private final SenseColorDAO senseColorDAO;
    private final String pillKeyStoreTableName;
    private final static Integer SCAN_QUERY_LIMIT = 5000;
    private final static String WHITE_SENSE_PREFIX = "91000008W";
    private final static String BLACK_SENSE_PREFIX = "91000008B";

    public PCHResources(final AmazonDynamoDB amazonDynamoDB, final String senseKeyStoreTableName, final String pillKeyStoreTableName, final SenseColorDAO senseColorDAO) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.senseKeyStoreTableName = senseKeyStoreTableName;
        this.pillKeyStoreTableName = pillKeyStoreTableName;
        this.senseColorDAO = senseColorDAO;
    }

    @POST
    @Path("/check/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> checkIfSerialNumbersExists(@Scope(OAuthScope.PCH_READ) final AccessToken accessToken, @Valid @NotNull Set<String> snToChecks) {

        LOGGER.warn("Checking {} Sense SNs", snToChecks.size());

        Map<String, AttributeValue> lastKeyEvaluated = null;
        final Set<String> store = Sets.newHashSet();
        int i = 0;
        do {
            LOGGER.info("Scanning table: {}. Iteration # {}. Store size: {}", senseKeyStoreTableName, i, store.size());
            final ScanRequest scanRequest = new ScanRequest()
                    .withTableName(senseKeyStoreTableName)
                    .withAttributesToGet("metadata")
                    .withExclusiveStartKey(lastKeyEvaluated)
                    .withLimit(SCAN_QUERY_LIMIT);

            final ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            LOGGER.info("Last key: {}", lastKeyEvaluated);
            for (final Map<String, AttributeValue> map : scanResult.getItems()) {
                final String metadata = (map.containsKey("metadata")) ? map.get("metadata").getS() : "";
                if(metadata.startsWith("9")) {
                    store.add(metadata.toUpperCase());
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                // Fail silently if exception on Thread.Sleep
            }
            i++;
        } while(lastKeyEvaluated != null);


        final Set<String> missingSN = Sets.difference(snToChecks, store);

        LOGGER.warn("{} SNs were not found", missingSN.size());
        LOGGER.warn("{}", missingSN);
        return missingSN;
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
                    .withAttributesToGet("metadata")
                    .withExclusiveStartKey(lastKeyEvaluated)
                    .withLimit(SCAN_QUERY_LIMIT);

            final ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            LOGGER.info("Last key: {}", lastKeyEvaluated);
            for (final Map<String, AttributeValue> map : scanResult.getItems()) {
                if(map.containsKey("metadata")) {
                    store.add(map.get("metadata").getS().trim());
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


    @GET
    @Path("/colors")
    public String updateSenseColors(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken) {

        Map<String, AttributeValue> lastKeyEvaluated = null;
        int count = 0;
        int i = 0;
        do {
            LOGGER.info("Scanning table: {}. Iteration # {}. Store size: {}", senseKeyStoreTableName, i, count);

            final ScanRequest scanRequest = new ScanRequest()
                    .withTableName(senseKeyStoreTableName)
                    .withAttributesToGet("device_id", "metadata")
                    .withExclusiveStartKey(lastKeyEvaluated)
                    .withLimit(SCAN_QUERY_LIMIT);

            final ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            lastKeyEvaluated = scanResult.getLastEvaluatedKey();
            LOGGER.info("Last key: {}", lastKeyEvaluated);
            for (final Map<String, AttributeValue> map : scanResult.getItems()) {
                if(map.containsKey("metadata") && map.containsKey("device_id")) {
                    // This a potential race condition but since colors are immutable that's not really an issue
                    final Optional<Device.Color> colorOptional = senseColorDAO.getColorForSense(map.get("device_id").getS());
                    if(!colorOptional.isPresent()) {
                        final String sn = map.get("metadata").getS();
                        senseColorDAO.saveColorForSense(map.get("device_id").getS(), fromSN(map.get("device_id").getS(), sn).name());
                    }
                }
                count ++;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOGGER.error("{}", e.getMessage());
                return "FAILED";
            }
            i++;
        } while(lastKeyEvaluated != null);


        return "OK";
    }


    public static Device.Color fromSN(final String senseId, final String serialNumber) {

        if(serialNumber.length() < BLACK_SENSE_PREFIX.length()) {
            return Device.Color.BLACK;
        }

        final String prefix = serialNumber.toUpperCase().substring(0, BLACK_SENSE_PREFIX.length());
        if(WHITE_SENSE_PREFIX.equals(prefix)) {
            return Device.Color.WHITE;
        } else if(BLACK_SENSE_PREFIX.equals(prefix)) {
            return Device.Color.BLACK;
        }

        LOGGER.warn("Unknown color for sense {} with SN: {}", senseId, serialNumber);
        return Device.Color.BLACK;
    }


}
