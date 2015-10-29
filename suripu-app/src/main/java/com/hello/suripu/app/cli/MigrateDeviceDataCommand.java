package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.opencsv.CSVReader;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

public class MigrateDeviceDataCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateDeviceDataCommand.class);

    private static final int NUM_BEFORE_FLUSH = 200;
    private static final int ITEMS_PER_BATCH = 25;

    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_BEFORE_FLUSH / ITEMS_PER_BATCH);

    public enum Columns {
        ID(0),
        ACCOUNT_ID(1),
        DEVICE_ID(2),
        AMBIENT_TEMP(3),
        AMBIENT_LIGHT(4),
        AMBIENT_HUMIDITY(5),
        AMBIENT_AIR_QUALITY(6),
        TS(7),
        LOCAL_UTC_TS(8),
        OFFSET_MILLIS(9),
        AMBIENT_LIGHT_VARIANCE(10),
        AMBIENT_LIGHT_PEAKINESS(11),
        AMBIENT_AIR_QUALITY_RAW(12),
        AMBIENT_DUST_VARIANCE(13),
        AMBIENT_DUST_MIN(14),
        AMBIENT_DUST_MAX(15),
        FIRMWARE_VERSION(16),
        WAVE_COUNT(17),
        HOLD_COUNT(18),
        AUDIO_NUM_DISTURBANCES(19),
        AUDIO_PEAK_DISTURBANCES_DB(20),
        AUDIO_PEAK_BACKGROUND_DB(21);

        public final int index;

        Columns(int index) {
            this.index = index;
        }
    }

    public enum DeviceMapColumns {
        ID(0),
        ACCOUNT_ID(1),
        EXTERNAL_SENSE_ID(2);

        public final int index;

        DeviceMapColumns(int index) {
            this.index = index;
        }
    }

    public MigrateDeviceDataCommand() {
        super("migrate_device_data", "Imports CSV data dumps into device data dynamo tables");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("--mapping")
                .nargs("?")
                .required(true)
                .help("mapping csv for ids");

        subparser.addArgument("--csv")
                .nargs("?")
                .required(true)
                .help("csv files");
    }

    private int writeData(final DeviceDataDAODynamoDB deviceDataDAO,
                             final List<DeviceData> items) throws ExecutionException, InterruptedException {

        final List<List<DeviceData>> batches = Lists.partition(items, ITEMS_PER_BATCH);
        final List<Future<Integer>> futures = Lists.newArrayListWithCapacity(batches.size());
        for (final List<DeviceData> batch : batches) {
            final Future<Integer> future = executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    LOGGER.debug("Submitting batch of {}", batch.size());
                    final int remaining = deviceDataDAO.batchInsert(batch);
                    LOGGER.debug("Batch submitted, {} remaining", remaining);
                    return remaining;
                }
            });
            futures.add(future);
        }

        int remainingItems = 0;
        for (final Future<Integer> future : futures) {
            remainingItems += future.get();
        }
        return remainingItems;
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap,
                       Namespace namespace,
                       SuripuAppConfiguration suripuAppConfiguration) throws Exception {
        final File mappingFile = new File(namespace.getString("mapping"));
        final Map<String, String> idMapping = readIdMapping(mappingFile);
        LOGGER.debug("Loaded {} id mapping entries", idMapping.size());

        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(provider);
        final DynamoDBConfiguration deviceDataConfiguration = suripuAppConfiguration.getDeviceDataConfiguration();
        dynamoDBClient.withEndpoint(deviceDataConfiguration.getEndpoint());
        LOGGER.debug("DynamoDB client set up for {}/'{}'",
                deviceDataConfiguration.getTableName(),
                deviceDataConfiguration.getEndpoint());

        final File csvFile = new File(namespace.getString("csv"));
        try (final InputStream rawInput = new FileInputStream(csvFile);
             final GZIPInputStream decodedInput = new GZIPInputStream(rawInput);
             final CSVReader reader = new CSVReader(new InputStreamReader(decodedInput), ',')) {
            LOGGER.debug("Beginning migration");

            final DeviceDataDAODynamoDB deviceDataDAO = new DeviceDataDAODynamoDB(dynamoDBClient,
                    deviceDataConfiguration.getTableName());

            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            final List<DeviceData> accumulator = Lists.newArrayListWithCapacity(25);
            int entriesWritten = 0;
            for (final String[] entry : reader) {
                final String accountId = getDeviceDataString(entry, Columns.ACCOUNT_ID);
                final String externalSenseId = idMapping.get(accountId);
                if (externalSenseId == null) {
                    LOGGER.error("No mapping for account id {}! Skipping row.", accountId);
                    continue;
                }

                final DeviceData deviceData = new DeviceData.Builder()
                        .withAccountId(getDeviceDataLong(entry, Columns.ACCOUNT_ID))
                        .withExternalDeviceId(externalSenseId)
                        .withAmbientTemperature(getDeviceDataInteger(entry, Columns.AMBIENT_TEMP))
                        .withAmbientLight(getDeviceDataInteger(entry, Columns.AMBIENT_LIGHT))
                        .withAmbientLightVariance(getDeviceDataInteger(entry, Columns.AMBIENT_LIGHT_VARIANCE))
                        .withAmbientHumidity(getDeviceDataInteger(entry, Columns.AMBIENT_HUMIDITY))
                        .withAmbientAirQualityRaw(getDeviceDataInteger(entry, Columns.AMBIENT_AIR_QUALITY_RAW))
                        .withAudioPeakBackgroundDB(getDeviceDataInteger(entry, Columns.AUDIO_PEAK_BACKGROUND_DB))
                        .withAudioPeakDisturbancesDB(getDeviceDataInteger(entry, Columns.AUDIO_PEAK_DISTURBANCES_DB))
                        .withAudioNumDisturbances(getDeviceDataInteger(entry, Columns.AUDIO_NUM_DISTURBANCES))
                        .withOffsetMillis(getDeviceDataInteger(entry, Columns.OFFSET_MILLIS))
                        .withDateTimeUTC(dateTimeFormatter.parseDateTime(getDeviceDataString(entry, Columns.TS)))
                        .withWaveCount(getDeviceDataInteger(entry, Columns.WAVE_COUNT))
                        .withHoldCount(getDeviceDataInteger(entry, Columns.HOLD_COUNT))
                        .build();

                accumulator.add(deviceData);

                if (accumulator.size() == NUM_BEFORE_FLUSH) {
                    LOGGER.debug("Writing {} entries to DynamoDB", accumulator.size());
                    final int numberWritten = writeData(deviceDataDAO, accumulator);
                    entriesWritten += numberWritten;
                    LOGGER.debug("Wrote {} entries to DynamoDB; {} written so far",
                            numberWritten, entriesWritten);
                    accumulator.clear();
                }
            }

            if (!accumulator.isEmpty()) {
                LOGGER.debug("Writing {} entries to DynamoDB", accumulator.size());
                final int numberWritten = writeData(deviceDataDAO, accumulator);
                entriesWritten += numberWritten;
                LOGGER.debug("Wrote {} entries to DynamoDB; {} written",
                        numberWritten, entriesWritten);
            }

            LOGGER.debug("Migration completed");
        }
    }

    private static String getDeviceDataString(final String[] entry, final Columns column) {
        return entry[column.index];
    }

    private static long getDeviceDataLong(final String[] entry, final Columns column) {
        return Long.parseLong(entry[column.index], 10);
    }

    private static int getDeviceDataInteger(final String[] entry, final Columns column) {
        return Integer.parseInt(entry[column.index], 10);
    }

    private Map<String, String> readIdMapping(final File mappingFile) throws IOException {
        final Map<String, String> mapping = Maps.newHashMap();
        try (final InputStream input = new FileInputStream(mappingFile);
             final CSVReader reader = new CSVReader(new InputStreamReader(input), ',')) {
            for (final String[] entry : reader) {
                mapping.put(entry[DeviceMapColumns.ACCOUNT_ID.index],
                        entry[DeviceMapColumns.EXTERNAL_SENSE_ID.index]);
            }
        }
        return mapping;
    }
}
