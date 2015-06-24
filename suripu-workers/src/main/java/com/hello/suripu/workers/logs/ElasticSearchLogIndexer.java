package com.hello.suripu.workers.logs;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.configuration.BlackListDevicesConfiguration;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchBulkSettings;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchTransportClient;
import com.hello.suripu.core.models.SenseLogDocument;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ElasticSearchLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchLogIndexer.class);

    private final static String INDEX_NAME = "mirage"; // probably will be determined by current datetime

    private static final Integer REFRESH_PERIOD_MINUTES = 15;

    private final JedisPool jedisPool;
    private final ElasticSearchTransportClient elasticSearchTransportClient;
    private final ElasticSearchBulkSettings elasticSearchBulkSettings;

    private final List<SenseLogDocument> documents;
    private DateTime lastBlackListFetchDateTime;
    private Set<String> senseBlackList;
    private Integer blackListUpdateCount;
    private Stopwatch stopwatch;

    public ElasticSearchLogIndexer(final JedisPool jedisPool, final ElasticSearchTransportClient elasticSearchTransportClient, ElasticSearchBulkSettings elasticSearchBulkSettings) {
        this.jedisPool = jedisPool;
        this.elasticSearchTransportClient = elasticSearchTransportClient;
        this.elasticSearchBulkSettings = elasticSearchBulkSettings;

        this.documents = Lists.newArrayList();
        this.lastBlackListFetchDateTime = DateTime.now(DateTimeZone.UTC);
        this.senseBlackList = Sets.newHashSet();
        this.blackListUpdateCount = 0;
        this.stopwatch = new Stopwatch().start();
    }


    public List<SenseLogDocument> chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<SenseLogDocument> documents = Lists.newArrayList();

        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            if (getSenseBlackList().contains(log.getDeviceId())) {
                LOGGER.info("Received log from blacklisted senseId {}, will not index", log.getDeviceId());
                continue;
            }

            final Long timestamp = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            documents.add(new SenseLogDocument(log.getDeviceId(), timestamp, log.getMessage(), log.getOrigin()));

        }
        return documents;
    }

    @Override
    public Integer index() {
        final Integer documentsSize = documents.size();
        if (documentsSize == 0) {
            LOGGER.warn("EMPTY DOCUMENTS");
            return 0;
        }

        BulkProcessor bulkProcessor = null;

        final TransportClient transportClient = elasticSearchTransportClient.generateClient();

//        To be extra cautious - check if index really exists
//        final IndicesExistsResponse indicesExistsResponse = client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet();
//        if (indicesExistsResponse.isExists()) {
//            LOGGER.error("Index {} does not exist, please do something about it", INDEX_NAME);
//        }

        try {
            bulkProcessor = BulkProcessor.builder(
                transportClient,
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        stopwatch.reset().start();
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {
                        for (final BulkItemResponse bulkItemResponse : response.getItems()) {
                            LOGGER.debug("Successfully {} {}/{}/{}", bulkItemResponse.getOpType(), bulkItemResponse.getIndex(), bulkItemResponse.getType(), bulkItemResponse.getId());
                        }
                        LOGGER.info("After bulking: Successfully indexed {} documents into index {}", response.getItems().length, INDEX_NAME);
                        LOGGER.info("Elapsed indexing time is {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) {
                        LOGGER.error(failure.getMessage());
                    }
                })
                .setBulkActions(elasticSearchBulkSettings.bulkActions)
                .setBulkSize(new ByteSizeValue(elasticSearchBulkSettings.bulkSizeInMegabyes, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(elasticSearchBulkSettings.flushIntervalInSeconds))
                .setConcurrentRequests(elasticSearchBulkSettings.concurrentRequests)
                .build();

            for (final SenseLogDocument senseLogDocument : documents) {
                bulkProcessor.add(new IndexRequest(INDEX_NAME, String.valueOf(new Random().nextInt(10))).source(senseLogDocument.toMap()));
            }
            LOGGER.info("Adding {} documents to bulk processor", documentsSize);
        }
        catch (Exception e) {
            LOGGER.error("Bulk processing failed because {}", e.getMessage());
            }
        finally {
            documents.clear();
            bulkProcessor.close();
            return documentsSize;

        }
    }

    @Override
    public void collect(final LoggingProtos.BatchLogMessage batchLogMessage) {
        LOGGER.debug("bye searchify, hello es");
        documents.addAll(chunkBatchLogMessage(batchLogMessage));


    }

    private Set<String> getSenseBlackList() {
        if (lastBlackListFetchDateTime.plusMinutes(REFRESH_PERIOD_MINUTES).isBeforeNow() || blackListUpdateCount == 0){
            lastBlackListFetchDateTime = DateTime.now(DateTimeZone.UTC);
            Jedis jedis = null;
            String exceptionMessage = "";
            try {
                jedis = jedisPool.getResource();
                senseBlackList = jedis.smembers(BlackListDevicesConfiguration.SENSE_BLACK_LIST_KEY);
                LOGGER.info("Refreshed sense black list");
            } catch (JedisDataException e) {
                exceptionMessage = String.format("Failed to get data from redis -  %s", e.getMessage());
                LOGGER.error(exceptionMessage);
            } catch (Exception e) {
                exceptionMessage = String.format("Failed to refresh sense black list because %s", e.getMessage());
                LOGGER.error(exceptionMessage);
            } finally {
                if (jedis != null) {
                    try {
                        if (exceptionMessage.isEmpty()) {
                            jedisPool.returnResource(jedis);
                        } else {
                            jedisPool.returnBrokenResource(jedis);
                        }
                    } catch (JedisException e) {
                        LOGGER.error("Failed to return to resource {}", e.getMessage());
                    }

                }
            }
        }
        blackListUpdateCount += 1;
        return senseBlackList;
    }



}
