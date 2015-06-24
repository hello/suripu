package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchBulkSettings;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchTransportClient;
import redis.clients.jedis.JedisPool;

public class ElasticSearchIndexerProcessorFactory implements IRecordProcessorFactory {

    private final LogIndexerWorkerConfiguration config;
    public ElasticSearchIndexerProcessorFactory(final LogIndexerWorkerConfiguration config){
        this.config = config;
    }

    @Override
    public IRecordProcessor createProcessor() {

        final JedisPool jedisPool = new JedisPool(config.redisConfiguration().getHost(), config.redisConfiguration().getPort());

        final ElasticSearchTransportClient elasticSearchTransportClient = ElasticSearchTransportClient.createWithDefaulSettings(config.getElasticSearchConfiguration().getHost(), config.getElasticSearchConfiguration().getTransportTCPPort());
        final ElasticSearchBulkSettings elasticSearchBulkSettings = new ElasticSearchBulkSettings(
                config.getElasticSearchConfiguration().getBulkActions(),
                config.getElasticSearchConfiguration().getBulkSizeInMegabyes(),
                config.getElasticSearchConfiguration().getFlushIntervalInSeconds(),
                config.getElasticSearchConfiguration().getConcurrentRequests()
        );
        final String elasticSearchIndexName = config.getElasticSearchConfiguration().getIndexName();
        return ElasticSearchIndexerProcessor.create(jedisPool, elasticSearchTransportClient.generateClient(), elasticSearchBulkSettings, elasticSearchIndexName);
    }
}
