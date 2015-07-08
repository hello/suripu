package com.hello.suripu.workers.logs;

import com.flaptor.indextank.apiclient.IndexAlreadyExistsException;
import com.flaptor.indextank.apiclient.IndexDoesNotExistException;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.flaptor.indextank.apiclient.MaximumIndexesExceededException;
import com.flaptor.indextank.apiclient.UnexpectedCodeException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.configuration.BlackListDevicesConfiguration;
import com.hello.suripu.core.logging.SenseLogTag;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SenseLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLogIndexer.class);
    private final static Integer INDEX_CREATION_DELAY = 1000;
    private static final Integer REFRESH_PERIOD_MINUTES = 15;
    private static final Map<String, SenseLogTag> tagToField = ImmutableMap.<String, SenseLogTag>builder()
            .put("ALARM RINGING", SenseLogTag.ALARM_RINGING)
            .put("fault", SenseLogTag.FIRMWARE_CRASH)
            .put("travis", SenseLogTag.FIRMWARE_CRASH)
            .put("xkd", SenseLogTag.FIRMWARE_CRASH)
            .put("ALERT", SenseLogTag.FIRMWARE_CRASH)
            .put("SSID RSSI UNIQUE", SenseLogTag.WIFI_INFO)
            .put("dust", SenseLogTag.DUST_STATS)
            .build();

    private final IndexTankClient indexTankClient;
    private final String senseLogIndexPrefix;
    private final IndexTankClient.Index senseLogBackupIndex;
    private final JedisPool jedisPool;

    private final List<IndexTankClient.Document> documents;
    private final Map<String, IndexTankClient.Index> indexes;
    private IndexTankClient.Index index;
    private DateTime lastBlackListFetchDateTime;
    private Set<String> senseBlackList;
    private Integer blackListUpdateCount;

    public SenseLogIndexer(final IndexTankClient indexTankClient, final String senseLogIndexPrefix, final IndexTankClient.Index senseLogBackupIndex, final JedisPool jedisPool) {
        this.indexTankClient = indexTankClient;
        this.senseLogIndexPrefix = senseLogIndexPrefix;
        this.senseLogBackupIndex = senseLogBackupIndex;
        this.jedisPool = jedisPool;

        this.documents = Lists.newArrayList();
        this.indexes = Maps.newHashMap();
        this.index = senseLogBackupIndex;
        this.lastBlackListFetchDateTime = DateTime.now(DateTimeZone.UTC);
        this.senseBlackList = Sets.newHashSet();
        this.blackListUpdateCount = 0;
    }


    public BatchLog chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = Lists.newArrayList();
        String createdDateString = new DateTime(DateTimeZone.UTC).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            if (getSenseBlackList().contains(log.getDeviceId())) {
                LOGGER.info("Log from blacklisted senseId {}, will not index", log.getDeviceId());
                continue;
            }

            final Long millis = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            final String documentId = String.format("%s-%d", log.getDeviceId(), millis);
            final DateTime createdDateTime = new DateTime(millis, DateTimeZone.UTC);
            final String halfDateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyyMMdda"));
            final String dateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyyMMdd"));

            final Map<String, String> fields = Maps.newHashMap();
            final Map<String, String> categories = Maps.newHashMap();

            fields.put("device_id", log.getDeviceId());
            fields.put("text", log.getMessage());
            fields.put("ts", String.valueOf(log.getTs()));
            fields.put("half_date", halfDateString);
            fields.put("date", dateString);
            fields.put("all", "1");

            for (final String tag : tagToField.keySet()) {
                fields.put(tagToField.get(tag).value, String.valueOf(log.getMessage().contains(tag)));
            }

            createdDateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd"));

            final Map<Integer, Float> variables = new HashMap<>();
            variables.put(0, new Float(millis / 1000));
            variables.put(1, new Float(millis));

            categories.put("device_id", log.getDeviceId());
            categories.put("origin", log.getOrigin());
            categories.put("half_date", halfDateString);
            categories.put("date", dateString);

            documents.add(new IndexTankClient.Document(documentId, fields, variables, categories));

        }
        return new BatchLog(documents, createdDateString);
    }

    @Override
    public Integer index() {
        try {
            if (!documents.isEmpty()) {
                index.addDocuments(ImmutableList.copyOf(documents));
                final Integer count = documents.size();
                LOGGER.info("Indexed {} documents", count);
                documents.clear();
                return count;
            }
        } catch (IndexDoesNotExistException e) {
            LOGGER.error("Index does not exist: {}", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            LOGGER.error("Failed connecting to searchify: {}", e.getMessage());
        } catch(IndexOutOfBoundsException e) {
            LOGGER.error("Searchify client error: {}", e.getMessage());
        } catch (UnexpectedCodeException e) {
            LOGGER.error("Unexpected: {}", e.getMessage());
        }


        return 0;
    }

    @Override
    public void collect(final LoggingProtos.BatchLogMessage batchLogMessage) {
        final BatchLog batchLog = chunkBatchLogMessage(batchLogMessage);
        documents.addAll(batchLog.documents);

        if (!indexes.containsKey(batchLog.createdDateString)){
            IndexTankClient.Index newIndex = senseLogBackupIndex;
            final String indexName = senseLogIndexPrefix + batchLog.createdDateString;
            try {
                newIndex = indexTankClient.createIndex(indexName);
                Integer waitSeconds = 0;
                while (!isIndexReady(newIndex)) {
                    waitSeconds +=  INDEX_CREATION_DELAY/1000;
                    LOGGER.warn("Index is not ready, has been waiting for {} seconds", waitSeconds);
                    if (waitSeconds >= 121) {
                        LOGGER.error("Stop waiting on index creation. Opting out");
                        System.exit(1);
                    }
                    try {
                        Thread.sleep(INDEX_CREATION_DELAY);
                    }
                    catch (InterruptedException e) {
                        LOGGER.error("interrupted");
                    }
                }
                LOGGER.info("Index {} is ready to serve!", indexName);
            }
            catch (IndexAlreadyExistsException indexAlreadyExistsException) {
                LOGGER.info("Index {} already existed", indexName);
                newIndex = indexTankClient.getIndex(indexName);
            }
            catch (MaximumIndexesExceededException e) {
                LOGGER.error("Failed to create new index {} because {} ", indexName, e.getMessage());
            }
            catch (IOException e) {
                LOGGER.error("Failed to create new index {} because {} ", indexName, e.getMessage());
            }
            catch (UnexpectedCodeException e) {
                LOGGER.error("Failed to create new index {} because {}", indexName, e.getMessage());
            }
            indexes.put(batchLog.createdDateString, newIndex);
        }
        index = indexes.get(batchLog.createdDateString);
    }

    private static class BatchLog {
        public final List<IndexTankClient.Document> documents;
        public final String createdDateString;
        public BatchLog(final List<IndexTankClient.Document> documents, final String createdDateString) {
            this.documents = documents;
            this.createdDateString = createdDateString;
        }
    }

    private Boolean isIndexReady(final IndexTankClient.Index index) {
        try {
            index.refreshMetadata();
            if (index.getMetadata().get("started") == null) {
                return Boolean.FALSE;
            }
            return (Boolean)index.getMetadata().get("started");
        }
        catch (IndexDoesNotExistException e) {
            LOGGER.error("Error when check index readiness {}", e.getMessage());
            return Boolean.FALSE;
        }
        catch (IOException e) {
            LOGGER.error("Error when check index readiness {}", e.getMessage());
            return Boolean.FALSE;
        }
        catch (UnexpectedCodeException e) {
            LOGGER.error("Error when check index readiness {}", e.getMessage());
            return Boolean.FALSE;
        }
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
