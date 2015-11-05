package com.hello.suripu.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.filters.CacheFilterFactory;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.coredw.managers.DynamoDBClientManaged;
import com.hello.suripu.coredw.managers.KinesisClientManaged;
import com.hello.suripu.coredw.oauth.OAuthAuthenticator;
import com.hello.suripu.coredw.oauth.OAuthProvider;
import com.hello.suripu.service.cli.CreateDynamoDBTables;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.modules.RolloutModule;
import com.hello.suripu.service.resources.AudioResource;
import com.hello.suripu.service.resources.CheckResource;
import com.hello.suripu.service.resources.LogsResource;
import com.hello.suripu.service.resources.ProvisionResource;
import com.hello.suripu.service.resources.ReceiveResource;
import com.hello.suripu.service.resources.RegisterResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SuripuService extends Service<SuripuConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SuripuService.class);

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateDynamoDBTables());
    }

    @Override
    public void run(final SuripuConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());
        environment.getJerseyResourceConfig()
                .getResourceFilterFactories().add(CacheFilterFactory.class);

        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);

        // Checks Environment first and then instance profile.
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final ClientConfiguration clientConfig = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1).withMaxConnections(100);
        final AmazonDynamoDBClientFactory dynamoDBFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, clientConfig, configuration.dynamoDBConfiguration());


        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.SENSE_KEY_STORE);

        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);
        final String bucketName = configuration.getAudioBucketName();

        final AmazonDynamoDB mergedInfoDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.ALARM_INFO);
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedInfoDynamoDBClient, tableNames.get(DynamoDBTableName.ALARM_INFO));


        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.RING_TIME_HISTORY);
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeHistoryDynamoDBClient, tableNames.get(DynamoDBTableName.RING_TIME_HISTORY));

        final AmazonDynamoDB otaHistoryDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.OTA_HISTORY);
        final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB = new OTAHistoryDAODynamoDB(otaHistoryDynamoDBClient, tableNames.get(DynamoDBTableName.OTA_HISTORY));

        final AmazonDynamoDB respCommandsDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.SYNC_RESPONSE_COMMANDS);
        final ResponseCommandsDAODynamoDB respCommandsDAODynamoDB = new ResponseCommandsDAODynamoDB(respCommandsDynamoDBClient, tableNames.get(DynamoDBTableName.SYNC_RESPONSE_COMMANDS));

        final AmazonDynamoDB fwVersionMapping = dynamoDBFactory.getForTable(DynamoDBTableName.FIRMWARE_VERSIONS);
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAO(fwVersionMapping, tableNames.get(DynamoDBTableName.FIRMWARE_VERSIONS));

        final AmazonDynamoDB fwUpgradePathDynamoDB = dynamoDBFactory.getForTable(DynamoDBTableName.FIRMWARE_UPGRADE_PATH);
        final FirmwareUpgradePathDAO firmwareUpgradePathDAO = new FirmwareUpgradePathDAO(fwUpgradePathDynamoDB, tableNames.get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH));

        // This is used to sign S3 urls with a shorter signature
        final AWSCredentials s3credentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return configuration.getAwsAccessKeyS3();
            }

            @Override
            public String getAWSSecretKey() {
                return configuration.getAwsAccessSecretS3();
            }
        };

        final AmazonS3 amazonS3UrlSigner = new AmazonS3Client(s3credentials);

        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
        kinesisClient.setEndpoint(configuration.getKinesisConfiguration().getEndpoint());

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(
                kinesisClient,
                configuration.getKinesisConfiguration().getStreams()
        );


        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_KEY_STORE),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";

            final String prefix = String.format("%s.%s.%s", apiKey, env, "suripu-service");

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final FirmwareUpdateStore firmwareUpdateStore = FirmwareUpdateStore.create(
                otaHistoryDAODynamoDB,
                s3Client,
                "hello-firmware",
                amazonS3UrlSigner,
                configuration.getOTAConfiguration().getS3CacheExpireMinutes(),
                firmwareVersionMappingDAO,
                firmwareUpgradePathDAO);

        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);
        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(tokenStore), "protected-resources", activityLogger));

        final AmazonDynamoDB teamStoreDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.TEAMS);
        final TeamStore teamStore = new TeamStore(teamStoreDynamoDBClient, tableNames.get(DynamoDBTableName.TEAMS));

        final GroupFlipper groupFlipper = new GroupFlipper(teamStore, 30);

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";
        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.FEATURES);
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, tableNames.get(DynamoDBTableName.FEATURES), namespace);

        final RolloutModule module = new RolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBFactory.getForTable(DynamoDBTableName.CALIBRATION);

        // 300 sec = 5 minutes, which should maximize cache hitrate
        // TODO: add cache hitrate to metrics
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.createWithCacheConfig(calibrationDynamoDBClient, tableNames.get(DynamoDBTableName.CALIBRATION), 300);

        final ReceiveResource receiveResource = new ReceiveResource(
                senseKeyStore,
                kinesisLoggerFactory,
                mergedUserInfoDynamoDB,
                ringTimeHistoryDAODynamoDB,
                configuration.getDebug(),
                firmwareUpdateStore,
                groupFlipper,
                configuration.getSenseUploadConfiguration(),
                configuration.getOTAConfiguration(),
                respCommandsDAODynamoDB,
                configuration.getRingDuration(),
                calibrationDAO
        );


        environment.addResource(receiveResource);
        environment.addResource(new RegisterResource(deviceDAO,
                tokenStore,
                kinesisLoggerFactory,
                senseKeyStore,
                mergedUserInfoDynamoDB,
                groupFlipper,
                configuration.getDebug()));


        final DataLogger senseLogs = kinesisLoggerFactory.get(QueueName.LOGS);
        final LogsResource logsResource = new LogsResource(
                !configuration.getDebug(),
                senseKeyStore,
                senseLogs
        );

        environment.addResource(new CheckResource(senseKeyStore));
        environment.addResource(logsResource);

        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());

        final DataLogger audioDataLogger = kinesisLoggerFactory.get(QueueName.AUDIO_FEATURES);
        final DataLogger audioMetaDataLogger = kinesisLoggerFactory.get(QueueName.ENCODE_AUDIO);
        environment.addResource(
                new AudioResource(
                        s3Client,
                        bucketName,
                        audioDataLogger,
                        configuration.getDebug(),
                        audioMetaDataLogger,
                        senseKeyStore));

        environment.addResource(new ProvisionResource(senseKeyStore, groupFlipper));

        // Manage the lifecycle of our clients
        environment.manage(new DynamoDBClientManaged(senseKeyStoreDynamoDBClient));
        environment.manage(new DynamoDBClientManaged(teamStoreDynamoDBClient));
        environment.manage(new DynamoDBClientManaged(featuresDynamoDBClient));
        environment.manage(new DynamoDBClientManaged(senseKeyStoreDynamoDBClient));
        environment.manage(new KinesisClientManaged(kinesisClient));

        // Make sure we can connect
        environment.addHealthCheck(new DynamoDbHealthCheck(senseKeyStoreDynamoDBClient));
        environment.addHealthCheck(new DynamoDbHealthCheck(teamStoreDynamoDBClient));
        environment.addHealthCheck(new DynamoDbHealthCheck(featuresDynamoDBClient));
        environment.addHealthCheck(new KinesisHealthCheck(kinesisClient));
    }


}
