package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DataScience.WakeProbabilityDistributions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;

/**
 * Created by benjo on 2/15/15.
 */
public class SleepTimePriorsLocalImpl implements SleepTimePriorsDAO {
    HashMap<String,WakeProbabilityDistributions> _mymap;

    public SleepTimePriorsLocalImpl() {
        _mymap = new HashMap<String,WakeProbabilityDistributions>();
    }

    private static String getKey(Long account_id, DateTime day) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        return String.format("%d::%s",account_id,fmt.print(day));
    }

    @Override
    public Optional<WakeProbabilityDistributions> getWakeDistributionByDay(Long accountId, DateTime day) {

        final String key = this.getKey(accountId,day);
        Optional<WakeProbabilityDistributions> ret = Optional.absent();

        WakeProbabilityDistributions value = null;

        synchronized (_mymap) {
            value = _mymap.get(key);
        }

        if (value != null) {
            ret = Optional.of(value);
        }

        return ret;
    }

    @Override
    public WakeProbabilityDistributions getWakeDistributionByDayEnforcingDefault(final Long accountId, DateTime day, final WakeProbabilityDistributions default_dist) {

        final String key = this.getKey(accountId,day);

        WakeProbabilityDistributions value = null;

        synchronized (_mymap) {
            value = _mymap.get(key);

            if (value == null) {
                value = default_dist;
            }
        }



        return value;
    }

    @Override
    public void updateWakeProbabilityDistributions( final Long accountId,final DateTime day,final WakeProbabilityDistributions newDist) {
        final String key = this.getKey(accountId,day);

        synchronized (_mymap) {
            _mymap.put(key, newDist);
        }
    }

}
