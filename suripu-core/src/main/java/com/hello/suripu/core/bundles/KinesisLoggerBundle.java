package com.hello.suripu.core.bundles;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
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
import java.util.Iterator;


public abstract class KinesisLoggerBundle<T extends Configuration> implements ConfiguredBundle<T> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KinesisLoggerBundle.class);

    private class KinesisAppender extends AppenderBase<ILoggingEvent> {

        private final AmazonKinesisAsyncClient kinesisAsyncClient;
        private final String topic;

        public KinesisAppender(final AmazonKinesisAsyncClient kinesisAsyncClient, final String topic) {
            this.kinesisAsyncClient = kinesisAsyncClient;
            this.topic = topic;
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
            this.kinesisAsyncClient.listStreams();
        }
        @Override
        public void stop() {
            super.stop();
            this.kinesisAsyncClient.shutdown();
        }

        @Override
        protected void append(ILoggingEvent eventObject) {
            final LoggingProtos.LogMessage logMessage = LoggingProtos.LogMessage.newBuilder()
                    .setMessage(String.format("[%s] %s - %s", eventObject.getLevel().levelStr, eventObject.getLoggerName(), eventObject.getFormattedMessage()))
                    .setOrigin("suripu-app")
                    .setTs(DateTime.now().getMillis())
                    .setProduction(false)
                    .build();

            final ByteBuffer byteBuffer = ByteBuffer.wrap(logMessage.toByteArray());
            final PutRecordResult recordResult = kinesisAsyncClient.putRecord(
                    new PutRecordRequest()
                            .withStreamName(topic)
                            .withData(byteBuffer)
                            .withPartitionKey("suripu-app")
            );
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
            final String topic = kinesisLoggerConfiguration.getStreamName();
            final Appender<ILoggingEvent> appender = new KinesisAppender(asyncClient, topic);
            final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            appender.setContext(lc);
            appender.setName("kinesis-logger");
            appender.addFilter(new Filter<ILoggingEvent>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
//                    if(!event.getLoggerName().contains("com.hello")) {
//                        return FilterReply.ACCEPT;
//                    }

                    return FilterReply.ACCEPT;
                }
            });

            final Logger httpLogger = (Logger) LoggerFactory.getLogger("http.request");
//            httpLogger.setAdditive(false);
            final LoggerContext context = httpLogger.getLoggerContext();
            appender.setContext(context);

            appender.start();
            httpLogger.addAppender(appender);
            root.addAppender(appender);
            final Iterator<Appender<ILoggingEvent>> itr = root.iteratorForAppenders();
            while(itr.hasNext()) {
                System.out.println(itr.next().getName());
            }

        }
    }

    public abstract KinesisLoggerConfiguration getConfiguration(T configuration);
}
