package com.hello.suripu.core.logging;

import java.util.List;

/**
 * Created by ksg on 4/13/16
 */
public class KinesisBatchPutResult {
    public final int success;
    public final int batchSize;
    public final List<Boolean> successPuts;

    public KinesisBatchPutResult(final int success, final int batchSize, final List<Boolean> successPuts) {
        this.success = success;
        this.batchSize = batchSize;
        this.successPuts = successPuts;
    }
}
