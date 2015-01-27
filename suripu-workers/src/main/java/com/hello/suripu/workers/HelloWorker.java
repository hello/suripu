package com.hello.suripu.workers;

import com.hello.suripu.workers.alarm.AlarmWorkerCommand;
import com.hello.suripu.workers.framework.Worker;
import com.hello.suripu.workers.insights.InsightsGeneratorWorkerCommand;
import com.hello.suripu.workers.logs.LogIndexerWorkerCommand;
import com.hello.suripu.workers.pill.PillWorkerCommand;
import com.hello.suripu.workers.pillscorer.PillScoreWorkerCommand;
import com.hello.suripu.workers.sense.SenseSaveWorkerCommand;
import com.hello.suripu.workers.timeline.TimelineWorkerCommand;
import com.yammer.dropwizard.config.Bootstrap;

import java.util.TimeZone;

public class HelloWorker extends Worker<HelloWorkerConfiguration> {

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new HelloWorker().run(args);
    }

    @Override
    public void initialize(Bootstrap<HelloWorkerConfiguration> bootstrap) {
        bootstrap.addCommand(new PillWorkerCommand("pill", "all things about pill"));
        bootstrap.addCommand(new PillScoreWorkerCommand("pillscorer", "scoring sleep pill data"));
        bootstrap.addCommand(new SenseSaveWorkerCommand("sense_save", "saving sense sensor data"));
        bootstrap.addCommand(new AlarmWorkerCommand("smart_alarm", "Start smart alarm worker"));
        bootstrap.addCommand(new LogIndexerWorkerCommand("index_logs", "Indexes logs from Kinesis stream into searchify index"));
        bootstrap.addCommand(new InsightsGeneratorWorkerCommand("insights_generator", "generate insights for users"));
        bootstrap.addCommand(new TimelineWorkerCommand("timeline", "generate timeline for users"));
    }

}
