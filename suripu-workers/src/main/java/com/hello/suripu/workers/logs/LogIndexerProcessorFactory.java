package com.hello.suripu.workers.logs;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.SenseEventsDAO;
import redis.clients.jedis.JedisPool;

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

        final IndexTankClient indexTankClient = new IndexTankClient(config.senseLogs().privateUrl());
        final String senseLogIndexPrefix = config.senseLogs().indexPrefix();
        final IndexTankClient.Index senseLogBackupIndex = indexTankClient.getIndex(config.senseLogs().backupIndexName());

        final AmazonDynamoDB amazonDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(config.getSenseEventsDynamoDBConfiguration().getEndpoint());
        final SenseEventsDAO senseEventsDAO = new SenseEventsDAO(amazonDynamoDB, config.getSenseEventsDynamoDBConfiguration().getTableName());

        final JedisPool jedisPool = new JedisPool(config.redisConfiguration().getHost(), config.redisConfiguration().getPort());

        return LogIndexerProcessor.create(indexTankClient, senseLogIndexPrefix, senseLogBackupIndex, senseEventsDAO, this.onBoardingLogDAO, jedisPool);
    }
}
