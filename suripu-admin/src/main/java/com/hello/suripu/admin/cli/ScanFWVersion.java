package com.hello.suripu.admin.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScanFWVersion extends ConfiguredCommand<SuripuAdminConfiguration> {

    public ScanFWVersion() {
        super("fw_check", "Scan stuff");
    }

    @Override
    protected void run(Bootstrap<SuripuAdminConfiguration> bootstrap, Namespace namespace, SuripuAdminConfiguration configuration) throws Exception {


        Logger LOGGER = LoggerFactory.getLogger(ScanFWVersion.class);

        final List<String> lines = Files.readLines(new File("sn_to_search.txt"), Charsets.UTF_8);
        final Set<String> snToCheck = Sets.newHashSet();
        for(String line: lines) {
            snToCheck.add(line.trim().toUpperCase());
        }

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        amazonDynamoDBClient.setEndpoint(configuration.dynamoDBConfiguration().endpoints().get(DynamoDBTableName.SENSE_KEY_STORE));
        int emptyMetadata = 0;
        int total = 0;
        int incorrectMetadata = 0;
        final Map<String, Integer> store = Maps.newHashMap();


        Map<String, AttributeValue> lastKeyEvaluated = null;
        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SENSE_LAST_SEEN))
                    .withExclusiveStartKey(lastKeyEvaluated)
                    .withAttributesToGet("sense_id", "fw_version")
                    .withLimit(5000);

            ScanResult result = amazonDynamoDBClient.scan(scanRequest);
            for (Map<String, AttributeValue> item : result.getItems()){
                if(item != null && item.containsKey("fw_version") && item.containsKey("sense_id")) {
                    if(Integer.parseInt(item.get("fw_version").getN()) != 111277477) {
                        store.put(item.get("sense_id").getS(), Integer.valueOf(item.get("fw_version").getN()));
                    }
                } else {
                    LOGGER.error("No fw_version and  for {}", item);
                }
            }
            lastKeyEvaluated = result.getLastEvaluatedKey();
        } while (lastKeyEvaluated != null);



        System.out.println("Total = " + total);
        System.out.println("with correct metadata = " + store.size());
        System.out.println("with incorrect metadata = " + incorrectMetadata);
        System.out.println("Empty metadata = " + emptyMetadata);
        int notFound = 0;
        for(String senseId: store.keySet()) {
            System.out.println("Sense " + senseId+ " -> FW version " + store.get(senseId));

        }
        System.out.println("Not found = " + notFound);

    }
}
