package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.flaptor.indextank.apiclient.IndexTankClient;

public class LogIndexerProcessorFactory implements IRecordProcessorFactory {

    private final String privateUrl;
    private final String applicationIndexName;
    private final String senseIndexName;

    public LogIndexerProcessorFactory(final String privateUrl, final String applicationIndexName, final String senseIndexName) {
        this.privateUrl = privateUrl;
        this.applicationIndexName = applicationIndexName;
        this.senseIndexName = senseIndexName;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final IndexTankClient client = new IndexTankClient(privateUrl);
        final IndexTankClient.Index applicationIndex = client.getIndex(applicationIndexName);
        final IndexTankClient.Index senseIndex = client.getIndex(senseIndexName);
        return LogIndexerProcessor.create(applicationIndex, senseIndex);
    }
}
