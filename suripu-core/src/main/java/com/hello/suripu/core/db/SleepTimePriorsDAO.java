package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DataScience.WakeProbabilityDistributions;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/15/15.
 */
public interface SleepTimePriorsDAO {

    public Optional<WakeProbabilityDistributions> getWakeDistributionByDay(final Long accountId, final DateTime day);

    public WakeProbabilityDistributions getWakeDistributionByDayEnforcingDefault(final Long accountId, final DateTime day, final WakeProbabilityDistributions default_dist);

    public void updateWakeProbabilityDistributions( final Long accountId,final DateTime day,final WakeProbabilityDistributions newDist);
}
