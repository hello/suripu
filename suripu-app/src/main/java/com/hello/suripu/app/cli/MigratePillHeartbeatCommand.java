package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by kingshy on 10/20/15.
 */
public class MigratePillHeartbeatCommand extends ConfiguredCommand<SuripuAppConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(MigratePillHeartbeatCommand.class);

    private final static int DYNAMO_DB_BATCH_PUT_SIZE = 25;

    // TODO!!! NOTE !!! Remember to change these accordingly, or put in configs
    private String dataDir = "/tmp/data/"; // dir to store the 2 files below
    private String pillMapFilename = dataDir + "pill_map_2015_10_20_1650.csv"; // maps internal pill-id to external pill-id

    // raw csv data file with no headers
    // id,pill_id,firmware_version,battery_level,last_updated,uptime,fw_version
    private String pillDataFilename = dataDir + "test_data";

    private Float sleepMilliSeconds = 500.0f; // minimum millisecs to backoff when writes get throttled
    private Integer threadSize = 4; // no. of threads for writing to Dynamo
    private Float dynamoWriteThroughput = 2.0f; // dynamoDB write throughput
    private Integer rawBatchSize = 100; // number of rows to read from csv before sending to Dynamo
    // end of TODO!!!


    public MigratePillHeartbeatCommand() {
        super("migrate_pill_heartbeat", "Migrate pill_status from RDS to DynamoDB");
    }

    private class DynamoDBInsert implements Callable<Set<PillHeartBeat>> {
        private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;

        private final Set<PillHeartBeat> heartBeats;

        public DynamoDBInsert(final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB, final Set<PillHeartBeat> heartBeats) {
            this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
            this.heartBeats = ImmutableSet.copyOf(heartBeats);
        }

        @Override
        public Set<PillHeartBeat> call() {
            final Set<PillHeartBeat> unprocessedHeartbeats = this.pillHeartBeatDAODynamoDB.put(this.heartBeats);
            return unprocessedHeartbeats;
        }
    }

    @Override
    protected void run(final Bootstrap<SuripuAppConfiguration> bootstrap, final Namespace namespace, final SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        Thread.sleep(5000L);
        migrateData(configuration, awsCredentialsProvider);
    }


    private void migrateData(SuripuAppConfiguration configuration, AWSCredentialsProvider awsCredentialsProvider) {

        final Map<Long, String> pillMap = getPillIdMapping();
        if (pillMap.isEmpty()) {
            LOGGER.debug("Pill id mapping is empty!!");
            return;
        }

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getPillHeartBeatConfiguration();
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());
        final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB = PillHeartBeatDAODynamoDB.create(client, tableName);

        final ExecutorService service = Executors.newFixedThreadPool(this.threadSize);

        final File csvFile = new File(this.pillDataFilename);
        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));

            int countItems = 0;
            int savedItems = 0;
            int batchCount = 0;
            final Map<String, PillHeartBeat> uniqueHeartBeats= Maps.newHashMap();
            final Set<Future<Set<PillHeartBeat>>> results = Sets.newHashSet();

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                final String[] columns = line.split(",");
                final Long internalPillId = Long.valueOf(columns[1]);

                if (pillMap.containsKey(internalPillId)) {
                    countItems++;

                    final PillHeartBeat heartBeat = PillHeartBeat.create(
                            pillMap.get(internalPillId), // pill_id
                            Integer.valueOf(columns[3]), // battery_level
                            Integer.valueOf(columns[6]), // fw_version
                            Integer.valueOf(columns[5]), // uptime
                            DateTimeUtil.datetimeStringToDateTime(columns[4].substring(0, 18)) // utc_dt
                    );

                    // create mega-batch of data with unique Hash-Range key, overwrite with latest
                    uniqueHeartBeats.put(this.getPillHash(heartBeat), heartBeat);

                    // read N distinct rows, then send to Dynamo with multiple threads
                    int loopCount = 0;
                    if (uniqueHeartBeats.size() >= this.rawBatchSize) {
                        int toSaveSize = 0;
                        batchCount++;

                        while (true) {
                            loopCount++;
                            final Set<PillHeartBeat> toSaveHeartbeats = Sets.newHashSet(uniqueHeartBeats.values());
                            uniqueHeartBeats.clear();

                            // create batches of data to save to DynamoDB
                            final int numIterations = toSaveHeartbeats.size() / DYNAMO_DB_BATCH_PUT_SIZE + 1;
                            for (int i = 0; i < numIterations; i++) {
                                final Set<PillHeartBeat> toSaveSet = ImmutableSet.copyOf(Iterables.limit(toSaveHeartbeats, DYNAMO_DB_BATCH_PUT_SIZE));
                                toSaveHeartbeats.removeAll(toSaveSet);
                                toSaveSize += toSaveSet.size();

                                // blast DynamoDB with multi-threaded writes
                                final Future<Set<PillHeartBeat>> result = service.submit(new DynamoDBInsert(pillHeartBeatDAODynamoDB, toSaveSet));
                                results.add(result);
                            }

                            // get un-saved data, and re-add to batch
                            int totUnprocessed = 0;
                            for (Future<Set<PillHeartBeat>> unprocessedHeartbeats: results) {
                                final int unprocessedSize = unprocessedHeartbeats.get().size();
                                if (unprocessedSize > 0) {
                                    totUnprocessed += unprocessedSize;
                                    toSaveSize -= unprocessedSize;
                                    for (final PillHeartBeat unprocessedHeartbeat : unprocessedHeartbeats.get()) {
                                        uniqueHeartBeats.put(this.getPillHash(unprocessedHeartbeat), unprocessedHeartbeat);
                                    }
                                }
                            }

                            results.clear();

                            if (uniqueHeartBeats.size() < DYNAMO_DB_BATCH_PUT_SIZE) {
                                // un-saved size less than max Dynamo batch size, process unsaved in the next mega-batch
                                LOGGER.debug("Done with batch {}, loop count = {}", batchCount, loopCount);
                                savedItems += toSaveSize;
                                break;
                            } else {
                                // We got some un-saved data, sleep a  little to let DynamoDB breathe
                                final long sleepMilliseconds = (int) ((float) totUnprocessed / this.dynamoWriteThroughput * this.sleepMilliSeconds);
                                LOGGER.debug("Batch {} - Iteration {}: unprocessed count {}, count items {}, sleep {}, unique size {}",
                                        batchCount, loopCount, totUnprocessed, countItems, sleepMilliseconds, uniqueHeartBeats.size());
                                Thread.sleep(sleepMilliseconds);
                            }
                        }
                    }

                    if (countItems % 10000 == 0) {
                        LOGGER.debug("---- Saved {} items out of {} to Dynamo ----", savedItems, countItems);
                    }
                }

            }
            LOGGER.debug("==== DONE saved {} items out of {} ====", savedItems, countItems);

        } catch (Exception ex) {
            ex.printStackTrace();;
            throw new RuntimeException(ex);
        }

        service.shutdown();

    }

    private String getPillHash(final PillHeartBeat heartBeat) {
        return heartBeat.pillId + DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT).print(heartBeat.createdAtUTC);
    }

    private Map<Long, String> getPillIdMapping() {
        final Map<Long, String> pillMap = Maps.newHashMap();
        final File csvFile = new File(this.pillMapFilename);
        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));
            String line = bufferedReader.readLine(); // header

            while ((line = bufferedReader.readLine()) != null) {
                final String[] columns = line.split(",");
                final Long pillId = Long.valueOf(columns[0]);
                final String pillName = columns[2];
                pillMap.put(pillId, pillName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();;
            throw new RuntimeException(ex);
        }
        return pillMap;
    }
}