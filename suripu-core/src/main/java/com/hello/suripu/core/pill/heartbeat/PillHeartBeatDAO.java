package com.hello.suripu.core.pill.heartbeat;

import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;

import java.util.List;

public interface PillHeartBeatDAO {

    void put(final PillHeartBeat pillHeartBeat);
    void put(List<PillHeartBeat> pillHeartBeats);
    List<DeviceStatus> get(final String pillId);
    List<DeviceStatus> get(final String pillId, final DateTime end);

}
