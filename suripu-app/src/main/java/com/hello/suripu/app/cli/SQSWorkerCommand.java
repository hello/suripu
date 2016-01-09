package com.hello.suripu.app.cli;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.sqs.TimelineQueue;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.modules.RolloutAppModule;
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
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.coredw.configuration.S3BucketConfiguration;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.Set;

// docs: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/Welcome.html
public class SQSWorkerCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQSWorkerCommand.class);

    public SQSWorkerCommand() {
        super("sqs_worker", "test sqs worker");
    }


    @Override
    protected void run(final Bootstrap<SuripuAppConfiguration> bootstrap,
                       final Namespace namespace,
                       final SuripuAppConfiguration config) throws Exception {

        // setup SQS connection
        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();

        final int maxConnections = config.getSqsMaxConnections();
        final AmazonSQSAsync sqsAsync = new AmazonSQSAsyncClient(provider, new ClientConfiguration().withMaxConnections(maxConnections));
        final AmazonSQSAsync sqs = new AmazonSQSBufferedAsyncClient(sqsAsync);

        final Region region = Region.getRegion(Regions.US_EAST_1);
        sqs.setRegion(region);

        final String sqsQueueUrl = getSQSQueueURL(sqs, config.getSqsQueueName());
        if (sqsQueueUrl.isEmpty()) {
            LOGGER.error("error=no-sqs-found queue_name={}", config.getSqsQueueName());
            return;
        }

        // create timeline processor
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(provider, config.dynamoDBConfiguration());

        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient,
                config.dynamoDBConfiguration().tables().get(DynamoDBTableName.FEATURES), "prod");

        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);


        final TimelineProcessor timelineProcessor = createTimelineProcessor(provider, config);

        // producer -- debugging, create 10 messages for testing
        sendMessages(sqs, sqsQueueUrl, 1310L, 10);


        // consumer
        processMessages(sqs,
                sqsQueueUrl,
                config.getSqsMaxMessage(),
                config.getSqsVisibilityTimeoutSeconds(),
                config.getSqsWaitTimeSeconds(),
                timelineProcessor);

    }

    // see https://github.com/aws/aws-sdk-java/tree/master/src/samples/AmazonSimpleQueueService
    // http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/throughput.html
    private void processMessages(final AmazonSQSAsync sqs,
                                final String sqsQueueUrl,
                                final int maxMessages,
                                final int visibilityTimeoutSeconds,
                                final int waitTimeSeconds,
                                final TimelineProcessor timelineProcessor) throws InterruptedException {

        final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(sqsQueueUrl)
                .withMaxNumberOfMessages(maxMessages)
                .withVisibilityTimeout(visibilityTimeoutSeconds)
                .withWaitTimeSeconds(waitTimeSeconds);

        // for debugging
        final Set<String> processedIdsSet = Sets.newHashSet();

        int emptyRuns = 0;
        int totalProcessed = 0;
        do {
            try {

                final ReceiveMessageResult rx = sqs.receiveMessage(receiveMessageRequest);
                final List<Message> messages = rx.getMessages();

                if (messages.isEmpty()) {
                    emptyRuns++;
                    LOGGER.debug("action=wait-on-no-data iterations={}", emptyRuns);
                    Thread.sleep(10L);
                    continue;
                }

                LOGGER.debug("action=consume-messages num_messages={}", messages.size());

                final List<DeleteMessageBatchRequestEntry> processedHandles = Lists.newArrayListWithExpectedSize(messages.size());

                for (final Message msg : messages) {
                    final String messageId = msg.getMessageId();
                    final String messageReceiptHandle = msg.getReceiptHandle();

                    // debugging
                    LOGGER.debug("action=get-sqs-message-info id={}", messageId);
                    processedIdsSet.add(messageId);

                    // decode message
                    final Optional<TimelineQueue.SQSMessage> optionalMsg = decodeMessage(msg.getBody());


                    if (optionalMsg.isPresent()) {
                        final TimelineQueue.SQSMessage sqsMsg = optionalMsg.get();
                        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(sqsMsg.getTargetDate());
                        final Long accountId = sqsMsg.getAccountId();
                        LOGGER.debug("action=decode-protobuf-message account_id={}, date={}", accountId, targetDate);

                        final TimelineResult result = timelineProcessor.retrieveTimelinesFast(accountId,
                                targetDate,
                                Optional.<TimelineFeedback>absent());

                        if (!result.getTimelineLogV2().isEmpty()) {
                            LOGGER.debug("action=compute-timeline num={} score={}",
                                    result.timelines.size(), result.timelines.get(0).score);
                        } else {
                            LOGGER.debug("action=no-timeline");
                        }
                        processedHandles.add(new DeleteMessageBatchRequestEntry(messageId, messageReceiptHandle));
                    }
                }

                LOGGER.debug("action=delete-messages num={}", processedHandles.size());
                totalProcessed += processedHandles.size();
                sqs.deleteMessageBatch(new DeleteMessageBatchRequest(sqsQueueUrl, processedHandles));

            } catch (AmazonServiceException ase) {
                LOGGER.error("error=sqs-request-rejected reason={}", ase.getMessage()) ;
            } catch (AmazonClientException ace) {
                LOGGER.error("error=amazon-client-exception reason={}", ace.getMessage());
            }

            emptyRuns++;
        } while (emptyRuns < 10);

        LOGGER.debug("Number of UNIQUE ids processed: {}", processedIdsSet.size());
        LOGGER.info("action=total-processed value={}", totalProcessed);
    }

    private void sendMessages(final AmazonSQSAsync sqs,
                              final String sqsQueueUrl,
                              final long accountId, final int numMessage) {

        final TimelineQueue.SQSMessage.Builder messageBuilder = TimelineQueue.SQSMessage.newBuilder();

        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final Random rnd = new Random();

        for (int i = 0; i < numMessage; i++) {
            final int randomInt = rnd.nextInt(60); // last 60 days
            final DateTime targetDate = now.minusDays(randomInt);
            final String date = DateTimeUtil.dateToYmdString(targetDate);
            LOGGER.debug("action=add-message num={} target_date={}", i, date);

            final TimelineQueue.SQSMessage SQSMessage = messageBuilder
                    .setAccountId(accountId)
                    .setTargetDate(date)
                    .setTimestamp(DateTime.now().getMillis()).build();

            final String message = encodeMessage(SQSMessage);
            sqs.sendMessage(new SendMessageRequest(sqsQueueUrl, message));
        }
    }

    private String getSQSQueueURL(final AmazonSQS sqs, final String queueName) {
        try {
            final List<String> queueUrls = sqs.listQueues().getQueueUrls();

            for (final String url : queueUrls) {
                if (url.contains(queueName)) {
                    LOGGER.debug("action=found-sqs-url value={}", url);
                    return url;
                }
            }
        }  catch (AmazonServiceException ase) {
            LOGGER.error("error=sqs-request-rejected reason={}", ase.getMessage()) ;
        } catch (AmazonClientException ace) {
            LOGGER.error("error=amazon-client-exception-internal-problem reason={}", ace.getMessage());
        }
        return "";
    }

    private static String encodeMessage(final TimelineQueue.SQSMessage message) {
        final byte [] bytes = message.toByteArray();
        return Base64.encodeBase64URLSafeString(bytes);
    }

    private static Optional<TimelineQueue.SQSMessage> decodeMessage(final String message) {
        final TimelineQueue.SQSMessage sqsMessage;
        try {
            sqsMessage = TimelineQueue.SQSMessage.parseFrom(Base64.decodeBase64(message));
            // LOGGER.debug("action=print-decoded-message value={}", sqsMessage.toString());
            return Optional.of(sqsMessage);
        } catch (InvalidProtocolBufferException pbe) {
            LOGGER.error(pbe.getMessage());
        }
        return Optional.absent();
    }

    private TimelineProcessor createTimelineProcessor(final AWSCredentialsProvider provider,
                                                      final SuripuAppConfiguration config)throws Exception {

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

        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(provider, config.dynamoDBConfiguration());
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
        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SQS_SLEEP_STATS);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                tableNames.get(DynamoDBTableName.SQS_SLEEP_STATS),
                config.getSqsSleepStatsVersion());

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

        final ClientConfiguration clientConfig = new ClientConfiguration()
                .withConnectionTimeout(200)
                .withMaxErrorRetry(1);

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
                defaultModelEnsembleDAO);

    }
}

