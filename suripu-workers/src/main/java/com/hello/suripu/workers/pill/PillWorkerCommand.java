package com.hello.suripu.workers.pill;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
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

public final class PillWorkerCommand extends ConfiguredCommand<PillWorkerConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillWorkerCommand.class);

    public PillWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    public final void run(Bootstrap<PillWorkerConfiguration> bootstrap, Namespace namespace, PillWorkerConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorDB());

        final DBI jdbiSensor = new DBI(sensorDataSource);
        jdbiSensor.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorDB().getDriverClass()));
        jdbiSensor.registerContainerFactory(new ImmutableListContainerFactory());
        jdbiSensor.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbiSensor.registerContainerFactory(new OptionalContainerFactory());
        jdbiSensor.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = jdbiSensor.onDemand(TrackerMotionDAO.class);

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.PILL_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final PillHeartBeatDAO heartBeatDAO = jdbiSensor.onDemand(PillHeartBeatDAO.class);
        final AmazonDynamoDB pillKeyStoreDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(pillKeyStoreDynamoDB,configuration.getKeyStore().getTableName(), new byte[16], 120);
        final IRecordProcessorFactory factory = new SavePillDataProcessorFactory(trackerMotionDAO, configuration.getBatchSize(), kinesisConfig, heartBeatDAO, pillKeyStore);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();

//        final S3Object headerBlob = s3Client.getObject("hello-data", "49540234611938095003552389818111531279102041887843811329-49540234611938095003552389818111531279102041887843811329-header");
//        final InputProtos.PillBlobHeader header = InputProtos.PillBlobHeader.parseFrom(headerBlob.getObjectContent());
//
//        final S3Object dataBlob = s3Client.getObject("hello-data", "49540234611938095003552389818111531279102041887843811329-49540234611938095003552389818111531279102041887843811329");
//        final InputProtos.PillBlob blob = InputProtos.PillBlob.parseFrom(dataBlob.getObjectContent());
//
//        System.out.println("Header -> Num of records = " + header.getNumItems());
//        System.out.println("Blob -> Num of records = " + blob.getItemsCount());
    }
}
