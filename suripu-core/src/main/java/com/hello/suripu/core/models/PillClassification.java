package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.util.PillStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Created by pangwu on 6/23/15.
 */
public class PillClassification {
    public final long id;
    public final long internalPillId;
    public final String pillId;
    public final DateTime last24PointWindowStartTime;
    public final DateTime last72PointWindowStartTime;
    public final float lastClassificationBatteryLevel;
    public final float max24HoursBatteryDelta;
    public final float max72HoursBatteryDelta;

    public final PillStatus status;

    public PillClassification(final long id,
                                final long internalPillId,
                               final String pillId,
                               final long last24PointWindowStartMillis,
                               final long last72PointWindowStartMillis,
                               final float lastClassificationBatteryLevel,
                               final float max24HoursBatteryDelta,
                               final float max72HoursBatteryDelta,
                               final int classificationResult){
        this.id = id;
        this.internalPillId = internalPillId;
        this.pillId = pillId;
        this.last24PointWindowStartTime = new DateTime(last24PointWindowStartMillis, DateTimeZone.UTC);
        this.last72PointWindowStartTime = new DateTime(last72PointWindowStartMillis, DateTimeZone.UTC);

        this.lastClassificationBatteryLevel = lastClassificationBatteryLevel;
        this.status = PillStatus.fromInt(classificationResult);

        this.max24HoursBatteryDelta = max24HoursBatteryDelta;
        this.max72HoursBatteryDelta = max72HoursBatteryDelta;

    }

    public static int floatToInt(final float value){
        return (int)(value * 100);
    }

    public static float intToFloat(final int value){
        return value / 100f;
    }

    public static DateTime getQueryStartTime(final Optional<PillClassification> lastRecord){
        if(!lastRecord.isPresent()){
            return new DateTime(0, DateTimeZone.UTC);
        }

        return lastRecord.get().last24PointWindowStartTime;
    }

    public static List<DeviceStatus> restoreBuffer(final long internalPillId, final DateTime queryStartTime, final DateTime queryEndTime, final PillHeartBeatDAO pillHeartBeatDAO){
        return pillHeartBeatDAO.getPillStatusBetweenUTC(internalPillId, queryStartTime, queryEndTime);
    }

    /*
    * Decision boundary from SVM
    * -0.0064 * d2d + 0.1281 * d3d - 3.4344 = 0
     */
    public static PillStatus classify(final float[] features){
        if(features.length < 2){
            return PillStatus.UNKNOWN;
        }

        final float dayToDayDrop = features[0];
        final float threeDayDrop = features[1];
        final float result = -0.0064f * dayToDayDrop + 0.1281f * threeDayDrop - 3.4344f;
        if(result > 0){
            return PillStatus.NORMAL;
        }

        return PillStatus.BAD;
    }
}
