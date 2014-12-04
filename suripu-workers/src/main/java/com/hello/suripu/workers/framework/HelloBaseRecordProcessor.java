package com.hello.suripu.workers.framework;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.hello.suripu.core.ObjectGraphRoot;
import com.librato.rollout.RolloutClient;

import javax.inject.Inject;

/**
 * Created by pangwu on 12/4/14.
 */
public abstract class HelloBaseRecordProcessor implements IRecordProcessor {
    @Inject
    protected RolloutClient feature;

    public HelloBaseRecordProcessor(){
        ObjectGraphRoot.getInstance().inject(this);
    }
}
