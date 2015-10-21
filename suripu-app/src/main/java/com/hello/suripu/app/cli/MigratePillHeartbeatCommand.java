package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.ImmutableSet;
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
import org.joda.time.DateTime;
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

    private final static String dataDir = "/Users/kingshy/DEV/Hello/Data/"; // "/home/ubuntu/misc/pill_status/"
    private final static String pillMapFilename = dataDir + "pill_map_2015_10_20_1650.csv";
    private final static String pillDataFilename = dataDir + "ps_data_aa";


    public MigratePillHeartbeatCommand() {
        super("migrate_pill_heartbeat", "Migrate pill_status from RDS to DynamoDB");
    }

    private class DynamoDBInsert implements Callable<Boolean> {
        private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;

        private final Set<PillHeartBeat> heartBeats;

        public DynamoDBInsert(final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB, final Set<PillHeartBeat> heartBeats) {
            this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
            this.heartBeats = ImmutableSet.copyOf(heartBeats);
        }

        @Override
        public Boolean call() {
            this.pillHeartBeatDAODynamoDB.put(this.heartBeats);
            return true;
        }
    }

//    @Override
//    public void configure(Subparser subparser) {
//        super.configure(subparser);
//        subparser.addArgument();
//    }
    @Override
    protected void run(final Bootstrap<SuripuAppConfiguration> bootstrap, final Namespace namespace, final SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        Thread.sleep(20*1000L);
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

        ExecutorService service = Executors.newFixedThreadPool(5);

        final File csvFile = new File(pillDataFilename);
        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));
            String line = bufferedReader.readLine(); // header

            int countItems = 0;
            int savedItems = 0;
            final Map<String, PillHeartBeat> batchHeartBeats = Maps.newHashMap();
            final Set<Future<Boolean>> results = Sets.newHashSet();

            while ((line = bufferedReader.readLine()) != null) {
                final String[] columns = line.split(",");
                final Long internalPillId = Long.valueOf(columns[1]);
                if (pillMap.containsKey(internalPillId)) {
                    countItems++;
                    final String pillId = pillMap.get(internalPillId);
                    final int batteryLevel = Integer.valueOf(columns[3]);
                    final DateTime createdAtUTC = DateTimeUtil.datetimeStringToDateTime(columns[4]);
                    final int uptime = Integer.valueOf(columns[5]);
                    final int fwVersion = Integer.valueOf(columns[6]);

                    final PillHeartBeat heartBeat = PillHeartBeat.create(pillId, batteryLevel, fwVersion, uptime, createdAtUTC);
                    final String hashRangeKey = pillId + ' ' + columns[4];
                    batchHeartBeats.put(hashRangeKey, heartBeat); // overwrite with latest heart beat

                    // do batch puts of 25 items each time
                    if (batchHeartBeats.size() > 24) {
                        // save to Dynamo
                        savedItems += batchHeartBeats.size();
                        final Set<PillHeartBeat> toSaveSet = ImmutableSet.copyOf(batchHeartBeats.values());
                        final Future<Boolean> res = service.submit(new DynamoDBInsert(pillHeartBeatDAODynamoDB, toSaveSet));
                        results.add(res);
                        // pillHeartBeatDAODynamoDB.put(toSaveSet);

                        batchHeartBeats.clear();
                    }

                    if (savedItems % 100 == 0) {
                        LOGGER.debug("Saved {} items out of {} to Dynamo", savedItems, countItems);
                    }
                }

            }
            for (Future<Boolean> fb : results) {
                LOGGER.debug("results: {}", fb.get());
            }
        } catch (Exception ex) {
            ex.printStackTrace();;
            throw new RuntimeException(ex);
        }

        service.shutdown();

    }

    private Map<Long, String> getPillIdMapping() {
        final Map<Long, String> pillMap = Maps.newHashMap();
        final File csvFile = new File(pillMapFilename);
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