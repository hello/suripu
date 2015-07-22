package com.hello.suripu.coredw.db;

import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Created by kingshy on 9/24/14.
 */
public class TrackerMotionDAOIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerMotionDAOIT.class);

    private final static Long ACCOUNT_ID = 3L;
    private final static Long BACKUP_TRACKER_ID = 9L;
    private final static int OFFSET_MILLIS = -25200000;
    private final static int MINUTE_MILLIS = 60 * 1000;

    private Long trackerID;
    private TrackerMotionDAO trackerMotionDAO;
    private DeviceDAO deviceDAO;
    private DatabaseConfiguration sensorDB;

    @Before
    public void setUp(){

        this.sensorDB = this.getSensorDB();
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource;
        try {
            dataSource = managedDataSourceFactory.build(sensorDB);
            final DBI jdbi = new DBI(dataSource);
            jdbi.registerArgumentFactory(new OptionalArgumentFactory(sensorDB.getDriverClass()));
            jdbi.registerContainerFactory(new ImmutableListContainerFactory());
            jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
            jdbi.registerContainerFactory(new OptionalContainerFactory());
            jdbi.registerArgumentFactory(new JodaArgumentFactory());

            this.trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);

            this.deviceDAO = jdbi.onDemand(DeviceDAO.class);
            final String deviceName = "Test_" + String.valueOf(DateTime.now().getMillis() / 1000L);
            try {
                final Long id = this.deviceDAO.registerPill(this.ACCOUNT_ID, deviceName);
                this.trackerID = id;
            } catch (UnableToExecuteStatementException exception) {
                this.trackerID = this.BACKUP_TRACKER_ID;
            }

            this.trackerMotionDAO.deleteDataTrackerID(this.trackerID);

        } catch (ClassNotFoundException e) {
            LOGGER.error("No driver found for database:{}", e.getMessage());
        }

    }

    @After
    public void cleanUp() {
        Integer deleted = this.trackerMotionDAO.deleteDataTrackerID(this.trackerID);
        Integer unregister = -1;
        if (this.trackerID != this.BACKUP_TRACKER_ID) {
            unregister = this.deviceDAO.unregisterPillByInternalPillId(this.trackerID);
        }
        LOGGER.debug("deleted tracker {}: {}, {}", this.trackerID, deleted, unregister);
    }


//    @Test
    public void testBatchInsertPass() {
        LOGGER.debug("===== Batch insert Test");
        final int batchSize = 80;
        final int dataSize = 8 * 60; // 8 hours
        final List<TrackerMotion> trackerData = this.getTrackerMotionData(5, dataSize);
        final int inserted = this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData, batchSize);
        assertThat(inserted, is(dataSize));
    }

//    @Test
    public void testBatchInsertWithDupes() {
        LOGGER.debug("===== Dupes Test");
        final int batchSize = 80;
        int dataSize = 2 * 60; // 2 hours
        final List<TrackerMotion> trackerData = this.getTrackerMotionData(5, dataSize);
        final int inserted = this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData, batchSize);
        assertThat(inserted, is(dataSize));

        dataSize = 10;
        final List<TrackerMotion> trackerData2 = this.getTrackerMotionData(5, dataSize);
        final int inserted2 = this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData2, batchSize);
        assertThat(inserted2, not(dataSize));

    }

    private DatabaseConfiguration getSensorDB() {

        final DatabaseConfiguration sensorDB = new DatabaseConfiguration();

        sensorDB.setDriverClass("org.postgresql.Driver");

        sensorDB.setUser("ingress_user");
        sensorDB.setPassword("hello ingress user");
        sensorDB.setUrl("jdbc:postgresql://localhost:5432/hello_2014_09_22");

        final Map<String, String> property = new HashMap<>();
        property.put("charSet", "UTF-8");
        sensorDB.setProperties(property);

        sensorDB.setMinSize(8);
        sensorDB.setMaxSize(32);
        sensorDB.setCheckConnectionWhileIdle(false);

        return sensorDB;
    }

    private List<TrackerMotion> getTrackerMotionData(final int seedValue, final int dataSize) {
        Random r = new Random(seedValue);
        final DateTime startDT = new DateTime(DateTime.now(), DateTimeZone.UTC).withTimeAtStartOfDay().plusHours(14);
        final long startTimestamp = startDT.getMillis();

        final List<TrackerMotion> trackerData = new ArrayList<>();
        for (int i = 0; i < dataSize; i++) {
            final TrackerMotion data = new TrackerMotion(
                    0L, this.ACCOUNT_ID,
                    this.trackerID,
                    startTimestamp + (i * this.MINUTE_MILLIS),
                    r.nextInt(100) * 100,
                    this.OFFSET_MILLIS,
                    0L,0L,0L);
            trackerData.add(data);
        }
        return trackerData;
    }
}
