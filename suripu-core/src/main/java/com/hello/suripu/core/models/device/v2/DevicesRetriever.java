package com.hello.suripu.core.models.device.v2;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.util.PillColorUtil;

import java.util.List;
import java.util.Map;

public class DevicesRetriever {

    public final DeviceDAO deviceDAO;
    public final DeviceDataDAO deviceDataDAO;
    public final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    public final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    public final PillHeartBeatDAO pillHeartBeatDAO;
    public final TrackerMotionDAO trackerMotionDAO;
    public final WifiInfoDAO wifiInfoDAO;
    public final SenseColorDAO senseColorDAO;
    public final Boolean isSenseLastSeenEnabled;
    public final Boolean isSensorsDBUnavailable;


    public DevicesRetriever(final DeviceDAO deviceDAO, final DeviceDataDAO deviceDataDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final PillHeartBeatDAO pillHeartBeatDAO, final TrackerMotionDAO trackerMotionDAO, final WifiInfoDAO wifiInfoDAO, final SenseColorDAO senseColorDAO, final Boolean isSenseLastSeenEnabled, final Boolean isSensorsDBUnavailable) {
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.wifiInfoDAO = wifiInfoDAO;
        this.senseColorDAO = senseColorDAO;
        this.isSenseLastSeenEnabled = isSenseLastSeenEnabled;
        this.isSensorsDBUnavailable = isSensorsDBUnavailable;
    }

    public Devices getAllDevices(final Long accountId) {
        final List<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
        final List<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
        final Optional<Pill.Color> pillColorOptional = retrievePillColor(accountId, senseAccountPairs);

        return new Devices(getSenses(senseAccountPairs), getPills(pillAccountPairs, pillColorOptional));
    }

    public List<Sense> getSenses(final List<DeviceAccountPair> senseAccountPairs) {
        final List<Sense> senses = Lists.newArrayList();

        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = retrieveSenseStatus(senseAccountPair);
            final Optional<WifiInfo> wifiInfoOptional = wifiInfoDAO.get(senseAccountPair.externalDeviceId);
            final Optional<Sense.Color> senseColorOptional = senseColorDAO.get(senseAccountPair.externalDeviceId);
            senses.add(Sense.create(senseAccountPair, senseStatusOptional, senseColorOptional, wifiInfoOptional));
        }
        return senses;
    }

    public List<Pill> getPills(final List<DeviceAccountPair> pillAccountPairs, final Optional<Pill.Color> pillColorOptional) {
        final List<Pill> pills = Lists.newArrayList();
        for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
            final Optional<DeviceStatus> pillStatusOptional = retrievePillStatus(pillAccountPair);
            pills.add(Pill.create(pillAccountPair, pillStatusOptional, pillColorOptional));
        }
        return pills;
    }

    private Optional<DeviceStatus> retrieveSenseStatus(final DeviceAccountPair senseAccountPair) {
        // First attempt: get it from last seen record in dynamo db
        if (isSenseLastSeenEnabled) {
            return sensorsViewsDynamoDB.senseStatus(senseAccountPair.externalDeviceId, senseAccountPair.accountId, senseAccountPair.internalDeviceId);
        }

        if (isSensorsDBUnavailable) {
            return Optional.absent();
        }
        // Second attempt: get it from sensor db with assumption that such sense has been active since an hour ago
        Optional<DeviceStatus> senseStatusOptional = deviceDataDAO.senseStatusLastHour(senseAccountPair.internalDeviceId);

        // Third attempt: get if from sensor db with assumption that such sense has been active since a week ago
        if (!senseStatusOptional.isPresent()) {
            senseStatusOptional = deviceDataDAO.senseStatusLastWeek(senseAccountPair.internalDeviceId);
        }
        return senseStatusOptional;
    }

    private Optional<DeviceStatus> retrievePillStatus(final DeviceAccountPair pillAccountPair) {
        // First attempt: get it from heartbeat
        Optional<DeviceStatus> pillStatusOptional = this.pillHeartBeatDAO.getPillStatus(pillAccountPair.internalDeviceId);

        // Second attempt: get it from tracker motion
        if (!pillStatusOptional.isPresent()) {
            pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId, pillAccountPair.accountId);
        }
        return pillStatusOptional;
    }

    private Optional<Pill.Color> retrievePillColor(final Long accountId, final List<DeviceAccountPair> senseAccountPairs) {
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            if (!accountId.equals(senseAccountPair.accountId)) {
                continue;
            }
            final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseAccountPair.externalDeviceId);
            if (userInfoList.isEmpty()) {
                return Optional.absent();
            }
            final Optional<OutputProtos.SyncResponse.PillSettings> pillSettingsOptional =  userInfoList.get(0).pillColor;
            if (pillSettingsOptional.isPresent()){
                return Optional.absent();
            }
            return Optional.of(PillColorUtil.displayPillColor(pillSettingsOptional.get().getPillColor()));
        }
        return Optional.absent();
    }
}
