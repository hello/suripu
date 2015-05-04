package com.hello.suripu.workers.logs;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.SenseEventsDAO;

public class LogIndexerProcessorFactory implements IRecordProcessorFactory {

    private final LogIndexerWorkerConfiguration config;
    private final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory;
    private final OnBoardingLogDAO onBoardingLogDAO;

    public LogIndexerProcessorFactory(final LogIndexerWorkerConfiguration config,
                                      final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory,
                                      final OnBoardingLogDAO onBoardingLogDAO) {
        this.config = config;
        this.amazonDynamoDBClientFactory = amazonDynamoDBClientFactory;
        this.onBoardingLogDAO = onBoardingLogDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {

        final IndexTankClient client = new IndexTankClient(config.applicationLogs().privateUrl());
        final IndexTankClient.Index applicationIndex = client.getIndex(config.applicationLogs().indexName());
        final IndexTankClient.Index senseIndex = client.getIndex(config.senseLogs().indexName());
        final IndexTankClient.Index workersIndex = client.getIndex(config.workersLogs().indexName());

        final AmazonDynamoDB amazonDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(config.getSenseEventsDynamoDBConfiguration().getEndpoint());
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(amazonDynamoDB, config.getSenseEventsDynamoDBConfiguration().getTableName());

        return LogIndexerProcessor.create(applicationIndex, senseIndex, workersIndex, senseEventsDAO, this.onBoardingLogDAO);
    }
}
