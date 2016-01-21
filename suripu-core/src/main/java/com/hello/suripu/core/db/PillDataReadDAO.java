package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

/**
 * Created by benjo on 1/21/16.
 */
public interface PillDataReadDAO {
    ImmutableList<TrackerMotion> getBetweenLocalUTC(long accountId,
                                                    DateTime startLocalTime,
                                                    DateTime endLocalTime);
}
