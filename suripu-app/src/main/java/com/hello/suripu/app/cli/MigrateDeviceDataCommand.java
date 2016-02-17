package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.opencsv.CSVReader;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

public class MigrateDeviceDataCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateDeviceDataCommand.class);

    private static final int NUM_BEFORE_FLUSH = 100;
    private static final int ITEMS_PER_BATCH = 25;
    private int backOffCounts = 0;

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

        final File mappingFile = new File(namespace.getString("mapping"));
        final Map<String, String> accountToExternalMapping = readIdMapping(mappingFile, DeviceMapColumns.EXTERNAL_SENSE_ID);
        LOGGER.debug("Loaded {} id mapping entries", accountToExternalMapping.size());

        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(provider);
        final DynamoDBConfiguration deviceDataConfiguration = suripuAppConfiguration.getDeviceDataConfiguration();
        dynamoDBClient.withEndpoint(deviceDataConfiguration.getEndpoint());

        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(dynamoDBClient,
                deviceDataConfiguration.getTableName());

        LOGGER.debug("DynamoDB client set up for {}/'{}'",
                deviceDataConfiguration.getTableName(),
                deviceDataConfiguration.getEndpoint());


        // choose which task to perform, migration or test results
        final String task = namespace.getString("task");
        if (task.equals("migrate")) {
            runMigrate(namespace, suripuAppConfiguration, accountToExternalMapping, deviceDataDAODynamoDB);
        } else if (task.equals("test")) {

            final Map<String, String> accountToInternalMapping = readIdMapping(mappingFile, DeviceMapColumns.ID);
            testResults(namespace, suripuAppConfiguration, deviceDataDAODynamoDB, accountToExternalMapping, accountToInternalMapping);
        } else {
            LOGGER.error("WRONG task");
        }

        this.executor.shutdown();
    }

    /**
     * Compare DDB vs Postgres results
     * @param namespace
     * @param configuration
     * @param deviceDataDAODynamoDB
     */
    private void testResults(final Namespace namespace,
                             final SuripuAppConfiguration configuration,
                             final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                             final Map<String, String> accountToExternalMapping,
                             final Map<String, String> accountToInternalMapping) throws Exception
    {
        // set up postgres DAO

        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource = managedDataSourceFactory.build(configuration.getSensorsDB());

        final DBI jdbi = new DBI(dataSource);
        jdbi.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorsDB().getDriverClass()));
        jdbi.registerContainerFactory(new ImmutableListContainerFactory());
        jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final DeviceDataDAO deviceDataDAO = jdbi.onDemand(DeviceDataDAO.class);

        final String accountIdString = namespace.getString("account");
        if (!accountToExternalMapping.containsKey(accountIdString)) {
            LOGGER.error("No sense mapping found for account id {}", accountIdString);
            return;
        }

        final String externalSenseId = accountToExternalMapping.get(accountIdString);
        final Long internalSenseId = Long.valueOf(accountToInternalMapping.get(accountIdString));
        final Long accountId = Long.valueOf(accountIdString);

        final Long startTimestamp = DateTimeUtil.datetimeStringToDateTime(namespace.getString("start")).getMillis();
        final Long endTimestamp = DateTimeUtil.datetimeStringToDateTime(namespace.getString("end")).withMillisOfSecond(0).getMillis();

        final Optional<Calibration> optionalCalibration = Optional.absent();
        final Optional<Device.Color> optionalColor = Optional.absent();

        final int slotMinutes = 1;
        final int defaultMissingValue = 0;

        LOGGER.debug("Getting data from DynamoDB");
        final AllSensorSampleList samplesDDB = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                startTimestamp, endTimestamp,
                accountId, externalSenseId,
                slotMinutes, defaultMissingValue,
                optionalColor, optionalCalibration, false);

        LOGGER.debug("Getting data from Postgres");
        final AllSensorSampleList samplesRDS = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                startTimestamp, endTimestamp,
                accountId, internalSenseId,
                slotMinutes, defaultMissingValue,
                optionalColor, optionalCalibration, false);

        // make sure that what's in RDS is also present in DDB
        final List<Sensor> sensors = samplesRDS.getAvailableSensors();
        int sensorErrors = 0;
        int sizeErrors = 0;
        int valueErrors = 0;
        int totalSamples = 0;

        for (final Sensor sensor : sensors) {
            if (samplesDDB.get(sensor).isEmpty()) {
                LOGGER.error("sensor {} is not in DDB sample list", sensor.toString());
                sensorErrors++;
                continue;
            }

            // let's check the values
            final List<Sample> valuesDDB = samplesDDB.get(sensor);
            final List<Sample> valuesRDS = samplesRDS.get(sensor);
            if (valuesDDB.size() != valuesRDS.size()) {
                LOGGER.error("List size for sensor {} are not equal, DDB {}, RDS {}",
                        sensor.toString(), valuesDDB.size(), valuesRDS.size());
                sizeErrors++;
                continue;
            }

            LOGGER.debug("Checking sensor values for {}", sensor.toString());
            for (int i=0; i< valuesRDS.size(); i++) {
                totalSamples++;
                if (!valuesDDB.get(i).equals(valuesRDS.get(i))){
                    LOGGER.error("Wrong values for timestamp {}:DDB {}, RDS {}",
                            new DateTime(valuesDDB.get(i).dateTime, DateTimeZone.UTC).toString(),
                            valuesDDB.get(i).toString(),
                            valuesRDS.get(i).toString());
                    valueErrors++;
                }
            }
        }

        System.out.println("Check Results");
        System.out.println(String.format("Total Samples: %d", totalSamples));
        System.out.println(String.format("Sensor Errors: %d", sensorErrors));
        System.out.println(String.format("Size Errors: %d", sizeErrors));
        System.out.println(String.format("Value Errors: %d", valueErrors));
    }

    /**
     * Perform migration from csv data file to DynamoDB
     * @param namespace
     * @param configuration
     * @param idMapping
     * @param deviceDataDAO
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void runMigrate(final Namespace namespace,
                            final SuripuAppConfiguration configuration,
                            final Map<String, String> idMapping,
                            final DeviceDataDAODynamoDB deviceDataDAO)
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


                final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
                final List<DeviceData> accumulator = Lists.newArrayListWithCapacity(ITEMS_PER_BATCH);
                for (final String[] entry : reader) {
                    linesRead++;
                    final String accountId = getDeviceDataString(entry, Columns.ACCOUNT_ID);

                    if (missingAccountMapping.contains(accountId)) {
                        entriesDiscarded++;
                        continue;
                    }

                    final String externalSenseId = idMapping.get(accountId);
                    if (externalSenseId == null) {
                        LOGGER.error("No mapping for account id {}! Skipping row.", accountId);
                        missingAccountMapping.add(accountId);
                        entriesDiscarded++;
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
                            .withAlreadyCalibratedAudioPeakBackgroundDB(getDeviceDataInteger(entry, Columns.AUDIO_PEAK_BACKGROUND_DB))
                            .withAlreadyCalibratedAudioPeakDisturbancesDB(getDeviceDataInteger(entry, Columns.AUDIO_PEAK_DISTURBANCES_DB))
                            .withAudioNumDisturbances(getDeviceDataInteger(entry, Columns.AUDIO_NUM_DISTURBANCES))
                            .withOffsetMillis(getDeviceDataInteger(entry, Columns.OFFSET_MILLIS))
                            .withDateTimeUTC(dateTimeFormatter.parseDateTime(getDeviceDataString(entry, Columns.TS)))
                            .withWaveCount(getDeviceDataInteger(entry, Columns.WAVE_COUNT))
                            .withHoldCount(getDeviceDataInteger(entry, Columns.HOLD_COUNT))
                            .build();

                    accumulator.add(deviceData);

                    if (accumulator.size() >= NUM_BEFORE_FLUSH) {
                        LOGGER.debug("Writing {} entries to DynamoDB", accumulator.size());
                        final List<DeviceData> remainingItems = writeData(deviceDataDAO, accumulator);

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
                            if (stopMonth > 0 && accumulator.get(0).dateTimeUTC.getMonthOfYear() > stopMonth) {
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
                    final List<DeviceData> remainingItems = writeData(deviceDataDAO, accumulator);
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


            LOGGER.debug("Missing {} account to device mapping", missingAccountMapping.size());
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

    private static String getDeviceDataString(final String[] entry, final Columns column) {
        return entry[column.index];
    }

    private static long getDeviceDataLong(final String[] entry, final Columns column) {
        return Long.parseLong(entry[column.index], 10);
    }

    private static int getDeviceDataInteger(final String[] entry, final Columns column) {
        return Integer.parseInt(entry[column.index], 10);
    }

    private Map<String, String> readIdMapping(final File mappingFile, final DeviceMapColumns senseColumn) throws IOException {
        final Map<String, String> mapping = Maps.newHashMap();
        try (final InputStream input = new FileInputStream(mappingFile);
             final CSVReader reader = new CSVReader(new InputStreamReader(input), ',')) {
            for (final String[] entry : reader) {
                mapping.put(entry[DeviceMapColumns.ACCOUNT_ID.index],
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

    private List<DeviceData> writeData(final DeviceDataDAODynamoDB deviceDataDAO,
                                       final List<DeviceData> items) throws ExecutionException, InterruptedException {

        final List<List<DeviceData>> batches = Lists.partition(items, ITEMS_PER_BATCH);

        final List<Future<List<DeviceData>>> futures = Lists.newArrayListWithCapacity(batches.size());
        for (final List<DeviceData> batch : batches) {
            final Future<List<DeviceData>> future = executor.submit(new Callable<List<DeviceData>>() {
                @Override
                public List<DeviceData> call() throws Exception {
                    LOGGER.debug("Submitting batch of {}", batch.size());
                    final List<DeviceData> remaining = deviceDataDAO.batchInsertReturnsRemaining(batch);
                    LOGGER.debug("Batch submitted, {} remaining", remaining.size());
                    return remaining;
                }
            });
            futures.add(future);
        }

        final List<DeviceData> remainingData = Lists.newArrayList();
        for (final Future<List<DeviceData>> future : futures) {
            if (!future.get().isEmpty()) {
                remainingData.addAll(future.get());
            }
        }
        return remainingData;
    }

}
