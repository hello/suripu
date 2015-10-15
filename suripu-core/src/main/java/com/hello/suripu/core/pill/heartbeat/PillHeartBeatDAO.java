package com.hello.suripu.core.pill.heartbeat;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

public interface PillHeartBeatDAO {

    void put(final PillHeartBeat pillHeartBeat);
    void put(Set<PillHeartBeat> pillHeartBeats);
    Optional<PillHeartBeat> get(final String pillId);
    List<PillHeartBeat> get(final String pillId, final DateTime end);

}
