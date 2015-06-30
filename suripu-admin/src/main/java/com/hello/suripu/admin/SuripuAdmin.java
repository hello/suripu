package com.hello.suripu.admin;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.cli.CreateDynamoDBTables;
import com.hello.suripu.admin.cli.ManageKinesisStreams;
import com.hello.suripu.admin.cli.PopulateColors;
import com.hello.suripu.admin.cli.ScanFWVersion;
import com.hello.suripu.admin.cli.ScanSerialNumbers;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.admin.resources.v1.AccountResources;
import com.hello.suripu.admin.resources.v1.AlarmResources;
import com.hello.suripu.admin.resources.v1.ApplicationResources;
import com.hello.suripu.admin.resources.v1.DataResources;
import com.hello.suripu.admin.resources.v1.DeviceResources;
import com.hello.suripu.admin.resources.v1.DiagnosticResources;
import com.hello.suripu.admin.resources.v1.EventsResources;
import com.hello.suripu.admin.resources.v1.FeaturesResources;
import com.hello.suripu.admin.resources.v1.FirmwareResource;
import com.hello.suripu.admin.resources.v1.InspectionResources;
import com.hello.suripu.admin.resources.v1.OnBoardingLogResource;
import com.hello.suripu.admin.resources.v1.PCHResources;
import com.hello.suripu.admin.resources.v1.TeamsResources;
import com.hello.suripu.admin.resources.v1.TokenResources;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOAdmin;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.PillViewsDynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.diagnostic.DiagnosticDAO;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.tracking.TrackingDAO;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class SuripuAdmin extends Service<SuripuAdminConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuAdmin.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new SuripuAdmin().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAdminConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateDynamoDBTables());
        bootstrap.addCommand(new ScanSerialNumbers());
        bootstrap.addCommand(new ScanFWVersion());
        bootstrap.addCommand(new PopulateColors());
        bootstrap.addCommand(new ManageKinesisStreams());
    }

    @Override
    public void run(SuripuAdminConfiguration configuration, Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI sensorsDB = factory.build(environment, configuration.getSensorsDB(), "postgresql");

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";

            final String prefix = String.format("%s.%s.%s", apiKey, env, "suripu-admin");

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));

            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());

        // Common DB
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final AccountDAOAdmin accountDAOAdmin = commonDB.onDemand(AccountDAOAdmin.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final DeviceDAOAdmin deviceDAOAdmin = commonDB.onDemand(DeviceDAOAdmin.class);
        final OnBoardingLogDAO onBoardingLogDAO = commonDB.onDemand(OnBoardingLogDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final TrackingDAO trackingDAO = commonDB.onDemand(TrackingDAO.class);
        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);

        // Sensor DB
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final DiagnosticDAO diagnosticDAO = sensorsDB.onDemand(DiagnosticDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);






        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final AmazonDynamoDB mergedUserInfoDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.ALARM_INFO);
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                mergedUserInfoDynamoDBClient, configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.ALARM_INFO)
        );

        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SENSE_KEY_STORE);
        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final AmazonDynamoDB pillKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_KEY_STORE);
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(
                pillKeyStoreDynamoDBClient,
                tableNames.get(DynamoDBTableName.PILL_KEY_STORE),
                "9876543219876543".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );
        final AmazonDynamoDB passwordResetDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PASSWORD_RESET);
        final PasswordResetDB passwordResetDB = PasswordResetDB.create(
                passwordResetDynamoDBClient,
                tableNames.get(DynamoDBTableName.PASSWORD_RESET)
        );

        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(configuration.getDebug()));
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);

        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore, configuration.getTokenExpiration());

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);
        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);
        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(accessTokenStore), "protected-resources", activityLogger));

        final JedisPool jedisPool = new JedisPool(
                configuration.getRedisConfiguration().getHost(),
                configuration.getRedisConfiguration().getPort()
        );

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";

        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FEATURES, FeatureStore.class);
        final FeatureStore featureStore = new FeatureStore(
                featuresDynamoDBClient,
                tableNames.get(DynamoDBTableName.FEATURES),
                namespace
        );

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TEAMS, TeamStore.class);
        final TeamStore teamStore = new TeamStore(teamStoreDBClient, tableNames.get(DynamoDBTableName.TEAMS));

        final AmazonDynamoDB senseEventsDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SENSE_EVENTS, SenseEventsDAO.class);
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(senseEventsDBClient, tableNames.get(DynamoDBTableName.SENSE_EVENTS));


        final AmazonDynamoDB fwVersionMapping = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_VERSIONS, FirmwareVersionMappingDAO.class);
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAO(fwVersionMapping, tableNames.get(DynamoDBTableName.FIRMWARE_VERSIONS));

        final AmazonDynamoDB otaHistoryClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.OTA_HISTORY, OTAHistoryDAODynamoDB.class);
        final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB = new OTAHistoryDAODynamoDB(otaHistoryClient, tableNames.get(DynamoDBTableName.OTA_HISTORY));

        final AmazonDynamoDB respCommandsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SYNC_RESPONSE_COMMANDS, ResponseCommandsDAODynamoDB.class);
        final ResponseCommandsDAODynamoDB respCommandsDAODynamoDB = new ResponseCommandsDAODynamoDB(respCommandsDynamoDBClient, tableNames.get(DynamoDBTableName.SYNC_RESPONSE_COMMANDS));

        final AmazonDynamoDB fwUpgradePathDynamoDB = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.FIRMWARE_UPGRADE_PATH, FirmwareUpgradePathDAO.class);
        final FirmwareUpgradePathDAO firmwareUpgradePathDAO = new FirmwareUpgradePathDAO(fwUpgradePathDynamoDB, tableNames.get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH));

        final AmazonDynamoDB sensorsViewsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.SENSE_LAST_SEEN, SensorsViewsDynamoDB.class);
        final SensorsViewsDynamoDB sensorsViewsDynamoDB = new SensorsViewsDynamoDB(
                sensorsViewsDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_PREFIX),
                tableNames.get(DynamoDBTableName.SENSE_LAST_SEEN)
        );


        final AmazonDynamoDB tzHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TIMEZONE_HISTORY, TimeZoneHistoryDAODynamoDB.class);
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                tzHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.TIMEZONE_HISTORY)
        );

        final AmazonDynamoDB smartAlarmHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TIMEZONE_HISTORY, SmartAlarmLoggerDynamoDB.class);
        final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB = new SmartAlarmLoggerDynamoDB(
                smartAlarmHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.SMART_ALARM_LOG)
        );

        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.TIMEZONE_HISTORY, RingTimeHistoryDAODynamoDB.class);
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
                ringTimeHistoryDynamoDBClient,
                tableNames.get(DynamoDBTableName.RING_TIME_HISTORY)
        );

        final AmazonDynamoDB pillViewsDynamoDBClient = dynamoDBClientFactory.getInstrumented(DynamoDBTableName.PILL_LAST_SEEN, PillViewsDynamoDB.class);
        final PillViewsDynamoDB pillViewsDynamoDB = new PillViewsDynamoDB(
                pillViewsDynamoDBClient,
                "", // TODO FIX THIS
                tableNames.get(DynamoDBTableName.PILL_LAST_SEEN)
        );

        environment.addResource(new PingResource());
        environment.addResource(new AccountResources(accountDAO, passwordResetDB, deviceDAO, accountDAOAdmin,
                timeZoneHistoryDAODynamoDB, smartAlarmLoggerDynamoDB, ringTimeHistoryDAODynamoDB));

        final DeviceResources deviceResources = new DeviceResources(deviceDAO, deviceDAOAdmin, deviceDataDAO, trackerMotionDAO, accountDAO,
                mergedUserInfoDynamoDB, senseKeyStore, pillKeyStore, jedisPool, pillHeartBeatDAO, senseColorDAO, respCommandsDAODynamoDB,pillViewsDynamoDB, sensorsViewsDynamoDB);

        environment.addResource(deviceResources);
        environment.addResource(new DataResources(deviceDataDAO, deviceDAO, accountDAO, userLabelDAO, trackerMotionDAO, sensorsViewsDynamoDB,senseColorDAO));
        environment.addResource(new ApplicationResources(applicationStore));
        environment.addResource(new FeaturesResources(featureStore));
        environment.addResource(new TeamsResources(teamStore));
        environment.addResource(new FirmwareResource(jedisPool, firmwareVersionMappingDAO, otaHistoryDAODynamoDB, respCommandsDAODynamoDB, firmwareUpgradePathDAO, deviceDAO, sensorsViewsDynamoDB, teamStore));
        environment.addResource(new EventsResources(senseEventsDAO));
        environment.addResource(new InspectionResources(deviceDAOAdmin));
        environment.addResource(new OnBoardingLogResource(accountDAO, onBoardingLogDAO));

        environment.addResource(
                new PCHResources(
                        senseKeyStoreDynamoDBClient, // we use the same endpoint for Sense and Pill keystore
                        tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                        tableNames.get(DynamoDBTableName.PILL_KEY_STORE),
                        senseColorDAO
                )
        );

        environment.addResource(new AlarmResources(mergedUserInfoDynamoDB, deviceDAO, accountDAO));
        environment.addResource(new DiagnosticResources(diagnosticDAO, accountDAO, deviceDAO, trackingDAO));
        environment.addResource(new TokenResources(accessTokenStore, applicationStore, accessTokenDAO, accountDAO));
    }
}
