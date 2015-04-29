package com.hello.suripu.admin;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.cli.CreateDynamoDBTables;
import com.hello.suripu.admin.cli.ScanSerialNumbers;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.admin.db.FirmwareVersionMappingDAO;
import com.hello.suripu.admin.resources.v1.AccountResources;
import com.hello.suripu.admin.resources.v1.ApplicationResources;
import com.hello.suripu.admin.resources.v1.DataResources;
import com.hello.suripu.admin.resources.v1.DeviceResources;
import com.hello.suripu.admin.resources.v1.EventsResources;
import com.hello.suripu.admin.resources.v1.FeaturesResources;
import com.hello.suripu.admin.resources.v1.FirmwareResource;
import com.hello.suripu.admin.resources.v1.InspectionResources;
import com.hello.suripu.admin.resources.v1.TeamsResources;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
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
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.TimeZone;

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
        bootstrap.addBundle(new KinesisLoggerBundle<SuripuAdminConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(final SuripuAdminConfiguration configuration) {
                return configuration.getKinesisLoggerConfiguration();
            }
        });
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

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final DeviceDAOAdmin deviceDAOAdmin = commonDB.onDemand(DeviceDAOAdmin.class);
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getUserInfoDynamoDBConfiguration().getEndpoint());
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                mergedUserInfoDynamoDBClient, configuration.getUserInfoDynamoDBConfiguration().getTableName()
        );

        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getSenseKeyStoreDynamoDBConfiguration().getEndpoint());
        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                configuration.getSenseKeyStoreDynamoDBConfiguration().getTableName(),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final AmazonDynamoDB pillKeyStoreDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPillKeyStoreDynamoDBConfiguration().getEndpoint());
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(
                pillKeyStoreDynamoDBClient,
                configuration.getPillKeyStoreDynamoDBConfiguration().getTableName(),
                "9876543219876543".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );
        final AmazonDynamoDB passwordResetDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPasswordResetDBConfiguration().getEndpoint());
        final PasswordResetDB passwordResetDB = PasswordResetDB.create(passwordResetDynamoDBClient, configuration.getPasswordResetDBConfiguration().getTableName());

        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(configuration.getDebug()));
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);

        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

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

        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, configuration.getFeaturesDynamoDBConfiguration().getTableName(), namespace);

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTeamsDynamoDBConfiguration().getEndpoint());
        final TeamStore teamStore = new TeamStore(teamStoreDBClient, configuration.getTeamsDynamoDBConfiguration().getTableName());

        final AmazonDynamoDB senseEventsDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getSenseEventsDBConfiguration().getEndpoint());
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(senseEventsDBClient, configuration.getSenseEventsDBConfiguration().getTableName());


        final AmazonDynamoDB fwVersionMapping = dynamoDBClientFactory.getForEndpoint(configuration.getFirmwareVersionsDynamoDBConfiguration().getEndpoint());
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAO(fwVersionMapping, configuration.getFirmwareVersionsDynamoDBConfiguration().getTableName());

        final AmazonDynamoDB otaHistoryClient = dynamoDBClientFactory.getForEndpoint(configuration.getOTAHistoryDBConfiguration().getEndpoint());
        final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB = new OTAHistoryDAODynamoDB(otaHistoryClient, configuration.getOTAHistoryDBConfiguration().getTableName());

        final AmazonDynamoDB respCommandsDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getResponseCommandsDBConfiguration().getEndpoint());
        final ResponseCommandsDAODynamoDB respCommandsDAODynamoDB = new ResponseCommandsDAODynamoDB(respCommandsDynamoDBClient, configuration.getResponseCommandsDBConfiguration().getTableName());

        environment.addResource(new PingResource());
        environment.addResource(new AccountResources(accountDAO, passwordResetDB, deviceDAO));
        environment.addResource(new DeviceResources(deviceDAO, deviceDAOAdmin, deviceDataDAO, trackerMotionDAO, accountDAO, mergedUserInfoDynamoDB, senseKeyStore, pillKeyStore, jedisPool, pillHeartBeatDAO));
        environment.addResource(new DataResources(deviceDataDAO, deviceDAO, accountDAO, userLabelDAO, trackerMotionDAO));
        environment.addResource(new ApplicationResources(applicationStore));
        environment.addResource(new FeaturesResources(featureStore));
        environment.addResource(new TeamsResources(teamStore));
        environment.addResource(new FirmwareResource(jedisPool, firmwareVersionMappingDAO, otaHistoryDAODynamoDB, respCommandsDAODynamoDB));
        environment.addResource(new EventsResources(senseEventsDAO));
        environment.addResource(new InspectionResources(deviceDAOAdmin));

    }
}
