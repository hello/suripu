package com.hello.suripu.workers.alarm;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmWorkerCommand extends ConfiguredCommand<AlarmWorkerConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmWorkerCommand.class);

    protected AlarmWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(final Bootstrap<AlarmWorkerConfiguration> bootstrap, final Namespace namespace, final AlarmWorkerConfiguration configuration) throws Exception {


        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI jdbi = new DBI(dataSource);
        jdbi.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        jdbi.registerContainerFactory(new ImmutableListContainerFactory());
        jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);
        final DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);

        final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = new MergedAlarmInfoDynamoDB(dynamoDBClient,
                configuration.getAlarmInfoDynamoDBConfiguration().getTableName());
        final RingTimeDAODynamoDB ringTimeDAODynamoDB = new RingTimeDAODynamoDB(dynamoDBClient, configuration.getRingTimeDBConfiguration().getTableName());


        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.MORPHEUS_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);


        final String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        final IRecordProcessorFactory factory = new AlarmRecordProcessorFactory(mergedAlarmInfoDynamoDB,
                ringTimeDAODynamoDB,
                trackerMotionDAO,
                configuration);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}
