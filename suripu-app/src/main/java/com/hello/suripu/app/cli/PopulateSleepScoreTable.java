package com.hello.suripu.app.cli;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kingshy on 3/19/15.
 */
public class PopulateSleepScoreTable extends ConfiguredCommand<SuripuAppConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PopulateSleepScoreTable.class);

    public PopulateSleepScoreTable() {
        super("populate_sleep_score", "backfill sleep score in dynamo");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
//        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
//
//        final DBI commonDB = new DBI(managedDataSourceFactory.build(configuration.getCommonDB()));
//        final DBI sensorsDB = new DBI(managedDataSourceFactory.build(configuration.getSensorsDB()));
//
//        commonDB.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
//        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
//        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());
//        commonDB.registerContainerFactory(new OptionalContainerFactory());
//        commonDB.registerArgumentFactory(new JodaArgumentFactory());
//
//        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
//        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
//        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);
//
//        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
//        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
//        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
//        sensorsDB.registerContainerFactory(new ImmutableListContainerFactory());
//        sensorsDB.registerContainerFactory(new ImmutableSetContainerFactory());
//
//        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
//        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
//
//        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
//        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
//
//
//        final AmazonDynamoDB ringTimeDynamoDBClient = dynamoDBClientFactory.getForEndpoint(
//                configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
//        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
//                ringTimeDynamoDBClient, configuration.getRingTimeHistoryDBConfiguration().getTableName());
//
//        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForEndpoint(
//                configuration.getSleepStatsDynamoConfiguration().getEndpoint());
//        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(
//                dynamoDBStatsClient,
//                configuration.getSleepStatsDynamoConfiguration().getTableName(),
//                configuration.getSleepStatsVersion()
//        );
//
//       /* Priors for bayesnet  */
//        final String priorDbTableName = configuration.getHmmBayesnetPriorsConfiguration().getTableName();
//        final AmazonDynamoDB priorsDb = dynamoDBClientFactory.getForEndpoint(configuration.getHmmBayesnetPriorsConfiguration().getEndpoint());
//        final BayesNetHmmModelPriorsDAO priorsDAO = new BayesNetHmmModelPriorsDAODynamoDB(priorsDb,priorDbTableName);
//
//        /* Models for bayesnet */
//        final String modelDbTableName = configuration.getHmmBayesnetModelsConfiguration().getTableName();
//        final AmazonDynamoDB modelsDb = dynamoDBClientFactory.getForEndpoint(configuration.getHmmBayesnetModelsConfiguration().getEndpoint());
//        final BayesNetModelDAO modelDAO = new BayesNetHmmModelDAODynamoDB(modelsDb,modelDbTableName);
//
//
//        /* data for ye olde HMM */
//        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
//        final String sleepHmmTableName = configuration.getSleepHmmDBConfiguration().getTableName();
//        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);
//
//        final String namespace1 = (configuration.getDebug()) ? "dev" : "prod";
//        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
//        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, "features", namespace1);
//
//        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
//        ObjectGraphRoot.getInstance().init(module);
//
//        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
//
//
//        final AmazonDynamoDB calibrationDynamoDB = dynamoDBClientFactory.getForEndpoint(configuration.getCalibrationConfiguration().getEndpoint());
//        final CalibrationDAO calibrationDAO = new CalibrationDynamoDB(calibrationDynamoDB, configuration.getCalibrationConfiguration().getTableName());
//
//        final TimelineProcessor timelineProcessor = TimelineProcessor.createTimelineProcessor(
//                trackerMotionDAO,
//                deviceDAO, deviceDataDAO,
//                ringTimeHistoryDAODynamoDB,
//                feedbackDAO,
//                sleepHmmDAODynamoDB,
//                accountDAO,
//                sleepStatsDAODynamoDB,
//                senseColorDAO, priorsDAO,modelDAO,
//                calibrationDAO);
//
//        LOGGER.info("Getting all pills..");
//        final List<DeviceAccountPair> activePills = deviceDAO.getAllPills(true);
//        LOGGER.info("Found {} active pills", activePills.size());
//
//        int numSaved = 0;
//        for (final DeviceAccountPair pill : activePills) {
//            final Long accountId = pill.accountId;
//
//            final DateTime startTargetDate = pill.created.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
//            final int numDays = DateTimeUtil.getDateDiffFromNowInDays(startTargetDate);
//
//            LOGGER.info("-------- Processing account {}, pill created {}, no. of days {}, start date {}",
//                    accountId, pill.created, numDays, startTargetDate);
//
//            int hasScore = 0;
//            for (int i = 0; i < numDays; i++) {
//                final DateTime targetDate = startTargetDate.plusDays(i);
//                final Optional<TimelineResult> result = timelineProcessor.retrieveTimelinesFast(accountId, targetDate);
//
//                if (!result.isPresent()) {
//                    continue;
//                }
//
//                final Timeline timeline = result.get().timelines.get(0);
//
//                if (timeline.events.isEmpty()) {
//                    LOGGER.info("Nothing for Date {}", targetDate);
//                } else {
//                    hasScore++;
//                    LOGGER.info("Saved for Date {}, score {}, sleep-stats{}", targetDate, timeline.date, timeline.statistics);
//                    numSaved++;
//                }
//                if (numSaved % 30 == 0) {
//                    Thread.sleep(500);
//                }
//            }
//            LOGGER.info("Account {} has scores for {} days", accountId, hasScore);
//            LOGGER.info("Saved {} scores so far", numSaved);
//        }

    }
}
