package com.hello.suripu.core.db;

import com.hello.suripu.core.models.Insights.InsightSchedule;

/**
 * Created by jyfan on 3/3/16.
 */
public interface InsightScheduleDAO {
    public InsightSchedule getInsightScheduleCBTI_V1();
    public InsightSchedule getInsightScheduleDefault();
}
