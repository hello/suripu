package com.hello.suripu.service;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.JodaArgumentFactory;
import com.hello.suripu.core.KinesisLogger;
import com.hello.suripu.core.aws.AWSHelper;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.EventDAO;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.PublicKeyStoreDynamoDB;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
import com.hello.suripu.core.managers.DynamoDBClientManaged;
import com.hello.suripu.core.managers.KinesisClientManaged;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.OAuthTokenStore;
import com.hello.suripu.core.oauth.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.PersistentApplicationStore;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.db.DeviceDataDAO;
import com.hello.suripu.service.resources.ReceiveResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuripuService extends Service<SuripuConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SuripuService.class);

    public static void main(String[] args) throws Exception {
        new SuripuService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
    }

    @Override
    public void run(SuripuConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final DeviceDataDAO dao = jdbi.onDemand(DeviceDataDAO.class);
        final AccessTokenDAO accessTokenDAO = jdbi.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = jdbi.onDemand(ApplicationsDAO.class);
        final ScoreDAO scoreDAO = jdbi.onDemand(ScoreDAO.class);
        final TrackerMotionDAO trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);
        final EventDAO eventDAO = jdbi.onDemand(EventDAO.class);


        final AWSCredentialsProvider awsCredentialsProvider = AWSHelper.getCredentials();

        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);

        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
        kinesisClient.setEndpoint(configuration.getKinesisConfiguration().getEndpoint());

        final KinesisLogger kinesisLogger = new KinesisLogger(kinesisClient, configuration.getKinesisConfiguration().getStreamName());

        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final PublicKeyStore publicKeyStore = new PublicKeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getKeyStoreTable()
        );

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        environment.addProvider(new OAuthProvider<AccessToken>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new ReceiveResource(dao, deviceDAO, scoreDAO, trackerMotionDAO, publicKeyStore, kinesisLogger));
        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());

        // Manage the lifecycle of our clients
        environment.manage(new DynamoDBClientManaged(dynamoDBClient));
        environment.manage(new KinesisClientManaged(kinesisClient));

        // Make sure we can connect
        environment.addHealthCheck(new DynamoDbHealthCheck(dynamoDBClient));
        environment.addHealthCheck(new KinesisHealthCheck(kinesisClient));
    }


}
