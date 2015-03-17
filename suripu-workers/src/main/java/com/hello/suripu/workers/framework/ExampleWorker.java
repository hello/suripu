package com.hello.suripu.workers.framework;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.yammer.dropwizard.config.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;

public class ExampleWorker extends WorkerEnvironmentCommand<ExampleWorkerConfiguration>  {

    public ExampleWorker(String command, String description) {
        super(command, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, ExampleWorkerConfiguration configuration) throws Exception {
        final Logger logger = LoggerFactory.getLogger(ExampleWorker.class);
        String queueName = "batch_pill_data";
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(100);
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);


        final IRecordProcessorFactory factory = new ExampleProcessorFactory();
        final com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker worker = new com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker(factory, kinesisConfig);
        worker.run();
    }



    private class ExampleProcessorFactory implements IRecordProcessorFactory {
        @Override
        public IRecordProcessor createProcessor() {
            return new ExampleProcessor();
        }
    }

    private class ExampleProcessor implements IRecordProcessor{
        private final Logger LOGGER = LoggerFactory.getLogger(ExampleProcessor.class);
        @Override
        public void initialize(String shardId) {

        }

        @Override
        public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
            for(final Record record : records) {
                LOGGER.warn("{}", record.getPartitionKey());
            }
        }

        @Override
        public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {

        }
    }
}
