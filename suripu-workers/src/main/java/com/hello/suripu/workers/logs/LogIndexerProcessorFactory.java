package com.hello.suripu.workers.logs;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;

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
        final AmazonDynamoDB amazonDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(config.getSenseEventsDynamoDBConfiguration().getEndpoint());
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(amazonDynamoDB, config.getSenseEventsDynamoDBConfiguration().getTableName());

        return LogIndexerProcessor.create(senseEventsDAO, this.onBoardingLogDAO);
    }
}
