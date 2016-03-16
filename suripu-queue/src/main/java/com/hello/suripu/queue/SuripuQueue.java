package com.hello.suripu.queue;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.queue.cli.PopulateTimelineQueueCommand;
import com.hello.suripu.queue.configuration.SQSConfiguration;
import com.hello.suripu.queue.configuration.SuripuQueueConfiguration;
import com.hello.suripu.queue.models.QueueHealthCheck;
import com.hello.suripu.queue.models.SenseDataDAO;
import com.hello.suripu.queue.resources.QueueInsertResources;
import com.hello.suripu.queue.tasks.TimelineQueueInsertTask;
import com.hello.suripu.queue.workers.ProcessMessages;
import com.hello.suripu.queue.workers.TimelineQueueProcessor;
import com.hello.suripu.queue.workers.TimelineQueueWorkerCommand;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class SuripuQueue extends Service<SuripuQueueConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuQueue.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuQueue().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuQueueConfiguration> bootstrap) {

        bootstrap.addCommand(new TimelineQueueWorkerCommand(this, "timeline_generator", "generate timeline"));
        bootstrap.addCommand(new PopulateTimelineQueueCommand(this, "write_batch_messages", "insert queue message to generate timelines"));
    }

    @Override
    public void run(final SuripuQueueConfiguration configuration, final Environment environment) throws Exception {

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";

            final String prefix = String.format("%s.%s.%s", apiKey, env, "suripu-app");

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));

            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        final AWSCredentialsProvider provider= new DefaultAWSCredentialsProviderChain();
        final SQSConfiguration sqsConfig = configuration.getSqsConfiguration();
        final int maxConnections = sqsConfig.getSqsMaxConnections();
        final AmazonSQSAsync sqsAsync = new AmazonSQSAsyncClient(provider, new ClientConfiguration()
                .withMaxConnections(maxConnections)
                .withConnectionTimeout(500));
        final AmazonSQSAsync sqs = new AmazonSQSBufferedAsyncClient(sqsAsync);

        final Region region = Region.getRegion(Regions.US_EAST_1);
        sqs.setRegion(region);

        // get queue url
        final Optional<String> optionalSqsQueueUrl = TimelineQueueProcessor.getSQSQueueURL(sqs, sqsConfig.getSqsQueueName());
        if (!optionalSqsQueueUrl.isPresent()) {
            LOGGER.error("error=no-sqs-queue-found value=queue-name-{}", sqsConfig.getSqsQueueName());
            throw new Exception("Invalid queue name");
        }

        final String sqsQueueUrl = optionalSqsQueueUrl.get();


        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final DBI sensorDB = new DBI(managedDataSourceFactory.build(configuration.getSensorDB()));

        sensorDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorDB.registerContainerFactory(new OptionalContainerFactory());
        sensorDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        sensorDB.registerContainerFactory(new ImmutableListContainerFactory());
        sensorDB.registerContainerFactory(new ImmutableSetContainerFactory());

        final SenseDataDAO senseDataDAO = sensorDB.onDemand(SenseDataDAO.class);

        environment.addHealthCheck(new QueueHealthCheck("test"));
        environment.addResource(new QueueInsertResources());

        final TimelineQueueProcessor queueProcessor = new TimelineQueueProcessor(sqsQueueUrl, sqs, configuration.getSqsConfiguration());

        final ExecutorService executor = environment.managedExecutorService("timeline_queue", configuration.getNumGeneratorThreads(), configuration.getNumGeneratorThreads(), 2, TimeUnit.SECONDS);

        final ProcessMessages processMessages = new ProcessMessages(queueProcessor, provider, configuration, executor);
        environment.manage(processMessages);

        // task to insert messages into sqs queue
        environment.addTask(new TimelineQueueInsertTask(sqs, senseDataDAO, sqsQueueUrl));
    }
}
