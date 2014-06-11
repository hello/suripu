package com.hello.suripu.workers.pill;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.configuration.QueueNames;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
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
        final ImmutableMap<QueueNames, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueNames.PILL_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonS3Client s3Client= new AmazonS3Client(awsCredentialsProvider);

        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());

        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        final IRecordProcessorFactory factory = new S3RecordProcessorFactory(s3Client, "hello-data", kinesisConfig);
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
