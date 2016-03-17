package com.hello.suripu.queue.models;

import com.yammer.metrics.core.HealthCheck;

/**
 * Created by ksg on 3/15/16
 */

public class QueueHealthCheck extends HealthCheck{
    private String name;

    public QueueHealthCheck(final String name) {
        super("health-check");
        this.name = name;
    }

    @Override
    public Result check() throws Exception {
        final String testString = String.format("%s", name);
        if (!testString.contains(name)) {
            return Result.unhealthy("Fail health check!");
        }
        return Result.healthy();
    }
}
