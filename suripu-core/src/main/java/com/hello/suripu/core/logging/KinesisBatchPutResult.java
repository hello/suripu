package com.hello.suripu.core.logging;

import java.util.List;

/**
 * Created by ksg on 4/13/16
 */
public class KinesisBatchPutResult {
    public final int numSuccesses;
    public final int batchSize;
    public final List<Boolean> successPuts;

    public KinesisBatchPutResult(final int numSuccesses, final int batchSize, final List<Boolean> successPuts) {
        this.numSuccesses = numSuccesses;
        this.batchSize = batchSize;
        this.successPuts = successPuts;
    }
}
