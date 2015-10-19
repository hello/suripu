package com.hello.suripu.core.pill.heartbeat;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

public interface PillHeartBeatDAO {

    void put(final PillHeartBeat pillHeartBeat);
    void put(Set<PillHeartBeat> pillHeartBeats);

    /**
     * Get most recent heartbeat if it exists
     * @param pillId
     * @return Optional of heartbeat
     */
    Optional<PillHeartBeat> get(final String pillId);

    /**
     * Returns a list of heartbeat prior to the start datetime provided
     * It is not up to the caller to define how many records are returned.
     * @param pillId
     * @param start
     * @return
     */
    List<PillHeartBeat> get(final String pillId, final DateTime start);

}
