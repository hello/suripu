package com.hello.suripu.core.logging;

import java.util.List;

public interface DataLogger {
    void putAsync(String deviceId, byte[] payload);

    String put(String deviceId, byte[] payload);

    String putWithSequenceNumber(String deviceId, byte[] payload, String sequenceNumber);

    KinesisBatchPutResult putRecords(List<DataLoggerBatchPayload> payloadBatch);
}
