package com.hello.suripu.core.db;

import com.hello.suripu.core.models.DeviceData;

import java.util.List;

/**
 * Created by jakepiccolo on 10/14/15.
 */
public interface DeviceDataIngestDAO {
    int batchInsertWithFailureFallback(final List<DeviceData> allDeviceData);
}
