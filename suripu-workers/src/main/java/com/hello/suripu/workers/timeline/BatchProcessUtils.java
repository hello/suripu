package com.hello.suripu.workers.timeline;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 1/29/15.
 */
public class BatchProcessUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(BatchProcessUtils.class);

    public static enum DataTypeFilter{
        PILL_HEARTBEAT,
        PILL_DATA
    }

    public static Map<String, Set<DateTime>> groupRequestingPillIdsByDataType(final List<SenseCommandProtos.batched_pill_data> batchedPillData,
                                                                              final DataTypeFilter filter){
        final HashMap<String, Set<DateTime>> pillIdTargetDatesMap = new HashMap<>();

        for (final SenseCommandProtos.batched_pill_data data : batchedPillData) {
            for(final SenseCommandProtos.pill_data pillData:data.getPillsList()) {
                boolean shouldProcess = false;
                if (pillData.hasUptime() && filter == DataTypeFilter.PILL_HEARTBEAT) {  // Only triggered by heartbeat
                    shouldProcess = true;
                }

                if(pillData.hasMotionDataEntrypted() && filter == DataTypeFilter.PILL_DATA){
                    shouldProcess = true;
                }

                if(!shouldProcess){
                    continue;
                }

                if (!pillIdTargetDatesMap.containsKey(pillData.getDeviceId())) {
                    pillIdTargetDatesMap.put(pillData.getDeviceId(), new HashSet<DateTime>());
                }

                final DateTime targetDateUTC = new DateTime(pillData.getTimestamp() * 1000L, DateTimeZone.UTC);
                pillIdTargetDatesMap.get(pillData.getDeviceId()).add(targetDateUTC);
            }

        }

        return pillIdTargetDatesMap;
    }

    public static Map<Long, Set<DateTime>> groupAccountAndExpireDateLocalUTC(final Map<String, Set<DateTime>> groupedPillIdRequestDateUTC,
                                                                          final DeviceDAO deviceDAO,
                                                                          final MergedUserInfoDynamoDB mergedUserInfoDynamoDB){
        final Map<Long, Set<DateTime>> accountIdTargetDatesLocalUTCMap = new HashMap<>();
        for(final String pillId:groupedPillIdRequestDateUTC.keySet()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final List<DeviceAccountPair> accountsLinkedWithPill = deviceDAO.getLinkedAccountFromPillId(pillId);
            if (accountsLinkedWithPill.size() == 0) {
                LOGGER.warn("No account linked with pill {}", pillId);
                continue;
            }

            if (accountsLinkedWithPill.size() > 1) {
                LOGGER.warn("{} accounts linked with pill {}, only account {} get the timeline",
                        accountsLinkedWithPill.size(),
                        pillId,
                        accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId);
            }

            final long accountId = accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId;
            final List<DeviceAccountPair> sensesLinkedWithAccount = deviceDAO.getSensesForAccountId(accountId);
            if (sensesLinkedWithAccount.size() == 0) {
                LOGGER.warn("No sense linked with account {} from pill {}", accountId, pillId);
                continue;
            }

            if (sensesLinkedWithAccount.size() > 1) {
                LOGGER.warn("{} senses linked with account {}, only sense {} got the timeline.",
                        sensesLinkedWithAccount.size(),
                        accountId,
                        sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId);
            }

            final String senseId = sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId;
            final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(senseId, accountId);

            if (!dateTimeZoneOptional.isPresent()) {
                LOGGER.error("No timezone for sense {} account {}", senseId, accountId);
                continue;
            }

            final Set<DateTime> targetDatesLocalUTC = new HashSet<>();
            for(final DateTime dataTime:groupedPillIdRequestDateUTC.get(pillId)){
                final DateTime dataTimeInLocal = dataTime.withZone(dateTimeZoneOptional.get());
                targetDatesLocalUTC.add(DateTimeUtil.getTargetDateLocalUTCFromLocalTime(dataTimeInLocal));

            }

            if(!targetDatesLocalUTC.isEmpty()){
                accountIdTargetDatesLocalUTCMap.put(accountId, targetDatesLocalUTC);
            }
        }

        return accountIdTargetDatesLocalUTCMap;
    }

    public static Map<Long, Set<DateTime>> groupAccountAndProcessDateLocalUTC(final Map<String, Set<DateTime>> groupedPillIdRequestDateUTC,
                                                                              final int startProcessHourOfDay,
                                                                              final int endProcessHourOfDay,
                                                                              final DateTime now,
                                                                              final DeviceDAO deviceDAO,
                                                                              final MergedUserInfoDynamoDB mergedUserInfoDynamoDB){
        final Map<Long, Set<DateTime>> accountIdTargetDatesLocalUTCMap = new HashMap<>();
        for(final String pillId:groupedPillIdRequestDateUTC.keySet()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final List<DeviceAccountPair> accountsLinkedWithPill = deviceDAO.getLinkedAccountFromPillId(pillId);
            if (accountsLinkedWithPill.size() == 0) {
                LOGGER.warn("No account linked with pill {}", pillId);
                continue;
            }

            if (accountsLinkedWithPill.size() > 1) {
                LOGGER.warn("{} accounts linked with pill {}, only account {} get the timeline",
                        accountsLinkedWithPill.size(),
                        pillId,
                        accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId);
            }

            final long accountId = accountsLinkedWithPill.get(accountsLinkedWithPill.size() - 1).accountId;
            final List<DeviceAccountPair> sensesLinkedWithAccount = deviceDAO.getSensesForAccountId(accountId);
            if (sensesLinkedWithAccount.size() == 0) {
                LOGGER.warn("No sense linked with account {} from pill {}", accountId, pillId);
                continue;
            }

            if (sensesLinkedWithAccount.size() > 1) {
                LOGGER.warn("{} senses linked with account {}, only sense {} got the timeline.",
                        sensesLinkedWithAccount.size(),
                        accountId,
                        sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId);
            }

            final String senseId = sensesLinkedWithAccount.get(sensesLinkedWithAccount.size() - 1).externalDeviceId;
            final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(senseId, accountId);

            if (!dateTimeZoneOptional.isPresent()) {
                LOGGER.error("No timezone for sense {} account {}", senseId, accountId);
                continue;
            }

            final Set<DateTime> targetDatesLocalUTC = new HashSet<>();
            for(final DateTime dataTime:groupedPillIdRequestDateUTC.get(pillId)){
                final DateTime dataTimeInLocal = dataTime.withZone(dateTimeZoneOptional.get());
                final DateTime processTargetDateLocalUTC = DateTimeUtil.getTargetDateLocalUTCFromLocalTime(dataTimeInLocal);
                final DateTime nowInLocal = now.withZone(dateTimeZoneOptional.get());
                final DateTime todaysTargetDateLocalUTC = DateTimeUtil.getTargetDateLocalUTCFromLocalTime(nowInLocal);
                if((nowInLocal.getHourOfDay() < startProcessHourOfDay || nowInLocal.getHourOfDay() > endProcessHourOfDay) && todaysTargetDateLocalUTC.equals(processTargetDateLocalUTC)){
                    LOGGER.debug("too early to process data for pill {}, date {}, user time {}", pillId, todaysTargetDateLocalUTC, nowInLocal);
                    continue;
                }

                targetDatesLocalUTC.add(processTargetDateLocalUTC);
            }

            if(!targetDatesLocalUTC.isEmpty()){
                accountIdTargetDatesLocalUTCMap.put(accountId, targetDatesLocalUTC);
            }
        }

        return accountIdTargetDatesLocalUTCMap;
    }


}
