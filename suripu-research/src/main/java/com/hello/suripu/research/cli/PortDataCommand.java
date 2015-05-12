package com.hello.suripu.research.cli;

import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.research.configuration.SuripuResearchConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by pangwu on 4/24/15.
 */
public class PortDataCommand extends ConfiguredCommand<SuripuResearchConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(PortDataCommand.class);

    public PortDataCommand(){
        super("port", "Get port data to csv.");
    }

    @Override
    protected void run(final Bootstrap<SuripuResearchConfiguration> bootstrap, final Namespace namespace, final SuripuResearchConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource commonDataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI commonJDBI = new DBI(commonDataSource);
        commonJDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        commonJDBI.registerContainerFactory(new ImmutableListContainerFactory());
        commonJDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        commonJDBI.registerContainerFactory(new OptionalContainerFactory());
        commonJDBI.registerArgumentFactory(new JodaArgumentFactory());

        final AccountDAOImpl accountDAO = commonJDBI.onDemand(AccountDAOImpl.class);

        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorsDB());

        final DBI sensorJDBI = new DBI(sensorDataSource);
        sensorJDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorsDB().getDriverClass()));
        sensorJDBI.registerContainerFactory(new ImmutableListContainerFactory());
        sensorJDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        sensorJDBI.registerContainerFactory(new OptionalContainerFactory());
        sensorJDBI.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = sensorJDBI.onDemand(TrackerMotionDAO.class);
        final DeviceDataDAO deviceDataDAO = sensorJDBI.onDemand(DeviceDataDAO.class);

        //printInvalidNights(accountDAO, trackerMotionDAO);
        final String accountFilePath = namespace.getString("accounts");
        LOGGER.info("account file path {}", accountFilePath);

        portData(accountFilePath, trackerMotionDAO, deviceDataDAO);

    }

    private void portData(final String accountFilePath, final TrackerMotionDAO trackerMotionDAO, final DeviceDataDAO deviceDataDAO){
        final CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

        try {
            final String outMotionFile = accountFilePath + ".motion.csv";
            final FileWriter motionFileWriter = new FileWriter(outMotionFile);
            final FileReader in = new FileReader(accountFilePath);

            final CSVPrinter motionCSVPrinter = new CSVPrinter(motionFileWriter, csvFileFormat);
            motionCSVPrinter.printRecord(Lists.newArrayList("account_id",
                    "tracker_id",
                    "value",
                    "kick_off",
                    "duration",
                    "motion_range",
                    "ts",
                    "offset_millis"));


            final String outSensorFile = accountFilePath + ".sensor.csv";
            final FileWriter sensorFileWriter = new FileWriter(outSensorFile);

            final CSVPrinter sensorCSVPrinter = new CSVPrinter(sensorFileWriter, csvFileFormat);
            sensorCSVPrinter.printRecord(Lists.newArrayList("account_id",
                    "device_id",
                    "ts",
                    "offset_millis",
                    "ambient_temp",
                    "ambient_light",
                    "ambient_light_variance",
                    "ambient_light_peakiness",
                    "ambient_humidity",
                    "ambient_air_quality",
                    "ambient_air_quality_raw",
                    "ambient_dust_variance",
                    "ambient_dust_min",
                    "ambient_dust_max",
                    "audio_num_disturbances",
                    "audio_peak_disturbances_db",
                    "audio_peak_background_db"));


            final Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
            for (final CSVRecord record : records) {
                final long accountId = Long.valueOf(record.get("account_id"));
                final DateTime targetNight = DateTimeUtil.ymdStringToDateTime(record.get("day_of_night"));
                LOGGER.info("Pulling data from account {} date {}", accountId, targetNight);
                final DateTime queryStartLocalUTC = targetNight.withZone(DateTimeZone.UTC).withHourOfDay(20);
                final DateTime queryEndTimeLocalUTC = targetNight.plusHours(16);

                final List<TrackerMotion> trackerMotionList = trackerMotionDAO.getBetweenLocalUTC(accountId,
                        queryStartLocalUTC, queryEndTimeLocalUTC);
                for(final TrackerMotion trackerMotion:trackerMotionList){
                    motionCSVPrinter.printRecord(trackerMotion.accountId,
                            trackerMotion.trackerId,
                            trackerMotion.value,
                            trackerMotion.kickOffCounts,
                            trackerMotion.onDurationInSeconds,
                            trackerMotion.motionRange,
                            trackerMotion.timestamp,
                            trackerMotion.offsetMillis);
                }

                final List<DeviceData> deviceData = deviceDataDAO.getBetweenByLocalTime(accountId,
                        queryStartLocalUTC,
                        queryEndTimeLocalUTC);
                for(final DeviceData deviceDatum:deviceData){
                    sensorCSVPrinter.printRecord(deviceDatum.accountId,
                            deviceDatum.deviceId,
                            deviceDatum.dateTimeUTC.getMillis(),
                            deviceDatum.offsetMillis,
                            deviceDatum.ambientTemperature,
                            deviceDatum.ambientLight,
                            deviceDatum.ambientLightVariance,
                            deviceDatum.ambientLightPeakiness,
                            deviceDatum.ambientHumidity,
                            deviceDatum.ambientAirQuality,
                            deviceDatum.ambientAirQualityRaw,
                            deviceDatum.ambientDustVariance,
                            deviceDatum.ambientDustMin,
                            deviceDatum.ambientDustMax,
                            deviceDatum.audioNumDisturbances,
                            deviceDatum.audioPeakDisturbancesDB,
                            deviceDatum.audioPeakBackgroundDB);
                }

            }

            motionCSVPrinter.close();

            sensorCSVPrinter.close();



        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: {}", accountFilePath);
        } catch (IOException ioe){
            LOGGER.error("IO Exception {}", ioe.getMessage());
        } finally {
            LOGGER.info("Done");
        }

    }
}
