package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimeZoneHistory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jarredheinrich on 11/1/16.
 */
public class TimeZoneOffsetMap {

    final TreeMap<Long,TimeZoneHistory> timezoneIdHistories;

    public static TimeZoneOffsetMap createFromTimezoneHistoryList(final List<TimeZoneHistory> timezoneHistoryList) {

        final TreeMap<Long,TimeZoneHistory> timezoneIdHistories = Maps.newTreeMap();

        //map from UTC time of history and ID
        for (final TimeZoneHistory history : timezoneHistoryList) {
            timezoneIdHistories.put(history.updatedAt,history);
        }

        return new TimeZoneOffsetMap(timezoneIdHistories);
    }


    public TimeZoneOffsetMap(final TreeMap<Long,TimeZoneHistory> timezoneIdHistories) {
        this.timezoneIdHistories = timezoneIdHistories;
    }


    public List<SleepSegment> remapSleepSegmentOffsets(final List<SleepSegment> segments) {

        final List<SleepSegment> newSegments = Lists.newArrayList();

        for (final SleepSegment segment : segments) {

            final Optional<Integer> newOffset = this.getOffset(segment.getTimestamp());

            final int offset;
            if (!newOffset.isPresent()) {
                offset = 0;
            }
            else {
                offset = newOffset.get();
            }

            newSegments.add(segment.createCopyWithNewOffset(offset));
        }

        return newSegments;
    }

    /* Get timezone id that is nearest in time to the timestamp */
    public String getTimeZoneIdWithUTCDefault(final long timestampUTC) {

        //floorEntry -- Returns a key-value mapping associated with
        // the greatest key less than or equal to the given key,
        // or null if there is no such key.
        final Map.Entry<Long,TimeZoneHistory> entry = timezoneIdHistories.floorEntry(timestampUTC+ DateTimeConstants.MILLIS_PER_HOUR * 12);

        if (entry == null) {
            return "UTC";
        }

        if (entry.getValue() == null) {
            return "UTC";
        }

        return entry.getValue().timeZoneId;
    }

    /* Get offset that is nearest in time to the timestamp */
    public Optional<Integer> getOffset(final long timestampUTC) {

        //floorEntry -- Returns a key-value mapping associated with
        // the greatest key less than or equal to the given key,
        // or null if there is no such key.
        final Map.Entry<Long,TimeZoneHistory> entry = timezoneIdHistories.floorEntry(timestampUTC);

        if (entry == null) {
            return Optional.absent();
        }

        if (entry.getValue() == null) {
            return Optional.absent();
        }

        return Optional.of(DateTimeZone.forID(entry.getValue().timeZoneId).getOffset(timestampUTC));
    }

    public int getOffsetWithDefaultAsZero(final long timestampUTC) {
        final Optional<Integer> offset = getOffset(timestampUTC);

        if (!offset.isPresent()) {
            return 0;
        }

        return offset.get();
    }

    public Optional<DateTime> mapDateTime(final DateTime dateTime){
         final Optional<Integer> offset = getOffset(dateTime.getMillis());

        if (!offset.isPresent()) {
            return Optional.absent();
        }

        final Map.Entry<Long,TimeZoneHistory> entry = timezoneIdHistories.floorEntry(dateTime.getMillis());

        if (entry == null) {
            return Optional.absent();
        }

        if (entry.getValue() == null) {
            return Optional.absent();
        }

        return Optional.of(new DateTime(dateTime.getMillis()).withZone(DateTimeZone.forID(entry.getValue().timeZoneId)));
    }

    public DateTime mapDateTimeWithDefaultTimezoneAsUTC(final DateTime dateTime) {
        final Optional<DateTime> mappedDateTime = mapDateTime(dateTime);

        if (!mappedDateTime.isPresent()) {
            return new DateTime(dateTime.getMillis()).withZone(DateTimeZone.UTC);
        }

        return mappedDateTime.get();
    }

    public Long getUTCFromLocalTime(final long localTime) {

        //NOTE this is not strictly kosher at all.
        //the correct way to fix this is to actually store offset when we store timeline feedback.
        //fuck. --BEJ
        //treat local time as UTC
        final Map.Entry<Long,TimeZoneHistory> entry = timezoneIdHistories.floorEntry(localTime + DateTimeConstants.MILLIS_PER_HOUR * 12);

        if (entry == null) {
            return localTime;
        }

        if (entry.getValue() == null) {
            return localTime;
        }

        return DateTimeZone.forID(entry.getValue().timeZoneId).convertLocalToUTC(localTime,false);


    }
    public Event getEventWithCorrectOffset(final Event event){
        final int offset = getOffsetWithDefaultAsZero(event.getStartTimestamp());

        final Event eventWithCorrectoffset = Event.createFromType(event.getType(), event.getStartTimestamp(), event.getEndTimestamp(), offset, event.getDescription(), event.getSoundInfo(), event.getSleepDepth());
        return eventWithCorrectoffset;
    }

    public DateTime getCurrentLocalDateTimeWithUTCDefault(){
        return DateTime.now(DateTimeZone.forID(getTimeZoneIdWithUTCDefault(DateTime.now(DateTimeZone.UTC).getMillis())));
    }

}
