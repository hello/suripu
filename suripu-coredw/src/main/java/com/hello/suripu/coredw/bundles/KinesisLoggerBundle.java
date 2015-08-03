package com.hello.suripu.coredw.bundles;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.nio.ByteBuffer;


public abstract class KinesisLoggerBundle<T extends Configuration> implements ConfiguredBundle<T> {
    private class KinesisAppender extends AppenderBase<ILoggingEvent> {

        private final LoggingProtos.BatchLogMessage.Builder batch;
        private final AmazonKinesisAsyncClient kinesisAsyncClient;
        private final String topic;
        private final Integer bufferSize;
        private final String origin;
        private final Boolean isProduction;
        private final String appVersion;


        public KinesisAppender(
                final AmazonKinesisAsyncClient kinesisAsyncClient,
                final KinesisLoggerConfiguration loggerConfiguration,
                final String appVersion) {
            this.kinesisAsyncClient = kinesisAsyncClient;
            this.topic = loggerConfiguration.getStreamName();
            this.bufferSize = loggerConfiguration.bufferSize();
            this.origin = loggerConfiguration.origin();
            this.appVersion = appVersion;
            this.batch = LoggingProtos.BatchLogMessage.newBuilder();
            this.isProduction = loggerConfiguration.isProduction();


        }

        private MessageFormatter formatter;

        public String getTopic() {
            return topic;
        }


        public MessageFormatter getFormatter() {
            return formatter;
        }

        public void setFormatter(MessageFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void start() {
            if (this.formatter == null) {
                this.formatter = new MessageFormatter();
            }
            super.start();
            this.kinesisAsyncClient.describeStream(topic);
        }
        @Override
        public void stop() {
            super.stop();
            if(batch.getMessagesCount() > 0) {
                flush();
            }
            this.kinesisAsyncClient.shutdown();
        }

        private void appendAndConvert(final ILoggingEvent eventObject) {

            final LoggingProtos.LogMessage logMessage = LoggingProtos.LogMessage.newBuilder()
                    .setMessage(String.format("[%s] %s - %s", eventObject.getLevel().levelStr, eventObject.getLoggerName(), eventObject.getFormattedMessage()))
                    .setLevel(eventObject.getLevel().toInteger())
                    .setOrigin(origin)
                    .setTs(DateTime.now().getMillis())
                    .setProduction(isProduction)
                    .build();
            batch.addMessages(logMessage);
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            this.appendAndConvert(eventObject);

            if(batch.getMessagesCount() >= bufferSize) {
                flush();
            }
        }

        private void flush() {
            batch.setReceivedAt(DateTime.now(DateTimeZone.UTC).getMillis());
            batch.setLogType(origin.contains("workers")  ? LoggingProtos.BatchLogMessage.LogType.WORKERS_LOG : LoggingProtos.BatchLogMessage.LogType.APPLICATION_LOG);
            batch.setAppVersion(appVersion);

            final LoggingProtos.BatchLogMessage tempBatch = batch.build();
            final PutRecordRequest request = new PutRecordRequest()
                    .withStreamName(topic)
                    .withData(ByteBuffer.wrap(tempBatch.toByteArray()))
                    .withPartitionKey(origin);

            kinesisAsyncClient.putRecordAsync(request, new AsyncHandler<PutRecordRequest, PutRecordResult>() {

                public void onError(Exception e) {
                    System.out.println(e.getMessage()); // Can't log this because otherwise it would be an infinite loop
                }

                public void onSuccess(PutRecordRequest request, PutRecordResult putRecordResult) {

                }
            });
            batch.clear();
        }
    }

    public void initialize(final Bootstrap<?> bootstrap) {


    }

    public void run(final T configuration, final Environment environment) {

        final KinesisLoggerConfiguration kinesisLoggerConfiguration = getConfiguration(configuration);
        if (kinesisLoggerConfiguration.isEnabled()) {
            final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
            final AmazonKinesisAsyncClient asyncClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
            final String appVersion = (getClass().getPackage().getImplementationVersion() == null) ? "0.0.0" : getClass().getPackage().getImplementationVersion();
            final Appender<ILoggingEvent> appender = new KinesisAppender(asyncClient, kinesisLoggerConfiguration, appVersion);
            final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            appender.setContext(lc);
            appender.setName("kinesis-logger");
            appender.addFilter(new Filter<ILoggingEvent>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
                    final Level level = event.getLevel();
                    if(level.isGreaterOrEqual(Level.valueOf(kinesisLoggerConfiguration.getLogLevel()))) {
                        return FilterReply.ACCEPT;
                    }
                    return FilterReply.DENY;
                }
            });

            appender.start();
            root.addAppender(appender);
        }
    }

    public abstract KinesisLoggerConfiguration getConfiguration(T configuration);
}
