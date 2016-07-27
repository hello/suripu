package com.hello.suripu.core.models;

import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Insights.SumLengthData;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 7/11/16.
 */
public class AggStatsTest {
    final long FAKE_ACCOUNT_ID = 9999L;
    final String FAKE_EXTERNAL_DEVICE_ID = "fakeit";

    @Test
    public void test_builder() {
        final DateTime dateLocal = DateTime.parse("2016-01-01");

        final int device_data_length = 100;
        final int tracker_motion_length = 50;

        final int avg_temp = 1002;
        final int max_temp = 1001;
        final int min_temp = 1000;
        final int avg_humid = 1005;
        final int avg_dust = 1006;

        final Map<Integer, SumLengthData> sumLengthMicroLuxHourMap = Maps.newHashMap();
        sumLengthMicroLuxHourMap.put(22, new SumLengthData(0, 0));
        sumLengthMicroLuxHourMap.put(23, new SumLengthData(0, 0));
        sumLengthMicroLuxHourMap.put(0, new SumLengthData(0, 0));

        final AggStats.Builder builder = new AggStats.Builder()
                .withAccountId(FAKE_ACCOUNT_ID)
                .withDateLocal(dateLocal)
                .withExternalDeviceId(FAKE_EXTERNAL_DEVICE_ID)

                .withDeviceDataLength(device_data_length)
                .withTrackerMotionLength(tracker_motion_length)

                .withAvgDailyTemp(avg_temp)
                .withMaxDailyTemp(max_temp)
                .withMinDailyTemp(min_temp)
                .withAvgDailyHumidity(avg_humid)
                .withAvgDailyDustDensity(avg_dust)

                .withSumLenMicroLuxHourMap(sumLengthMicroLuxHourMap);

        final AggStats aggStats = builder.build();

        assertThat(aggStats.accountId, is(FAKE_ACCOUNT_ID));
        assertThat(aggStats.dateLocal, is(dateLocal));
        assertThat(aggStats.externalDeviceId, is(FAKE_EXTERNAL_DEVICE_ID));

        assertThat(aggStats.deviceDataLength, is(device_data_length));
        assertThat(aggStats.trackerMotionLength, is(tracker_motion_length));

        assertThat(aggStats.avgDailyTemp, is(avg_temp));
        assertThat(aggStats.maxDailyTemp, is(max_temp));
        assertThat(aggStats.minDailyTemp, is(min_temp));

        assertThat(aggStats.avgDailyHumidity, is(avg_humid));

        assertThat(aggStats.avgDailyDustDensity, is(avg_dust));
    }
}
