package com.hello.suripu.core.db;

import com.hello.suripu.core.models.RingTime;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by benjo on 1/21/16.
 */
public interface RingTimeHistoryReadDAO {
    List<RingTime> getRingTimesBetween(String senseId, Long accountId,
                                       DateTime startTime,
                                       DateTime endTime);
}
