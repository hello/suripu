package com.hello.suripu.core.actions;

/**
 * Created by ksg on 1/30/17
 */
public class ActionProcessor {
    private final ActionFirehoseDAO firehoseDAO;

    public ActionProcessor(final ActionFirehoseDAO firehoseDAO) {
        this.firehoseDAO = firehoseDAO;
    }
}

