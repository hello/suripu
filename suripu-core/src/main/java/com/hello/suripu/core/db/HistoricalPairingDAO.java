package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoricalPairingDAO implements PairingDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalPairingDAO.class);

    private final DeviceReadDAO deviceReadDAO;
    private final DeviceDataReadAllSensorsDAO deviceDataReadAllSensorsDAO;

    public HistoricalPairingDAO(DeviceReadDAO deviceReadDAO, DeviceDataReadAllSensorsDAO deviceDataReadAllSensorsDAO) {
        this.deviceReadDAO = deviceReadDAO;
        this.deviceDataReadAllSensorsDAO = deviceDataReadAllSensorsDAO;
    }

    @Override
    public Optional<String> senseId(long accountId, DateTime start, DateTime end) {
        final Optional<DeviceAccountPair> deviceIdPair = deviceReadDAO.getMostRecentSensePairByAccountId(accountId);
        if (deviceIdPair.isPresent()) {
            final Seconds seconds = Seconds.secondsBetween(start, deviceIdPair.get().created);
            // Current Sense was paired before the date we're interested in.
            if(seconds.getSeconds() < 0) {
                return Optional.of(deviceIdPair.get().externalDeviceId);
            }
        }

        // If no current Sense is paired, look at historical data
        LOGGER.warn("action=get-historical-pairing account_id={} start_time={} end_time={}", accountId, start, end);
        final Optional<String> deviceId = deviceDataReadAllSensorsDAO.getSensePairedBetween(accountId, start, end);
        if(!deviceId.isPresent()) {
            LOGGER.warn("msg=no-sense-paired account_id={} start={}", accountId, start);
            return Optional.absent();
        }
        return deviceId;
    }

    @Override
    public Optional<String> pillId(long accountId, DateTime start, DateTime stop) {
        LOGGER.warn("NOT_IMPLEMENTED");
        return Optional.absent();
    }
}
