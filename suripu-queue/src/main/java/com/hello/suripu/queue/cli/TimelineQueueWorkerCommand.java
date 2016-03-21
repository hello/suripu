package com.hello.suripu.queue.cli;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleFromS3;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAODynamoDB;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAODynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAOImpl;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.coredw.configuration.S3BucketConfiguration;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
import com.hello.suripu.queue.configuration.SQSConfiguration;
import com.hello.suripu.queue.configuration.SuripuQueueConfiguration;
import com.hello.suripu.queue.modules.RolloutQueueModule;
import com.hello.suripu.queue.timeline.TimelineGenerator;
import com.hello.suripu.queue.timeline.TimelineQueueProcessor;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.cli.EnvironmentCommand;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.reporting.GraphiteReporter;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// docs: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/Welcome.html
public class TimelineQueueWorkerCommand extends EnvironmentCommand<SuripuQueueConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueWorkerCommand.class);
    private Boolean isRunning = false;
    private ExecutorService executor;

    public TimelineQueueWorkerCommand(Service<SuripuQueueConfiguration> service, String name, String description) {
        super(service, name, description);
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("--task")
                .nargs("?")
                .required(true)
                .help("task to perform, send or process queue messages");

        // for sending messages
        subparser.addArgument("--num_msg")
                .nargs("?")
                .required(false)
                .help("number of messages to send");

        subparser.addArgument("--account")
                .nargs("?")
                .required(false)
                .help("number of messages to send");

    }

    @Override
    protected void run(Environment environment, Namespace namespace, SuripuQueueConfiguration configuration) throws Exception {

        final String task = namespace.getString("task");

        // setup SQS connection
        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();

        final SQSConfiguration sqsConfiguration = configuration.getSqsConfiguration();
        final int maxConnections = sqsConfiguration.getSqsMaxConnections();
        final AmazonSQSAsync sqsAsync = new AmazonSQSAsyncClient(provider, new ClientConfiguration().withMaxConnections(maxConnections).withConnectionTimeout(500));
        final AmazonSQSAsync sqs = new AmazonSQSBufferedAsyncClient(sqsAsync);

        final Region region = Region.getRegion(Regions.US_EAST_1);
        sqs.setRegion(region);

        final Optional<String> optionalSqsQueueUrl = TimelineQueueProcessor.getSQSQueueURL(sqs, sqsConfiguration.getSqsQueueName());
        if (!optionalSqsQueueUrl.isPresent()) {
            LOGGER.error("error=no-sqs-found queue_name={}", sqsConfiguration.getSqsQueueName());
            throw new Exception("Invalid queue name");
        }

        final String sqsQueueUrl = optionalSqsQueueUrl.get();

        final TimelineQueueProcessor queueProcessor = new TimelineQueueProcessor(sqsQueueUrl, sqs, sqsConfiguration);

        if (task.equalsIgnoreCase("send")) {
            // producer -- debugging, create 10 messages for testing
            Integer numMessages = 30;
            Long accountId = 1310L;

            if (namespace.getString("num_msg") != null) {
                numMessages = Integer.valueOf(namespace.getString("num_msg"));
            }

            if (namespace.getString("account") != null) {
                accountId = Long.valueOf(namespace.getString("account"));
            }

            queueProcessor.sendMessages(accountId, numMessages);

        } else {
            // consumer
            final int numConsumerThreads = configuration.getNumConsumerThreads();
            executor = environment.managedExecutorService("timeline_queue", numConsumerThreads, numConsumerThreads, 2, TimeUnit.SECONDS);
            isRunning = true;
            processMessages(queueProcessor, provider, configuration);
        }
    }


    /**
     * Main queue worker function
     * @param queueProcessor - send, receive, encode and decode queue messages
     * @param provider - aws credentials
     * @param configuration - config object
     * @throws Exception
     */

    private void processMessages(final TimelineQueueProcessor queueProcessor,
                                 final AWSCredentialsProvider provider,
                                 final SuripuQueueConfiguration configuration) throws Exception {
        // setup rollout module
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(provider, configuration.dynamoDBConfiguration());
        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.FEATURES), "prod");

        final RolloutQueueModule module = new RolloutQueueModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        // set up metrics
        if (configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.suripu-queue", apiKey, env);

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final Meter messagesProcessed = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "processed", "messages-processed", TimeUnit.SECONDS);
        final Meter messagesReceived = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "received", "messages-received", TimeUnit.SECONDS);
        final Meter messagesDeleted = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "deleted", "messages-deleted", TimeUnit.SECONDS);
        final Meter validSleepScore = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "ok-sleep-score", "valid-score", TimeUnit.SECONDS);
        final Meter invalidSleepScore = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "invalid-sleep-score", "invalid-score", TimeUnit.SECONDS);
        final Meter noTimeline = Metrics.defaultRegistry().newMeter(TimelineQueueWorkerCommand.class, "timeline-fail", "fail-to-created", TimeUnit.SECONDS);

        final TimelineProcessor timelineProcessor = createTimelineProcessor(provider, configuration);
        int numEmptyQueueIterations = 0;

        do {
            final List<TimelineQueueProcessor.TimelineMessage> messages = queueProcessor.receiveMessages();

            messagesReceived.mark(messages.size());

            final List<Future<Optional<TimelineQueueProcessor.TimelineMessage>>> futures = Lists.newArrayListWithCapacity(messages.size());

            if (!messages.isEmpty()) {
                for (final TimelineQueueProcessor.TimelineMessage message : messages) {
                    final TimelineGenerator generator = new TimelineGenerator(timelineProcessor, message);
                    final Future<Optional<TimelineQueueProcessor.TimelineMessage>> future = executor.submit(generator);
                    futures.add(future);
                }

                final List<DeleteMessageBatchRequestEntry> processedHandlers = Lists.newArrayList();
                for (final Future<Optional<TimelineQueueProcessor.TimelineMessage>> future : futures) {
                    final Optional<TimelineQueueProcessor.TimelineMessage> processed = future.get();

                    if (!processed.isPresent()) {
                        noTimeline.mark();
                        continue;
                    }

                    processedHandlers.add(new DeleteMessageBatchRequestEntry(processed.get().messageId, processed.get().messageHandler));
                    if (processed.get().sleepScore > 0) {
                        validSleepScore.mark();
                    } else {
                        invalidSleepScore.mark();
                    }
                }

                if (!processedHandlers.isEmpty()) {
                    LOGGER.debug("action=delete-messages num={}", processedHandlers.size());
                    messagesProcessed.mark(processedHandlers.size());
                    final int deleted = queueProcessor.deleteMessages(processedHandlers);
                    messagesDeleted.mark(deleted);
                }

                numEmptyQueueIterations = 0;

            } else {
                numEmptyQueueIterations++;
                LOGGER.debug("action=empty-iteration value={}", numEmptyQueueIterations);
            }

        } while (isRunning);
    }

    private TimelineProcessor createTimelineProcessor(final AWSCredentialsProvider provider,
                                                      final SuripuQueueConfiguration config)throws Exception {

        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final DBI commonDB = new DBI(managedDataSourceFactory.build(config.getCommonDB()));

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        final DeviceReadDAO deviceDAO = commonDB.onDemand(DeviceReadDAO.class);
        final FeedbackReadDAO feedbackDAO = commonDB.onDemand(FeedbackReadDAO.class);
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final UserTimelineTestGroupDAO userTimelineTestGroupDAO = commonDB.onDemand(UserTimelineTestGroupDAOImpl.class);

        final ClientConfiguration clientConfig = new ClientConfiguration()
                .withConnectionTimeout(1000)
                .withMaxErrorRetry(5);

        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(provider,
                clientConfig, config.dynamoDBConfiguration());

        final ImmutableMap<DynamoDBTableName, String> tableNames = config.dynamoDBConfiguration().tables();

        final AmazonDynamoDB pillDataDAODynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_DATA);
        final PillDataDAODynamoDB pillDataDAODynamoDB = new PillDataDAODynamoDB(pillDataDAODynamoDBClient,
                tableNames.get(DynamoDBTableName.PILL_DATA));

        final AmazonDynamoDB deviceDataDAODynamoDBClient =  dynamoDBClientFactory.getForTable(DynamoDBTableName.DEVICE_DATA);
        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(deviceDataDAODynamoDBClient,
                tableNames.get(DynamoDBTableName.DEVICE_DATA));

        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.RING_TIME_HISTORY);
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.RING_TIME_HISTORY));

        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_HMM);
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,
                tableNames.get(DynamoDBTableName.SLEEP_HMM));

        // use SQS version for testing
        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_STATS);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                tableNames.get(DynamoDBTableName.SLEEP_STATS),
                config.getSleepStatsVersion());

        final AmazonDynamoDB onlineHmmModelsDb = dynamoDBClientFactory.getForTable(DynamoDBTableName.ONLINE_HMM_MODELS);
        final OnlineHmmModelsDAO onlineHmmModelsDAO = OnlineHmmModelsDAODynamoDB.create(onlineHmmModelsDb,
                tableNames.get(DynamoDBTableName.ONLINE_HMM_MODELS));

        final AmazonDynamoDB featureExtractionModelsDb = dynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURE_EXTRACTION_MODELS);
        final FeatureExtractionModelsDAO featureExtractionDAO = new FeatureExtractionModelsDAODynamoDB(featureExtractionModelsDb,
                tableNames.get(DynamoDBTableName.FEATURE_EXTRACTION_MODELS));

        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.CALIBRATION);
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.create(calibrationDynamoDBClient,
                tableNames.get(DynamoDBTableName.CALIBRATION));

        /* Default model ensemble for all users  */
        final S3BucketConfiguration timelineModelEnsemblesConfig = config.getTimelineModelEnsemblesConfiguration();
        final S3BucketConfiguration seedModelConfig = config.getTimelineSeedModelConfiguration();

        final AmazonS3 amazonS3 = new AmazonS3Client(provider, clientConfig);
        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = DefaultModelEnsembleFromS3.create(amazonS3,
                timelineModelEnsemblesConfig.getBucket(),
                timelineModelEnsemblesConfig.getKey(),
                seedModelConfig.getBucket(),
                seedModelConfig.getKey());

        return TimelineProcessor.createTimelineProcessor(
                pillDataDAODynamoDB,
                deviceDAO,
                deviceDataDAODynamoDB,
                ringTimeHistoryDAODynamoDB,
                feedbackDAO,
                sleepHmmDAODynamoDB,
                accountDAO,
                sleepStatsDAODynamoDB,
                senseColorDAO,
                onlineHmmModelsDAO,
                featureExtractionDAO,
                calibrationDAO,
                defaultModelEnsembleDAO,
                userTimelineTestGroupDAO);

    }

}

