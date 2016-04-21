package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.opencsv.CSVReader;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

public class MovePillDataToDynamoDBCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovePillDataToDynamoDBCommand.class);

    private static final int NUM_BEFORE_FLUSH = 100;
    private static final int ITEMS_PER_BATCH = 25;
    private int backOffCounts = 0;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss:00Z");

    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_BEFORE_FLUSH / ITEMS_PER_BATCH);

    public enum Columns {
        ID(0),
        ACCOUNT_ID(1),
        TRACKER_ID(2),
        SVM_NO_GRAVITY(3),
        TS(4),
        OFFSET_MILLIS(5),
        LOCAL_UTC_TS(6),
        MOTION_RANGE(7),
        KICKOFF_COUNTS(8),
        ON_DURATION_SECONDS(9);

        public final int index;

        Columns(int index) {
            this.index = index;
        }
    }

    public enum PillMapColumns {
        ID(0),
        ACCOUNT_ID(1),
        EXTERNAL_PILL_ID(2);

        public final int index;

        PillMapColumns(int index) {
            this.index = index;
        }
    }

    public MovePillDataToDynamoDBCommand() {
        super("move_pill_data", "Imports pill data CSV dump into prod_pill_data_yyyy_mm tables");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("--mapping")
                .nargs("?")
                .required(true)
                .help("mapping csv for ids");

        subparser.addArgument("--task")
                .nargs("?")
                .required(true)
                .help("task to perform. migrate or test");

        // for migrate task
        subparser.addArgument("--csv")
                .nargs("?")
                .required(false)
                .help("csv files");

        subparser.addArgument("--dir")
                .nargs("?")
                .required(false)
                .help("data files directory");

        // for test task
        subparser.addArgument("--account")
                .nargs("?")
                .required(false)
                .help("account-id to test");

        subparser.addArgument("--start")
                .nargs("?")
                .required(false)
                .help("start utc timestamp");

        subparser.addArgument("--end")
                .nargs("?")
                .required(false)
                .help("end utc timestamp");

    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap,
                       Namespace namespace,
                       SuripuAppConfiguration suripuAppConfiguration) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final File mappingFile = new File(namespace.getString("mapping"));
        final Map<String, String> accountToExternalMapping = readIdMapping(mappingFile, PillMapColumns.EXTERNAL_PILL_ID);
        LOGGER.debug("Loaded {} id mapping entries", accountToExternalMapping.size());

        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(provider);
        final DynamoDBConfiguration pillDataConfiguration = suripuAppConfiguration.getPillDataConfiguration();
        dynamoDBClient.withEndpoint(pillDataConfiguration.getEndpoint());

        final PillDataDAODynamoDB pillDataDAODynamoDB = new PillDataDAODynamoDB(dynamoDBClient,
                pillDataConfiguration.getTableName());

        LOGGER.debug("DynamoDB client set up for {}/'{}'",
                pillDataConfiguration.getTableName(),
                pillDataConfiguration.getEndpoint());


        // choose which task to perform, migration or test results
        final String task = namespace.getString("task");
        switch (task) {
            case "migrate":
                runMigrate(namespace, suripuAppConfiguration, accountToExternalMapping, pillDataDAODynamoDB);
                break;
            case "test":
                break;
            default:
                LOGGER.error("WRONG task");
                break;
        }

        this.executor.shutdown();
    }

    private List<TrackerMotion> getUniqueList(final List<TrackerMotion> data) {
        final Set<TrackerMotion> samples = new TreeSet<>(new Comparator<TrackerMotion>() {
            @Override
            public int compare(TrackerMotion o1, TrackerMotion o2) {
                final long t1 = o1.timestamp;
                final long t2 = o2.timestamp;
                if (t1 < t2) {
                    return -1;
                }
                if (t1 > t2) {
                    return 1;
                }
                return 0;
            }
        });
        samples.addAll(data);
        final List<TrackerMotion> results = Lists.newArrayListWithCapacity(samples.size());
        results.addAll(samples);
        return results;
    }
    /**
     * Perform migration from csv data file to DynamoDB
     * @param namespace name
     * @param configuration config
     * @param idMapping account to external pill id mapping
     * @param pillDataDAODynamoDB dynamoDB client
     * @throws java.io.IOException
     * @throws InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    private void runMigrate(final Namespace namespace,
                            final SuripuAppConfiguration configuration,
                            final Map<String, String> idMapping,
                            final PillDataDAODynamoDB pillDataDAODynamoDB)
            throws IOException, InterruptedException, ExecutionException {
        int entriesWritten = 0;
        int entriesDiscarded = 0;
        int linesRead = 0;

        final Set<String> missingAccountMapping = Sets.newHashSet();

        final String directoryName = namespace.getString("dir");
        final String prefix = namespace.getString("csv") + "-";
        final List<File> csvFiles = this.getDataFiles(directoryName, prefix);

        final long nextFlushSleepMillis = configuration.getNextFlushSleepMillis();
        final int stopMonth = configuration.getStopMonth();

        for (final File csvFile : csvFiles) {
            try (final InputStream rawInput = new FileInputStream(csvFile);
                 final GZIPInputStream decodedInput = new GZIPInputStream(rawInput);
                 final CSVReader reader = new CSVReader(new InputStreamReader(decodedInput), ',')) {
                LOGGER.debug("Beginning migration for file {}", csvFile.getName());


                final List<TrackerMotion> accumulator = Lists.newArrayListWithCapacity(ITEMS_PER_BATCH);
                for (final String[] entry : reader) {
                    linesRead++;
                    final String accountId = getString(entry, Columns.ACCOUNT_ID);

                    if (missingAccountMapping.contains(accountId)) {
                        entriesDiscarded++;
                        continue;
                    }

                    final String externalPillId = idMapping.get(accountId);
                    if (externalPillId == null) {
                        LOGGER.error("No mapping for account id {}! Skipping row.", accountId);
                        missingAccountMapping.add(accountId);
                        entriesDiscarded++;
                        continue;
                    }

                    final TrackerMotion trackerMotion = new TrackerMotion.Builder()
                            .withAccountId(getLong(entry, Columns.ACCOUNT_ID))
                            .withTimestampMillis(getTimestampMillis(entry, Columns.TS))
                            .withExternalTrackerId(externalPillId)
                            .withOffsetMillis(getInteger(entry, Columns.OFFSET_MILLIS))
                            .withValue(getInteger(entry, Columns.SVM_NO_GRAVITY))
                            .withMotionRange(getLong(entry, Columns.MOTION_RANGE))
                            .withKickOffCounts(getLong(entry, Columns.KICKOFF_COUNTS))
                            .withOnDurationInSeconds(getLong(entry, Columns.ON_DURATION_SECONDS))
                            .build();

                    accumulator.add(trackerMotion);

                    if (accumulator.size() >= NUM_BEFORE_FLUSH) {
                        LOGGER.debug("Writing {} entries to DynamoDB", accumulator.size());
                        final List<TrackerMotion> remainingItems = writeData(pillDataDAODynamoDB, accumulator);

                        final int numberWritten = (accumulator.size() - remainingItems.size());
                        entriesWritten += numberWritten;
                        LOGGER.debug("Wrote {} entries to DynamoDB; {} written so far, read {} lines",
                                numberWritten, entriesWritten, linesRead);

                        accumulator.clear();
                        Thread.sleep(nextFlushSleepMillis); // let it breathe a little

                        if (!remainingItems.isEmpty()) {
                            LOGGER.debug("{} remaining", remainingItems.size());
                            accumulator.addAll(remainingItems);

                            // files are in local_utc, we may spillover, set to -1 to not check
                            final DateTime remainingFirstDateTime = new DateTime(accumulator.get(0).timestamp, DateTimeZone.UTC);
                            if (stopMonth > 0 && remainingFirstDateTime.getMonthOfYear() > stopMonth) {
                                LOGGER.debug("We're in month {}!", stopMonth + 1);
                                accumulator.clear();
                                break;
                            }

                            this.backOff(remainingItems.size());
                            this.backOffCounts++;
                        }
                    }
                }

                LOGGER.debug("Finished processing file, inserting remaining {}.", accumulator.size());

                int loop = 0;
                while (!accumulator.isEmpty()) {
                    loop++;
                    LOGGER.debug("remainder loop {}, size {}", loop, accumulator.size());
                    final int originalSize = accumulator.size();
                    final List<TrackerMotion> remainingItems = writeData(pillDataDAODynamoDB, accumulator);
                    accumulator.clear();
                    if (!remainingItems.isEmpty()) {
                        accumulator.addAll(remainingItems);

                        backOff(remainingItems.size());
                        this.backOffCounts++;
                        entriesWritten += originalSize - remainingItems.size();
                    }
                }

                LOGGER.debug("Migration completed, wrote {} from {} lines, discarded {}",
                        entriesWritten, linesRead, entriesDiscarded);
                LOGGER.debug("Number of back-offs {}.", this.backOffCounts);
            }


            LOGGER.debug("Missing {} account to pill mapping", missingAccountMapping.size());
            for (final String accountId : missingAccountMapping) {
                LOGGER.debug("No mapping for {}", accountId);
            }

            LOGGER.debug("Import done for file {}.", csvFile.getName());
            Thread.sleep(3000L); // pause a little between files

        }
        LOGGER.debug("ALL DONE!");
    }

    private List<File> getDataFiles(final String directoryName, final String prefix) {
        LOGGER.debug("Getting data files from directory {}, prefix {}", directoryName, prefix);
        final File directory = new File(directoryName);
        final File[] fileList = directory.listFiles();
        final List<File> resultList = Lists.newArrayList();
        if (fileList != null) {
            for (final File file : fileList) {
                if (file.getName().startsWith(prefix)) {
                    LOGGER.debug("Adding data file {}", file.getName());
                    resultList.add(file);
                }
            }
        }
        return resultList;
    }

    private static String getString(final String[] entry, final Columns column) {
        return entry[column.index];
    }

    private static long getLong(final String[] entry, final Columns column) {
        return Long.parseLong(entry[column.index], 10);
    }

    private static int getInteger(final String[] entry, final Columns column) {
        return Integer.parseInt(entry[column.index], 10);
    }

    private static Long getTimestampMillis(final String[] entry, final Columns column) {
        final String dtString = getString(entry, column) + ":00Z";
        final DateTime dt = DateTime.parse(dtString, DATE_TIME_FORMATTER).withZone(DateTimeZone.UTC);
        return dt.getMillis();
    }

    private Map<String, String> readIdMapping(final File mappingFile, final PillMapColumns senseColumn) throws IOException {
        final Map<String, String> mapping = Maps.newHashMap();
        try (final InputStream input = new FileInputStream(mappingFile);
             final CSVReader reader = new CSVReader(new InputStreamReader(input), ',')) {
            for (final String[] entry : reader) {
                mapping.put(entry[PillMapColumns.ACCOUNT_ID.index],
                        entry[senseColumn.index]);
            }
        }
        return mapping;
    }

    private void backOff(int remaining) throws InterruptedException {
        long howLong = (long) (((float) remaining / NUM_BEFORE_FLUSH) * 500.0f);
        final float percRemaining = remaining / ((float) ITEMS_PER_BATCH);
        if (percRemaining > 0.5f) {
            howLong += (long) ((percRemaining) / 0.2f) * 500L;
        }
        LOGGER.debug("Backing off for {} milliseconds", howLong);
        Thread.sleep(howLong);
    }

    private List<TrackerMotion> writeData(final PillDataDAODynamoDB pillDataDAODynamoDB,
                                       final List<TrackerMotion> items) throws ExecutionException, InterruptedException {

        final List<List<TrackerMotion>> batches = Lists.partition(items, ITEMS_PER_BATCH);

        final List<Future<List<TrackerMotion>>> futures = Lists.newArrayListWithCapacity(batches.size());
        for (final List<TrackerMotion> batch : batches) {
            final Future<List<TrackerMotion>> future = executor.submit(new Callable<List<TrackerMotion>>() {
                @Override
                public List<TrackerMotion> call() throws Exception {
                    LOGGER.debug("Submitting batch of {}", batch.size());
                    final List<TrackerMotion> remaining = pillDataDAODynamoDB.migrationBatchInsert(batch);
                    LOGGER.debug("Batch submitted, {} remaining", remaining.size());
                    return remaining;
                }
            });
            futures.add(future);
        }

        final List<TrackerMotion> remainingData = Lists.newArrayList();
        for (final Future<List<TrackerMotion>> future : futures) {
            if (!future.get().isEmpty()) {
                remainingData.addAll(future.get());
            }
        }
        return remainingData;
    }

}
