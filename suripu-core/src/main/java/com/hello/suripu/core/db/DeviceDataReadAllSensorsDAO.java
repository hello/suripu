package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import org.joda.time.DateTime;

/**
 * Created by benjo on 1/21/16.
 */
public interface DeviceDataReadAllSensorsDAO {
    AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(
            Long queryStartTimestampInUTC,
            Long queryEndTimestampInUTC,
            Long accountId,
            String externalDeviceId,
            int slotDurationInMinutes,
            Integer missingDataDefaultValue,
            Optional<Device.Color> color,
            Optional<Calibration> calibrationOptional,
            Boolean useAudioPeakEnergy);

    Optional<String> getSensePairedBetween(Long accountId, DateTime startTime, DateTime endTime);
}
