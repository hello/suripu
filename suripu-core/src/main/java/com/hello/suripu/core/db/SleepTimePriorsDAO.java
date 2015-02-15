package com.hello.suripu.core.db;

import com.hello.suripu.core.models.DataScience.WakeProbabilityDistributions;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/15/15.
 */
public interface SleepTimePriorsDAO {

    public WakeProbabilityDistributions getWakeDistributionByDay(final Long account_id,final DateTime day,final WakeProbabilityDistributions default_dist);

    public void updateWakeProbabilityDistributions( final Long account_id,final DateTime day,final WakeProbabilityDistributions new_dist);
}
