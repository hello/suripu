package com.hello.suripu.research;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAODynamoDB;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAODynamoDB;
import com.hello.suripu.coredw.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.coredw.db.TimelineLogDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.filters.CacheFilterFactory;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.coredw.oauth.OAuthAuthenticator;
import com.hello.suripu.coredw.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.hello.suripu.research.configuration.SuripuResearchConfiguration;
import com.hello.suripu.research.db.LabelDAO;
import com.hello.suripu.research.db.LabelDAOImpl;
import com.hello.suripu.research.modules.RolloutResearchModule;
import com.hello.suripu.research.resources.v1.AccountInfoResource;
import com.hello.suripu.research.resources.v1.DataScienceResource;
import com.hello.suripu.research.resources.v1.PredictionResource;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;

/**
 * Created by pangwu on 3/2/15.
 */
public class SuripuResearch extends Service<SuripuResearchConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuResearch.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuResearch().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuResearchConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addBundle(new KinesisLoggerBundle<SuripuResearchConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(final SuripuResearchConfiguration configuration) {
                return configuration.getKinesisLoggerConfiguration();
            }
        });
    }

    @Override
    public void run(final SuripuResearchConfiguration configuration, final Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI sensorsDB = factory.build(environment, configuration.getSensorsDB(), "postgresql");
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI researchDB = factory.build(environment, configuration.getResearchDB(), "postgresql");

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        researchDB.registerArgumentFactory(new JodaArgumentFactory());
        researchDB.registerContainerFactory(new OptionalContainerFactory());
        researchDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        researchDB.registerContainerFactory(new ImmutableListContainerFactory());
        researchDB.registerContainerFactory(new ImmutableSetContainerFactory());

        final LabelDAO labelDAO = commonDB.onDemand(LabelDAOImpl.class);
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        // TODO: create research DB DAOs here

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());
        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);

        final AmazonDynamoDBClientFactory featureStoreDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = featureStoreDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        //sleep HMM protobufs in teh cloud
        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
        final String sleepHmmTableName = configuration.getSleepHmmDBConfiguration().getTableName();
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);

        final String priorDbTableName = configuration.getOnlineHmmModelsConfiguration().getTableName();
        final AmazonDynamoDB priorsDb = dynamoDBClientFactory.getForEndpoint(configuration.getOnlineHmmModelsConfiguration().getEndpoint());
        final OnlineHmmModelsDAO priorsDAO = OnlineHmmModelsDAODynamoDB.create(priorsDb,priorDbTableName);

        final String modelDbTableName = configuration.getFeatureExtractionConfiguration().getTableName();
        final AmazonDynamoDB modelsDb = dynamoDBClientFactory.getForEndpoint(configuration.getFeatureExtractionConfiguration().getEndpoint());
        final FeatureExtractionModelsDAO modelDAO = new FeatureExtractionModelsDAODynamoDB(modelsDb,modelDbTableName);

        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getCalibrationConfiguration().getEndpoint());
        final CalibrationDAO calibrationDAO = new CalibrationDynamoDB(calibrationDynamoDBClient, configuration.getCalibrationConfiguration().getTableName());


        //ring time history
        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeHistoryDynamoDBClient,
                configuration.getRingTimeHistoryDBConfiguration().getTableName());

        //sleep stats
        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepStatsDynamoConfiguration().getEndpoint());
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                configuration.getSleepStatsDynamoConfiguration().getTableName(),
                configuration.getSleepStatsVersion());

        /*  Timeline Log dynamo dB stuff */
        final String timelineLogTableName =   configuration.getTimelineLogDBConfiguration().getTableName();
        final AmazonDynamoDB timelineLogDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTimelineLogDBConfiguration().getEndpoint());
        final TimelineLogDAO timelineLogDAO = new TimelineLogDAODynamoDB(timelineLogDynamoDBClient,timelineLogTableName);

        final RolloutResearchModule module = new RolloutResearchModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        LOGGER.warn("DEBUG MODE = {}", configuration.getDebug());
        // Custom JSON handling for responses.
        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(configuration.getDebug()));
        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(accessTokenStore), "protected-resources", activityLogger));

        environment.getJerseyResourceConfig()
                .getResourceFilterFactories().add(CacheFilterFactory.class);
        environment.addResource(new DataScienceResource(accountDAO, trackerMotionDAO,
                deviceDataDAO, deviceDAO, userLabelDAO, feedbackDAO,timelineLogDAO,labelDAO,senseColorDAO));



        final TimelineProcessor timelineProcessor =  TimelineProcessor.createTimelineProcessor(trackerMotionDAO,deviceDAO,deviceDataDAO,ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAODynamoDB,accountDAO,sleepStatsDAODynamoDB,senseColorDAO,priorsDAO,modelDAO,calibrationDAO);

        environment.addResource(new PredictionResource(accountDAO,trackerMotionDAO,deviceDataDAO,deviceDAO, userLabelDAO,sleepHmmDAODynamoDB,feedbackDAO,timelineProcessor,senseColorDAO,modelDAO,priorsDAO));
        environment.addResource(new AccountInfoResource(accountDAO, deviceDAO));


    }
}
