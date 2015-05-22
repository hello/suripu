package com.hello.suripu.workers;

import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.workers.alarm.AlarmWorkerCommand;
import com.hello.suripu.workers.framework.WorkerConfiguration;
import com.hello.suripu.workers.insights.InsightsGeneratorWorkerCommand;
import com.hello.suripu.workers.logs.LogIndexerWorkerCommand;
import com.hello.suripu.workers.notifications.PushNotificationsWorkerCommand;
import com.hello.suripu.workers.pill.PillWorkerCommand;
import com.hello.suripu.workers.sense.SenseSaveWorkerCommand;
import com.hello.suripu.workers.timeline.TimelineWorkerCommand;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

public class HelloWorker extends Service<WorkerConfiguration> {

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new HelloWorker().run(args);
    }

    @Override
    public void initialize(Bootstrap<WorkerConfiguration> bootstrap) {
        bootstrap.addBundle(new KinesisLoggerBundle<WorkerConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(WorkerConfiguration configuration) {
                return configuration.getKinesisLoggerConfiguration();
            }
        });
        bootstrap.addCommand(new PillWorkerCommand("pill", "all things about pill"));
        bootstrap.addCommand(new SenseSaveWorkerCommand("sense_save", "saving sense sensor data"));
        bootstrap.addCommand(new AlarmWorkerCommand("smart_alarm", "Start smart alarm worker"));
        bootstrap.addCommand(new LogIndexerWorkerCommand("index_logs", "Indexes logs from Kinesis stream into searchify index"));
        bootstrap.addCommand(new InsightsGeneratorWorkerCommand("insights_generator", "generate insights for users"));
        bootstrap.addCommand(new TimelineWorkerCommand("timeline", "generate timeline for users"));
        bootstrap.addCommand(new PushNotificationsWorkerCommand("push", "send push notifications"));
    }

    @Override
    public void run(WorkerConfiguration configuration, Environment environment) throws Exception {
        // Do nothing
    }
}
