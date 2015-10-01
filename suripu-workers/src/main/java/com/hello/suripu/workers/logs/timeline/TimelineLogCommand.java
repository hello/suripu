package com.hello.suripu.workers.logs.timeline;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.workers.framework.WorkerEnvironmentCommand;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class TimelineLogCommand extends WorkerEnvironmentCommand<TimelineLogConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineLogCommand.class);

    public TimelineLogCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, TimelineLogConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.LOGS);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);


        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.maxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.LATEST);

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);



        final DBIFactory dbiFactory = new DBIFactory();
        final DBI commonDB = dbiFactory.build(environment, configuration.getCommonDB(), "postgresql");
        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final TimelineAnalytics timelineAnalytics = commonDB.onDemand(TimelineAnalytics.class);

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final IRecordProcessorFactory factory = new TimelineLogProcessorFactory(timelineAnalytics);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}
