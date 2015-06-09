package com.hello.suripu.admin.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.models.Device;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class PopulateColors extends ConfiguredCommand<SuripuAdminConfiguration> {


    private final static Logger LOGGER = LoggerFactory.getLogger(PopulateColors.class);

    private static String SENSE_US_PREFIX_WHITE = "91000008W";
    private static String SENSE_US_PREFIX_BLACK = "91000008B";

    public PopulateColors() {
        super("colors", "Scan stuff");
    }

    @Override
    protected void run(Bootstrap<SuripuAdminConfiguration> bootstrap, Namespace namespace, SuripuAdminConfiguration configuration) throws Exception {

        final File file = new File("device_sn.txt");
        final String firstLine = Files.readFirstLine(file, Charsets.UTF_8);
        final ObjectMapper mapper = new ObjectMapper();

        if(firstLine.isEmpty()) {
            final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
            final AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
            amazonDynamoDBClient.setEndpoint(configuration.dynamoDBConfiguration().endpoints().get(DynamoDBTableName.SENSE_KEY_STORE));
            int emptyMetadata = 0;
            int total = 0;
            int incorrectMetadata = 0;
            final Map<String, String> store = Maps.newHashMap();


            Map<String, AttributeValue> lastKeyEvaluated = null;
            do {
                ScanRequest scanRequest = new ScanRequest()
                        .withTableName(configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SENSE_KEY_STORE))
                        .withExclusiveStartKey(lastKeyEvaluated)
                        .withLimit(5000);

                ScanResult result = amazonDynamoDBClient.scan(scanRequest);
                for (Map<String, AttributeValue> item : result.getItems()) {
                    if (item != null && item.containsKey("metadata")) {
                        store.put(item.get("device_id").getS(), item.get("metadata").getS().trim().toUpperCase());
                    } else {
                        LOGGER.error("No metadata for {}", item);
                    }
                }
                lastKeyEvaluated = result.getLastEvaluatedKey();
            } while (lastKeyEvaluated != null);

            System.out.println("Total = " + total);
            System.out.println("with correct metadata = " + store.size());
            System.out.println("with incorrect metadata = " + incorrectMetadata);
            System.out.println("Empty metadata = " + emptyMetadata);
            int notFound = 0;


            final String json = mapper.writeValueAsString(store);

            File wfile = new File("device_sn.txt");
            Files.write(json, wfile, Charsets.UTF_8);
        }

        final Map<String, String> store = mapper.readValue(firstLine, Map.class);

        for(final String deviceId : store.keySet()) {
            final String serialNumber = store.get(deviceId);
            Optional<Device.Color> colorOptional = toColor(serialNumber, deviceId);
            if(colorOptional.isPresent()) {
                System.out.println("OK " + deviceId + " " +  serialNumber + " " + colorOptional.get().name());
            } else {
                System.out.println("KO " + deviceId);
            }
        }
    }

    public static Optional<Device.Color> toColor(final String serialNumber, final String deviceId) {
        if(serialNumber.length() < SENSE_US_PREFIX_WHITE.length()) {
            LOGGER.error("SN {} is too short for device_id = {}", serialNumber, deviceId);
            return Optional.absent();
        }
        final String snPrefix = serialNumber.substring(0, SENSE_US_PREFIX_WHITE.length());
        if(snPrefix.toUpperCase().equals(SENSE_US_PREFIX_WHITE)) {
            return Optional.of(Device.Color.WHITE);
        } else if (snPrefix.toUpperCase().equals(SENSE_US_PREFIX_BLACK)) {
            return Optional.of(Device.Color.BLACK);
        }

        LOGGER.error("Can't get color for SN {}, {}", serialNumber, deviceId);
        return Optional.absent();
    }
}
