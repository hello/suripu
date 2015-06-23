package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.models.RingTime;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 4/28/15.
 */
public class ScanNonSmartAlarmCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ScanNonSmartAlarmCommand.class);

    public ScanNonSmartAlarmCommand(){
        super("non_smart_log", "Get all smart alarm within 5 minutes of expected ring time.");
    }

    @Override
    protected void run(final Bootstrap<SuripuAppConfiguration> bootstrap, final Namespace namespace, final SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);

        final AmazonDynamoDB smartAlarmLogDynamoDBClient = dynamoDBClientFactory.getForEndpoint(
                configuration.getSmartAlarmLogDBConfiguration().getEndpoint());
        final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB = new SmartAlarmLoggerDynamoDB(
                smartAlarmLogDynamoDBClient, configuration.getSmartAlarmLogDBConfiguration().getTableName());
        final List<Map.Entry<Long, RingTime>> nonStartAlarms = smartAlarmLoggerDynamoDB.scanSmartRingTimesTooCloseToExpected();

        LOGGER.info("Scan completed.");
    }
}
