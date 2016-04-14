package com.hello.suripu.core.logging;

import java.util.List;

/**
 * Created by ksg on 4/13/16
 */
public class KinesisBatchPutResult {
    public final int success;
    public final int batchSize;
    public final List<DataLoggerBatchPayload> failedRecords;

    public KinesisBatchPutResult(final int success, final int batchSize, final List<DataLoggerBatchPayload> failedRecords) {
        this.success = success;
        this.batchSize = batchSize;
        this.failedRecords = failedRecords;
    }
}
