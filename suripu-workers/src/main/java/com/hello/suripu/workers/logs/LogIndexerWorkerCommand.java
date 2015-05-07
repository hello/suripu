package com.hello.suripu.workers.logs;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LogIndexerWorkerCommand extends ConfiguredCommand<LogIndexerWorkerConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerWorkerCommand.class);

    public LogIndexerWorkerCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(final Bootstrap<LogIndexerWorkerConfiguration> bootstrap, final Namespace namespace, final LogIndexerWorkerConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        init(configuration, awsCredentialsProvider);


        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.suripu-workers", apiKey, env);

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

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

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final DBI commonDB = new DBI(managedDataSourceFactory.build(configuration.getCommonDBConfiguration()));

        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());
        commonDB.registerArgumentFactory(new JodaArgumentFactory());


        final OnBoardingLogDAO onBoardingLogDAO = commonDB.onDemand(OnBoardingLogDAO.class);



        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final IRecordProcessorFactory factory = new LogIndexerProcessorFactory(configuration,
                amazonDynamoDBClientFactory,
                onBoardingLogDAO);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }


    protected void init(final LogIndexerWorkerConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(awsCredentialsProvider);
        amazonDynamoDB.setEndpoint(configuration.getSenseEventsDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getSenseEventsDynamoDBConfiguration().getTableName();
        try {
            amazonDynamoDB.describeTable(tableName);
            LOGGER.info("{} already exists.", tableName);
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SenseEventsDAO.createTable(tableName, amazonDynamoDB);
            final TableDescription description = result.getTableDescription();
            LOGGER.info("{}: {}", tableName, description.getTableStatus());
        }
    }
}