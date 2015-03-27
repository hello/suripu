package com.hello.suripu.admin;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.admin.resources.v1.AccountResources;
import com.hello.suripu.admin.resources.v1.DataResources;
import com.hello.suripu.admin.resources.v1.DeviceResources;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
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

import javax.ws.rs.HEAD;
import java.util.TimeZone;

public class SuripuAdmin extends Service<SuripuAdminConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuAdmin.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuAdmin().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAdminConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
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

        environment.addResource(new PingResource());
        environment.addResource(new AccountResources(accountDAO));
        environment.addResource(new DeviceResources(deviceDAO, deviceDataDAO, trackerMotionDAO, accountDAO, mergedUserInfoDynamoDB, senseKeyStore, pillKeyStore));
        environment.addResource(new DataResources(deviceDataDAO, deviceDAO, accountDAO));
    }
}
