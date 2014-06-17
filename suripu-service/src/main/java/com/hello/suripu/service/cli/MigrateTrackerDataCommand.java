package com.hello.suripu.service.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrackerMotionDAODynamoDB;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.skife.jdbi.v2.DBI;

/**
 * Created by pangwu on 6/3/14.
 */
public class MigrateTrackerDataCommand extends ConfiguredCommand<SuripuConfiguration> {

    public MigrateTrackerDataCommand(){
        super("migrate_tracker_data", "Migrate tracker data from Postgres to DynamoDB");

    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("account_id").nargs("?").help("The account_id from Account table, please consult Tim.");
        subparser.addArgument("start_date").nargs("?").help("The start date of the night to migrate data with. Formatted as yyyy-MM-dd");
        subparser.addArgument("end_date").nargs("?").help("The end date of the night to migrate data with. Formatted as yyyy-MM-dd");
    }

    @Override
    protected void run(Bootstrap<SuripuConfiguration> bootstrap, Namespace namespace, SuripuConfiguration configuration) throws Exception {
        final DateTime startDate = DateTime.parse(namespace.getString("start_date") + " PST", DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT + " z"));
        final DateTime endDate = DateTime.parse(namespace.getString("end_date") + " PST", DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT + " z"));
        long accountId = Long.valueOf(namespace.getString("account_id"));

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);

        dynamoDBClient.setEndpoint(configuration.getMotionDBConfiguration().getEndpoint());
        final String tableName = configuration.getMotionDBConfiguration().getTableName();

        final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB = new TrackerMotionDAODynamoDB(dynamoDBClient, tableName);


        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource = managedDataSourceFactory.build(configuration.getDatabaseConfiguration());


        final DBI jdbi = new DBI(dataSource);
        jdbi.registerArgumentFactory(new OptionalArgumentFactory(configuration.getDatabaseConfiguration().getDriverClass()));
        jdbi.registerContainerFactory(new ImmutableListContainerFactory());
        jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new JodaArgumentFactory());


        final TrackerMotionDAO trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);


        final ImmutableList<TrackerMotion> oldDataFromPostgres = trackerMotionDAO.getBetween(accountId,
                startDate,
                endDate.plusHours(23).plusMinutes(59).plusSeconds(59).plusMillis(999));

        trackerMotionDAODynamoDB.setTrackerMotions(accountId, oldDataFromPostgres);
        System.out.println("Migrate data for account_id: " + accountId + " from " + startDate + " to " + endDate + " succeeded");

    }

}
