package com.hello.suripu.core.db;

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

    private static String getKey(Long account_id, DateTime day) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        return String.format("%d::%s",account_id,fmt.print(day));
    }

    @Override
    public WakeProbabilityDistributions getWakeDistributionByDay(final Long account_id, DateTime day, final WakeProbabilityDistributions default_dist) {

        final String key = this.getKey(account_id,day);

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
    public void updateWakeProbabilityDistributions( final Long account_id,final DateTime day,final WakeProbabilityDistributions new_dist) {
        final String key = this.getKey(account_id,day);

        synchronized (_mymap) {
            _mymap.put(key,new_dist);
        }
    }

}
