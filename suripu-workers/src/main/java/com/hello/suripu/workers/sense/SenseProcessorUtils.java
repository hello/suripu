package com.hello.suripu.workers.sense;


import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SenseProcessorUtils {
    public final static Integer CLOCK_SKEW_TOLERATED_IN_HOURS = 2;

    public static DateTime getSampleTime(final DateTime createdAtRounded, final DataInputProtos.periodic_data periodicData, final Boolean attemptToRecoverSenseReportedTimeStamp) {
        // To validate that the firmware is sending a correct unix timestamp
        // we need to compare it to something immutable, coming from a different clock (server)
        // We can't compare to now because now changes, and if we want to reprocess old data it will be immediately discarded

        final Long timestampMillis = periodicData.getUnixTime() * 1000L;
        final DateTime rawDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);

        // This is intended to check for very specific clock bugs from Sense
        final DateTime periodicDataSampleDateTime = attemptToRecoverSenseReportedTimeStamp
                ? DateTimeUtil.possiblySanitizeSampleTime(createdAtRounded, rawDateTime, CLOCK_SKEW_TOLERATED_IN_HOURS)
                : rawDateTime;
        return periodicDataSampleDateTime;
    }

    public static Boolean isClockOutOfSync(final DateTime sampleDateTime, final DateTime createdAtRounded) {
        return sampleDateTime.isAfter(createdAtRounded.plusHours(CLOCK_SKEW_TOLERATED_IN_HOURS)) || sampleDateTime.isBefore(createdAtRounded.minusHours(CLOCK_SKEW_TOLERATED_IN_HOURS));
    }

    public static Integer getFirmwareVersion(final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker, final DataInputProtos.periodic_data periodicData) {
        // Grab FW version from Batch or periodic data for EVT units
        final Integer firmwareVersion =  batchPeriodicDataWorker.getData().hasFirmwareVersion()
                ? batchPeriodicDataWorker.getData().getFirmwareVersion()
                : periodicData.getFirmwareVersion();
        return firmwareVersion;
    }
}
