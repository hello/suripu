package com.hello.suripu.core.util;

import com.google.common.collect.Maps;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jarredheinrich on 11/1/16.
 */
public class TimeZoneOffsetMap {

    public static TimeZoneOffsetMap create(final String timeZoneId, final long startTimeUTC, final long endTimeUTC) {
        final TreeMap<Long,Integer> offsetByTimeUTC = Maps.newTreeMap();
        for (long timeUTC = startTimeUTC; timeUTC < endTimeUTC; timeUTC += DateTimeConstants.MILLIS_PER_MINUTE){
            offsetByTimeUTC.put(timeUTC, DateTimeZone.forID(timeZoneId).getOffset(timeUTC));
        }

        return new TimeZoneOffsetMap(offsetByTimeUTC);
    }
    public static TimeZoneOffsetMap create(final Integer offset, final long startTimeUTC, final long endTimeUTC) {
        final TreeMap<Long,Integer> offsetByTimeUTC = Maps.newTreeMap();
        for (long timeUTC = startTimeUTC; timeUTC < endTimeUTC; timeUTC += DateTimeConstants.MILLIS_PER_MINUTE){
            offsetByTimeUTC.put(timeUTC,offset);
        }

        return new TimeZoneOffsetMap(offsetByTimeUTC);
    }

    final TreeMap<Long,Integer> offsetByTimeUTC;

    private TimeZoneOffsetMap(final TreeMap<Long, Integer> offsetByTimeUTC) {
        this.offsetByTimeUTC = offsetByTimeUTC;
    }

    /* Get offset that is nearest in time to the timestamp */
    public int get(final long timestampUTC) {

        if (offsetByTimeUTC.isEmpty()) {
            return 0;
        }

        //get entry >=
        Map.Entry<Long,Integer> higherEntry = offsetByTimeUTC.ceilingEntry(timestampUTC);

        //get entry <=
        Map.Entry<Long,Integer> lowerEntry = offsetByTimeUTC.floorEntry(timestampUTC);

        if (higherEntry == null) {
            higherEntry = lowerEntry;
        }

        if (lowerEntry == null) {
            lowerEntry = higherEntry;
        }


        //find nearest
        final long diffHigher = Math.abs(higherEntry.getKey() - timestampUTC);
        final long diffLower = Math.abs(lowerEntry.getKey() - timestampUTC);

        if (diffHigher > diffLower) {
            return lowerEntry.getValue();
        }
        else {
            return higherEntry.getValue();
        }

    }

}
