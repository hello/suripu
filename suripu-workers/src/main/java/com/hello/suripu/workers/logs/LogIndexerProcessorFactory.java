package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.flaptor.indextank.apiclient.IndexTankClient;

public class LogIndexerProcessorFactory implements IRecordProcessorFactory {

    private final String privateUrl;
    private final String indexName;

    public LogIndexerProcessorFactory(final String privateUrl, final String indexName) {
        this.privateUrl = privateUrl;
        this.indexName = indexName;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final IndexTankClient client = new IndexTankClient(privateUrl);
        final IndexTankClient.Index index = client.getIndex(indexName);
        return new LogIndexerProcessor(index);
    }
}
