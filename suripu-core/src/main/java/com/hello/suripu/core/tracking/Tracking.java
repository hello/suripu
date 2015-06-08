package com.hello.suripu.core.tracking;

import com.hello.suripu.core.diagnostic.Count;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.List;

public class Tracking {


    public static Integer uptime(final List<Count> countList, DateTime ref) {
        Integer i = 0;
        for(final Count count : countList) {
            i += count.count;
        }

        final Integer minutes = Minutes.minutesBetween(countList.get(0).date, ref).getMinutes();

        final Float result = Math.min(i / (float) minutes * 100, 100);
        return Math.round(result);
    }
}
