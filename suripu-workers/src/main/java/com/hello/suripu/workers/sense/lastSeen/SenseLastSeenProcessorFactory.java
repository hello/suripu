package com.hello.suripu.workers.sense.lastSeen;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.WifiInfoDAO;

public class SenseLastSeenProcessorFactory implements IRecordProcessorFactory {
    private final Integer maxRecords;
    private final WifiInfoDAO wifiInfoDAO;

    public SenseLastSeenProcessorFactory(
            final Integer maxRecords,
            final WifiInfoDAO wifiInfoDAO) {
        this.maxRecords = maxRecords;
        this.wifiInfoDAO = wifiInfoDAO;

    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SenseLastSeenProcessor(maxRecords, wifiInfoDAO);
    }
}
