package com.hello.suripu.core.insights.models;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jingyun on 7/25/15.
 */
public class WakeStdDevData {
    // see https://s3.amazonaws.com/hello-data/insights-raw-data/wakeStdDev_distribution_2015_07_25.csv
    private static String DISTRIBUTION_DATA =
                "0,0.002\n"+
                "1,0.4\n"+
                "2,0.6\n"+
                "3,0.9\n"+
                "4,1.1\n"+
                "5,1.4\n"+
                "6,1.6\n"+
                "7,1.9\n"+
                "8,2.2\n"+
                "9,2.5\n"+
                "10,2.8\n"+
                "11,3.1\n"+
                "12,3.4\n"+
                "13,3.8\n"+
                "14,4.1\n"+
                "15,4.5\n"+
                "16,4.8\n"+
                "17,5.2\n"+
                "18,5.6\n"+
                "19,6.0\n"+
                "20,6.4\n"+
                "21,6.8\n"+
                "22,7.3\n"+
                "23,7.7\n"+
                "24,8.2\n"+
                "25,8.7\n"+
                "26,9.2\n"+
                "27,9.7\n"+
                "28,10.2\n"+
                "29,10.7\n"+
                "30,11.2\n"+
                "31,11.3\n"+
                "32,12.4\n"+
                "33,12.9\n"+
                "34,13.5\n"+
                "35,14.1\n"+
                "36,14.7\n"+
                "37,15.4\n"+
                "38,16.0\n"+
                "39,16.6\n"+
                "40,17.3\n"+
                "41,18.0\n"+
                "42,18.7\n"+
                "43,19.4\n"+
                "44,20.1\n"+
                "45,20.8\n"+
                "46,21.5\n"+
                "47,22.3\n"+
                "48,23.0\n"+
                "49,23.8\n"+
                "50,24.6\n"+
                "51,25.4\n"+
                "52,26.2\n"+
                "53,27.0\n"+
                "54,27.8\n"+
                "55,28.6\n"+
                "56,29.5\n"+
                "57,30.3\n"+
                "58,31.2\n"+
                "59,32.0\n"+
                "60,32.9\n"+
                "61,33.8\n"+
                "62,34.7\n"+
                "63,35.6\n"+
                "64,36.5\n"+
                "65,37.4\n"+
                "66,38.3\n"+
                "67,39.2\n"+
                "68,40.1\n"+
                "69,41.0\n"+
                "70,41.9\n"+
                "71,42.9\n"+
                "72,43.8\n"+
                "73,44.7\n"+
                "74,45.7\n"+
                "75,46.6\n"+
                "76,47.5\n"+
                "77,48.5\n"+
                "78,49.4\n"+
                "79,50.4\n"+
                "80,51.3\n"+
                "81,52.2\n"+
                "82,53.2\n"+
                "83,54.1\n"+
                "84,55.0\n"+
                "85,55.9\n"+
                "86,56.9\n"+
                "87,57.8\n"+
                "88,58.7\n"+
                "89,59.6\n"+
                "90,60.5\n"+
                "91,61.4\n"+
                "92,62.3\n"+
                "93,63.2\n"+
                "94,64.1\n"+
                "95,64.9\n"+
                "96,65.8\n"+
                "97,66.6\n"+
                "98,67.5\n"+
                "99,68.3\n"+
                "100,69.8\n"+
                "101,70.0\n"+
                "102,70.1\n"+
                "103,71.6\n"+
                "104,72.3\n"+
                "105,73.1\n"+
                "106,73.9\n"+
                "107,74.6\n"+
                "108,75.4\n"+
                "109,76.1\n"+
                "110,76.8\n"+
                "111,77.5\n"+
                "112,78.2\n"+
                "113,78.9\n"+
                "114,79.6\n"+
                "115,80.3\n"+
                "116,80.9\n"+
                "117,81.5\n"+
                "118,82.2\n"+
                "119,82.8\n"+
                "120,83.4\n"+
                "121,84.0\n"+
                "122,84.5\n"+
                "123,85.1\n"+
                "124,85.6\n"+
                "125,86.2\n"+
                "126,86.7\n"+
                "127,87.2\n"+
                "128,87.7\n"+
                "129,88.2\n"+
                "130,88.7\n"+
                "131,89.1\n"+
                "132,89.6\n"+
                "133,90.0\n"+
                "134,90.4\n"+
                "135,90.8\n"+
                "136,91.2\n"+
                "137,91.6\n"+
                "138,92.0\n"+
                "139,92.4\n"+
                "140,92.7\n"+
                "141,93.1\n"+
                "142,93.4\n"+
                "143,93.7\n"+
                "144,94.1\n"+
                "145,94.4\n"+
                "146,94.6\n"+
                "147,94.9\n"+
                "148,95.2\n"+
                "149,95.5\n"+
                "150,95.7\n"+
                "151,96.0\n"+
                "152,96.2\n"+
                "153,96.4\n"+
                "154,96.6\n"+
                "155,96.9\n"+
                "156,97.1\n"+
                "157,97.3\n"+
                "158,97.4\n"+
                "159,97.6\n"+
                "160,97.8\n"+
                "161,98.0\n"+
                "162,98.1\n"+
                "163,98.3\n"+
                "164,98.4\n"+
                "165,98.6\n"+
                "166,98.7\n"+
                "167,98.8\n"+
                "168,98.9";

    private static final double MAX_WAKE_STDDEV = 169;
    private static final int MAX_WAKE_STDDEV_PERCENTILE = 99;


    private ImmutableMap<Integer, Integer> distributionLookup;


    public WakeStdDevData() {
        final Map<Integer, Integer> temp = new HashMap<>();
        final String[] rows = DISTRIBUTION_DATA.split("\n");
        for(final String row : rows) {
            final String[] rowParts = row.split(",");
            final int wakeStdDev = Integer.parseInt(rowParts[0]);
            final int percentile =  (int) Float.parseFloat(rowParts[1]);
            temp.put(wakeStdDev, percentile);
        }
        this.distributionLookup = ImmutableMap.copyOf(temp);
    }


    public int getWakeStdDevPercentile(final int wakeStdDevValue) {
        if (wakeStdDevValue >= MAX_WAKE_STDDEV) {
            return MAX_WAKE_STDDEV_PERCENTILE;
        }
        return this.distributionLookup.get(wakeStdDevValue);
    }

}
