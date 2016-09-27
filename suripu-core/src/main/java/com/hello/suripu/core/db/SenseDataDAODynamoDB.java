package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseDataDAODynamoDB implements SenseDataDAO {

    private static Logger LOGGER = LoggerFactory.getLogger(SenseDataDAODynamoDB.class);
    final private PairingDAO pairingDAO;
    final private DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB;
    final private SenseColorDAO senseColorDAO;
    final private CalibrationDAO calibrationDAO;

    final private static int SLOT_DURATION_MINUTES = 1;

    public SenseDataDAODynamoDB(final PairingDAO pairingDAO, final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB, SenseColorDAO senseColorDAO, CalibrationDAO calibrationDAO) {
        this.pairingDAO = pairingDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.calibrationDAO = calibrationDAO;
    }

    @Override
    public Optional<AllSensorSampleList> get(long accountId, DateTime date, DateTime startTimeLocalUTC, DateTime endTimeLocalUTC, DateTime currentTimeUTC, int tzOffsetMillis) {

        // get all sensor data, used for light and sound disturbances, and presleep-insights

        final Optional<String> senseIdOptional = pairingDAO.senseId(accountId, startTimeLocalUTC, endTimeLocalUTC);
        if(!senseIdOptional.isPresent()) {
            LOGGER.warn("msg=no-sense-paired account_id={} start={} end={}", accountId, startTimeLocalUTC, endTimeLocalUTC);
            return Optional.absent();
        }
        final DateTime startTimeUTC = startTimeLocalUTC.minusMillis(tzOffsetMillis);
        final DateTime endTimeUTC = endTimeLocalUTC.minusMillis(tzOffsetMillis);

        final String senseId = senseIdOptional.get();
        // get color of sense, yes this matters for the light sensor
        final Optional<Device.Color> optionalColor = senseColorDAO.getColorForSense(senseId);

        final Optional<Calibration> calibrationOptional = calibrationDAO.getStrict(senseId);

        // query dates in utc_ts (table has an index for this)
        LOGGER.debug("Query all sensors with utc ts for account {}", accountId);

        final AllSensorSampleList allSensorSampleList = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                startTimeUTC.getMillis(),
                endTimeUTC.getMillis(),
                accountId, senseId, SLOT_DURATION_MINUTES, -1 ,optionalColor, calibrationOptional,
                false // Don't use the new audio peak energy since the models haven't trained on it.
        );

        return Optional.of(allSensorSampleList);
    }
}
