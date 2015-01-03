package com.hello.suripu.core.bundles;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
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

        public KinesisAppender(
                final AmazonKinesisAsyncClient kinesisAsyncClient,
                final KinesisLoggerConfiguration loggerConfiguration) {
            this.kinesisAsyncClient = kinesisAsyncClient;
            this.topic = loggerConfiguration.getStreamName();
            this.bufferSize = loggerConfiguration.bufferSize();
            this.origin = loggerConfiguration.origin();
            this.batch = LoggingProtos.BatchLogMessage.newBuilder();
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
            this.kinesisAsyncClient.shutdown();
        }

        private void appendAndConvert(final ILoggingEvent eventObject) {

            final LoggingProtos.LogMessage logMessage = LoggingProtos.LogMessage.newBuilder()
                    .setMessage(String.format("[%s] %s - %s", eventObject.getLevel().levelStr, eventObject.getLoggerName(), eventObject.getFormattedMessage()))
                    .setLevel(eventObject.getLevel().toInteger())
                    .setOrigin(origin)
                    .setTs(DateTime.now().getMillis())
                    .setProduction(false)  // TODO: configure this
                    .build();
            batch.addMessages(logMessage);
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            this.appendAndConvert(eventObject);

            if(batch.getMessagesCount() >= bufferSize) {
                final LoggingProtos.BatchLogMessage tempBatch = batch.build();
                final PutRecordRequest request = new PutRecordRequest()
                        .withStreamName(topic)
                        .withData(ByteBuffer.wrap(tempBatch.toByteArray()))
                        .withPartitionKey(origin);

                kinesisAsyncClient.putRecordAsync(request, new AsyncHandler<PutRecordRequest, PutRecordResult>() {
                    @Override
                    public void onError(Exception e) {
                        System.out.println(e.getMessage());
                    }

                    @Override
                    public void onSuccess(PutRecordRequest request, PutRecordResult putRecordResult) {

                    }
                });
                batch.clear();
            }
        }
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {


    }

    public void run(final T configuration, final Environment environment) {

        final KinesisLoggerConfiguration kinesisLoggerConfiguration = getConfiguration(configuration);
        if (kinesisLoggerConfiguration.isEnabled()) {
            final AWSCredentialsProvider awsCredentialsProvider = new EnvironmentVariableCredentialsProvider();
            final AmazonKinesisAsyncClient asyncClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
            final Appender<ILoggingEvent> appender = new KinesisAppender(asyncClient, kinesisLoggerConfiguration);
            final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            appender.setContext(lc);
            appender.setName("kinesis-logger");
            appender.addFilter(new Filter<ILoggingEvent>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
                    final Level level = event.getLevel();
                    if(level.isGreaterOrEqual(Level.DEBUG)) {
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
