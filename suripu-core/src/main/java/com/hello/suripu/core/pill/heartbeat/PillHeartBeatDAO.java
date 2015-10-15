package com.hello.suripu.core.pill.heartbeat;

import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

public interface PillHeartBeatDAO {

    void put(final PillHeartBeat pillHeartBeat);
    void put(Set<PillHeartBeat> pillHeartBeats);
    List<DeviceStatus> get(final String pillId);
    List<DeviceStatus> get(final String pillId, final DateTime end);

}
