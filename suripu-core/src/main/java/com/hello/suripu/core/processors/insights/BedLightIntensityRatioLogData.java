package com.hello.suripu.core.processors.insights;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jyfan on 8/28/15.
 */
public class BedLightIntensityRatioLogData {
    // see https://s3.amazonaws.com/hello-data/insights-raw-data/bedLightIntensityRatioLog_distribution_2015_08_28.csv
    private static String DISTRIBUTION_DATA = "-7,1\n" +
            "-6,4\n" +
            "-5,9\n" +
            "-4,17\n" +
            "-3,28\n" +
            "-2,42\n" +
            "-1,57\n" +
            "0,77\n" +
            "1,91\n" +
            "2,97\n";

    private static final int MIN_BED_LIGHT_INTENSITY_RATIO_LOG = -6;
    private static final int MIN_BED_LIGHT_INTENSITY_RATIO_LOG_PERCENTILE = 4;

    private static final int MAX_BED_LIGHT_INTENSITY_RATIO_LOG = 2;
    private static final int MAX_BED_LIGHT_INTENSITY_RATIO_LOG_PERCENTILE = 97;

    private ImmutableMap<Integer, Integer> distributionLookup;
    public  BedLightIntensityRatioLogData() {
        final Map<Integer, Integer> temp = new HashMap<>();
        final String[] rows = DISTRIBUTION_DATA.split("\n");
        for(final String row : rows) {
            final String[] rowParts = row.split(",");
            final int bedLightIntensityRatioLog = Integer.parseInt(rowParts[0]);
            final int percentile =  (int) Float.parseFloat(rowParts[1]);
            temp.put(bedLightIntensityRatioLog, percentile);
        }
        this.distributionLookup = ImmutableMap.copyOf(temp);
    }

    public int getBedLightIntensityPercentile(final int bedLightIntensityRatioLog) {
        if (bedLightIntensityRatioLog < MIN_BED_LIGHT_INTENSITY_RATIO_LOG) {
            return MIN_BED_LIGHT_INTENSITY_RATIO_LOG_PERCENTILE;
        }
        else if (bedLightIntensityRatioLog > MAX_BED_LIGHT_INTENSITY_RATIO_LOG) {
            return MAX_BED_LIGHT_INTENSITY_RATIO_LOG_PERCENTILE;
        }
        return this.distributionLookup.get(bedLightIntensityRatioLog);
    }
}
