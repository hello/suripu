package com.hello.suripu.workers;

import com.hello.suripu.workers.framework.Worker;
import com.hello.suripu.workers.pill.PillWorkerCommand;
import com.hello.suripu.workers.pillscorer.PillScoreWorkerCommand;
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
    }

}
