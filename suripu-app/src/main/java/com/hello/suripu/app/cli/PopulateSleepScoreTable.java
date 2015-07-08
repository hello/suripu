package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.common.base.Optional;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.modules.RolloutAppModule;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.ModelPriorsDAO;
import com.hello.suripu.core.db.ModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();

        final DBI commonDB = new DBI(managedDataSourceFactory.build(configuration.getCommonDB()));
        final DBI sensorsDB = new DBI(managedDataSourceFactory.build(configuration.getSensorsDB()));

        commonDB.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new JodaArgumentFactory());

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        sensorsDB.registerContainerFactory(new ImmutableListContainerFactory());
        sensorsDB.registerContainerFactory(new ImmutableSetContainerFactory());

        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);

        final AmazonDynamoDB ringTimeDynamoDBClient = dynamoDBClientFactory.getForEndpoint(
                configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
                ringTimeDynamoDBClient, configuration.getRingTimeHistoryDBConfiguration().getTableName());

        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForEndpoint(
                configuration.getSleepStatsDynamoConfiguration().getEndpoint());
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(
                dynamoDBStatsClient,
                configuration.getSleepStatsDynamoConfiguration().getTableName(),
                configuration.getSleepStatsVersion()
        );

          /* Priors for bayesnet  */
        final String priorDbTableName = configuration.getHmmBayesnetPriorsConfiguration().getTableName();
        final AmazonDynamoDB priorsDb = dynamoDBClientFactory.getForEndpoint(priorDbTableName);
        final ModelPriorsDAO priorsDAO = new ModelPriorsDAODynamoDB(priorsDb,priorDbTableName);


        /* data for ye olde HMM */
        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
        final String sleepHmmTableName = configuration.getSleepHmmDBConfiguration().getTableName();
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);

        final String namespace1 = (configuration.getDebug()) ? "dev" : "prod";
        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, "features", namespace1);

        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final TimelineProcessor timelineProcessor = TimelineProcessor.createTimelineProcessor(
                trackerMotionDAO,
                deviceDAO, deviceDataDAO,
                ringTimeHistoryDAODynamoDB,
                feedbackDAO,
                sleepHmmDAODynamoDB,
                accountDAO,
                sleepStatsDAODynamoDB,
                senseColorDAO, priorsDAO,null);

        LOGGER.info("Getting all pills..");
        final List<DeviceAccountPair> activePills = deviceDAO.getAllPills(true);
        LOGGER.info("Found {} active pills", activePills.size());

        int numSaved = 0;
        for (final DeviceAccountPair pill : activePills) {
            final Long accountId = pill.accountId;

            final DateTime startTargetDate = pill.created.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
            final int numDays = DateTimeUtil.getDateDiffFromNowInDays(startTargetDate);

            LOGGER.info("-------- Processing account {}, pill created {}, no. of days {}, start date {}",
                    accountId, pill.created, numDays, startTargetDate);

            int hasScore = 0;
            for (int i = 0; i < numDays; i++) {
                final DateTime targetDate = startTargetDate.plusDays(i);
                final Optional<TimelineResult> result = timelineProcessor.retrieveTimelinesFast(accountId, targetDate);

                if (!result.isPresent()) {
                    continue;
                }

                final Timeline timeline = result.get().timelines.get(0);

                if (timeline.events.isEmpty()) {
                    LOGGER.info("Nothing for Date {}", targetDate);
                } else {
                    hasScore++;
                    LOGGER.info("Saved for Date {}, score {}, sleep-stats{}", targetDate, timeline.date, timeline.statistics);
                    numSaved++;
                }
                if (numSaved % 30 == 0) {
                    Thread.sleep(500);
                }
            }
            LOGGER.info("Account {} has scores for {} days", accountId, hasScore);
            LOGGER.info("Saved {} scores so far", numSaved);
        }

    }
}
