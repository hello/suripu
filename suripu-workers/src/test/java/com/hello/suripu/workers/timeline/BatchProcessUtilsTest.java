package com.hello.suripu.workers.timeline;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 1/29/15.
 */
public class BatchProcessUtilsTest {


    private DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);

    @Test
    public void testGroupAccountAndProcessDateLocalUTCTooLateToProcess(){
        final HashMap<String, Set<DateTime>> groupedPillIds = new HashMap<>();
        final DateTime targetDate1 = new DateTime(2015, 1, 20, 7, 10, DateTimeZone.UTC);
        final DateTime targetDate2 = new DateTime(2015, 1, 20, 20, 0, DateTimeZone.UTC);
        final HashSet<DateTime> targetDatesUTC = new HashSet<>();
        targetDatesUTC.add(targetDate1);
        targetDatesUTC.add(targetDate2);

        final String pillId1 = "Pang's 911";
        final String sensId = "Sense";
        groupedPillIds.put(pillId1, targetDatesUTC);

        final long accountId = 1L;
        final List<DeviceAccountPair> deviceAccountPairsForPill = new ArrayList<>();
        deviceAccountPairsForPill.add(new DeviceAccountPair(accountId, 2L, pillId1, DateTimeUtil.MORPHEUS_DAY_ONE));

        final List<DeviceAccountPair> deviceAccountPairsForSense = new ArrayList<>();
        deviceAccountPairsForSense.add(new DeviceAccountPair(accountId, 3L, sensId, DateTimeUtil.MORPHEUS_DAY_ONE));

        when(deviceDAO.getLinkedAccountFromPillId(pillId1)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForPill));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForSense));

        when(mergedUserInfoDynamoDB.getTimezone(sensId, accountId)).thenReturn(Optional.of(DateTimeZone.UTC));

        final Map<Long, DateTime> groupedtargetDateLocalUTC = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(groupedPillIds,
                new DateTime(2015, 1, 20, 20, 1, DateTimeZone.UTC),
                5,
                11,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        assertThat(groupedtargetDateLocalUTC.containsKey(accountId), is(false));

    }

    @Test
    public void testGroupAccountAndProcessDateLocalUTCWithinProcessInterval(){
        final HashMap<String, Set<DateTime>> groupedPillIds = new HashMap<>();
        final DateTime targetDate1 = new DateTime(2015, 1, 20, 7, 10, DateTimeZone.UTC);
        final DateTime targetDate2 = new DateTime(2015, 1, 20, 8, 0, DateTimeZone.UTC);
        final HashSet<DateTime> targetDatesUTC = new HashSet<>();
        targetDatesUTC.add(targetDate1);
        targetDatesUTC.add(targetDate2);

        final String pillId1 = "Pang's 911";
        final String sensId = "Sense";
        groupedPillIds.put(pillId1, targetDatesUTC);

        final long accountId = 1L;
        final List<DeviceAccountPair> deviceAccountPairsForPill = new ArrayList<>();
        deviceAccountPairsForPill.add(new DeviceAccountPair(accountId, 2L, pillId1, DateTimeUtil.MORPHEUS_DAY_ONE));

        final List<DeviceAccountPair> deviceAccountPairsForSense = new ArrayList<>();
        deviceAccountPairsForSense.add(new DeviceAccountPair(accountId, 3L, sensId, DateTimeUtil.MORPHEUS_DAY_ONE));

        when(deviceDAO.getLinkedAccountFromPillId(pillId1)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForPill));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForSense));

        when(mergedUserInfoDynamoDB.getTimezone(sensId, accountId)).thenReturn(Optional.of(DateTimeZone.UTC));

        final Map<Long, DateTime> groupedtargetDateLocalUTC = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(groupedPillIds,
                new DateTime(2015, 1, 20, 10, 1, DateTimeZone.UTC),
                5,
                11,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        assertThat(groupedtargetDateLocalUTC.containsKey(accountId), is(true));
        assertThat(groupedtargetDateLocalUTC.get(accountId), is(new DateTime(2015, 1, 19, 0, 0, DateTimeZone.UTC)));

    }


    @Test
    public void testGroupAccountAndProcessDateLocalUTCTooEarlyToProcess(){
        final HashMap<String, Set<DateTime>> groupedPillIds = new HashMap<>();
        final DateTime targetDate1 = new DateTime(2015, 1, 20, 4, 10, DateTimeZone.UTC);
        final DateTime targetDate2 = new DateTime(2015, 1, 20, 3, 0, DateTimeZone.UTC);
        final HashSet<DateTime> targetDatesUTC = new HashSet<>();
        targetDatesUTC.add(targetDate1);
        targetDatesUTC.add(targetDate2);

        final String pillId1 = "Pang's 911";
        final String sensId = "Sense";
        groupedPillIds.put(pillId1, targetDatesUTC);

        final long accountId = 1L;
        final List<DeviceAccountPair> deviceAccountPairsForPill = new ArrayList<>();
        deviceAccountPairsForPill.add(new DeviceAccountPair(accountId, 2L, pillId1, DateTimeUtil.MORPHEUS_DAY_ONE));

        final List<DeviceAccountPair> deviceAccountPairsForSense = new ArrayList<>();
        deviceAccountPairsForSense.add(new DeviceAccountPair(accountId, 3L, sensId, DateTimeUtil.MORPHEUS_DAY_ONE));

        when(deviceDAO.getLinkedAccountFromPillId(pillId1)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForPill));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForSense));

        when(mergedUserInfoDynamoDB.getTimezone(sensId, accountId)).thenReturn(Optional.of(DateTimeZone.UTC));

        final Map<Long, DateTime> groupedtargetDateLocalUTC = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(groupedPillIds,
                new DateTime(2015, 1, 20, 4, 59, DateTimeZone.UTC),
                5,
                11,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        assertThat(groupedtargetDateLocalUTC.containsKey(accountId), is(false));

    }
}
