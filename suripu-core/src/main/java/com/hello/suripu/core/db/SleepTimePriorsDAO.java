package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DataScience.SleepEventDistributions;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/15/15.
 */
public interface SleepTimePriorsDAO {

    public Optional<SleepEventDistributions> getWakeDistributionByDay(final Long accountId, final DateTime day);

    public SleepEventDistributions getWakeDistributionByDayEnforcingDefault(final Long accountId, final DateTime day, final SleepEventDistributions default_dist);

    public void updateWakeProbabilityDistributions( final Long accountId,final DateTime day,final SleepEventDistributions newDist);
}
