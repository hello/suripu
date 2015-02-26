package com.hello.suripu.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Joiner;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.filters.CacheFilterFactory;
import com.hello.suripu.core.firmware.FirmwareUpdateDAO;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.managers.DynamoDBClientManaged;
import com.hello.suripu.core.managers.KinesisClientManaged;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.service.cli.CreateKeyStoreDynamoDBTable;
import com.hello.suripu.service.cli.CreatePillKeyStoreDynamoDBTable;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.modules.RolloutModule;
import com.hello.suripu.service.resources.AudioResource;
import com.hello.suripu.service.resources.CheckResource;
import com.hello.suripu.service.resources.DownloadResource;
import com.hello.suripu.service.resources.LogsResource;
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

import java.net.InetAddress;
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
        bootstrap.addCommand(new CreateKeyStoreDynamoDBTable());
        bootstrap.addCommand(new CreatePillKeyStoreDynamoDBTable());
        bootstrap.addBundle(new KinesisLoggerBundle<SuripuConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(final SuripuConfiguration configuration) {
                return configuration.kinesisLoggerConfiguration();
            }
        });
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

        final FirmwareUpdateDAO firmwareUpdateDAO = commonDB.onDemand(FirmwareUpdateDAO.class);

        // Checks Environment first and then instance profile.
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB dynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);
        final String bucketName = configuration.getAudioBucketName();

        final AmazonDynamoDBClientFactory dynamoDBFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB mergedInfoDynamoDBClient = dynamoDBFactory.getForEndpoint(configuration.getAlarmInfoDynamoDBConfiguration().getEndpoint());

        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedInfoDynamoDBClient,
                configuration.getAlarmInfoDynamoDBConfiguration().getTableName());

        final AmazonDynamoDBClientFactory ringTimeHistoryDynamoDBFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = ringTimeHistoryDynamoDBFactory.getForEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());

        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeHistoryDynamoDBClient,
                configuration.getRingTimeHistoryDBConfiguration().getTableName());

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

        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getTableName(),
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
            final String hostName = InetAddress.getLocalHost().getHostName();

            final String prefix = String.format("%s.%s.%s", apiKey, env, hostName);

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final FirmwareUpdateStore firmwareUpdateStore = new FirmwareUpdateStore(firmwareUpdateDAO, s3Client, "hello-firmware", amazonS3UrlSigner);

        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);
        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(tokenStore), "protected-resources", activityLogger));
        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        final TeamStore teamStore = new TeamStore(dynamoDBClient, "teams");
        final GroupFlipper groupFlipper = new GroupFlipper(teamStore, 30);

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(dynamoDBClient, "features", namespace);

        final RolloutModule module = new RolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        final ReceiveResource receiveResource = new ReceiveResource(
                senseKeyStore,
                kinesisLoggerFactory,
                mergedUserInfoDynamoDB,
                ringTimeHistoryDAODynamoDB,
                configuration.getDebug(),
                // the room condition in config file is intentionally left there, just in case we figure out it is still useful.
                // Let's remove it in the next next deploy.
                firmwareUpdateStore,
                groupFlipper,
                configuration.getSenseUploadConfiguration(),
                configuration.getOTAConfiguration()
        );




        environment.addResource(receiveResource);
        environment.addResource(new RegisterResource(deviceDAO,
                tokenStore,
                kinesisLoggerFactory,
                senseKeyStore,
                mergedUserInfoDynamoDB,
                groupFlipper,
                configuration.getDebug()));
        final LogsResource logsResource = new LogsResource(
                configuration.getIndexLogConfiguration().getPrivateUrl(),
                configuration.getIndexLogConfiguration().getIndexName(),
                senseKeyStore
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

        environment.addResource(new DownloadResource(s3Client, "hello-firmware"));

        // Manage the lifecycle of our clients
        environment.manage(new DynamoDBClientManaged(dynamoDBClient));
        environment.manage(new KinesisClientManaged(kinesisClient));

        // Make sure we can connect
        environment.addHealthCheck(new DynamoDbHealthCheck(dynamoDBClient));
        environment.addHealthCheck(new KinesisHealthCheck(kinesisClient));
    }


}
