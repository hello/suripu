package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTimeZone;

import java.awt.Color;
import java.util.List;

public interface MergedUserInfoDAO {
    String tableName();

    boolean setTimeZone(String deviceId, long accountId, DateTimeZone timeZone);

    boolean deletePillColor(String senseId, long accountId, String pillId);

    Optional<Color> setNextPillColor(String senseId, long accountId, String pillId);

    boolean setPillColor(String deviceId, long accountId, String pillId, Color pillColor);

    boolean setAlarms(String deviceId, long accountId,
                      long lastUpdatedAt,
                      List<Alarm> oldAlarms,
                      List<Alarm> newAlarms,
                      DateTimeZone userTimeZone);

    @Deprecated
    boolean createUserInfoWithEmptyAlarmList(String deviceId, long accountId, DateTimeZone userTimeZone);

    boolean setRingTime(String deviceId, long accountId, RingTime ringTime);

    Optional<UserInfo> getInfo(String deviceId, long accountId);

    Optional<UserInfo> unlinkAccountToDevice(long accountId, String deviceId);

    List<UserInfo> getInfo(String deviceId);

    Optional<DateTimeZone> getTimezone(String senseId, Long accountId);
}
