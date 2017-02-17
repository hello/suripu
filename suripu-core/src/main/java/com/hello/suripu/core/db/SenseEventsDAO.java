package com.hello.suripu.core.db;

import com.hello.suripu.core.metrics.DeviceEvents;
import org.joda.time.DateTime;

import java.util.List;

public interface SenseEventsDAO {
    List<DeviceEvents> get(String deviceId, DateTime start, Integer limit);

    List<DeviceEvents> get(String deviceId, DateTime start);

    List<DeviceEvents> getAlarms(String deviceId, DateTime start, DateTime end);

    Integer write(List<DeviceEvents> deviceEventsList);
}
