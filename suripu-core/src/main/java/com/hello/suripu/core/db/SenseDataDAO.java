package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AllSensorSampleList;
import org.joda.time.DateTime;

public interface SenseDataDAO {

    Optional<AllSensorSampleList> get(long accountId, DateTime date, DateTime startTimeLocalUTC, DateTime endTimeLocalUTC, DateTime currentTimeUTC, int tzOffsetMillis);
}
