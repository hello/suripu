package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.flaptor.indextank.apiclient.IndexTankClient;

public class LogIndexerProcessorFactory implements IRecordProcessorFactory {

    private final LogIndexerWorkerConfiguration logIndexerWorkerConfiguration;

    public LogIndexerProcessorFactory(final LogIndexerWorkerConfiguration logIndexerWorkerConfiguration) {
        this.logIndexerWorkerConfiguration = logIndexerWorkerConfiguration;
    }

    @Override
    public IRecordProcessor createProcessor() {

        final IndexTankClient client = new IndexTankClient(logIndexerWorkerConfiguration.applicationLogs().privateUrl());
        final IndexTankClient.Index applicationIndex = client.getIndex(logIndexerWorkerConfiguration.applicationLogs().indexName());
        final IndexTankClient.Index senseIndex = client.getIndex(logIndexerWorkerConfiguration.senseLogs().indexName());
        final IndexTankClient.Index workersIndex = client.getIndex(logIndexerWorkerConfiguration.workersLogs().indexName());
        return LogIndexerProcessor.create(applicationIndex, senseIndex, workersIndex);
    }
}
