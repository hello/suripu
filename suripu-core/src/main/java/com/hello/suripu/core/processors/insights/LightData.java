package com.hello.suripu.core.processors.insights;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kingshy on 1/5/15.
 */
public class LightData {
    // see https://s3.amazonaws.com/hello-data/insights-raw-data/light_distribution_2015_12_14.csv
    private static String DISTRIBUTION_DATA = "0,0\n" +
                    "1,0\n" +
                    "2,19\n" +
                    "3,30\n" +
                    "4,39\n" +
                    "5,44\n" +
                    "6,49\n" +
                    "7,53\n" +
                    "8,56\n" +
                    "9,59\n" +
                    "10,61\n" +
                    "11,63\n" +
                    "12,65\n" +
                    "13,66\n" +
                    "14,68\n" +
                    "15,69\n" +
                    "16,71\n" +
                    "17,72\n" +
                    "18,73\n" +
                    "19,74\n" +
                    "20,75\n" +
                    "21,75\n" +
                    "22,76\n" +
                    "23,77\n" +
                    "24,77\n" +
                    "25,78\n" +
                    "26,79\n" +
                    "27,79\n" +
                    "28,80\n" +
                    "29,80\n" +
                    "30,80\n" +
                    "31,81\n" +
                    "32,81\n" +
                    "33,82\n" +
                    "34,82\n" +
                    "35,82\n" +
                    "36,83\n" +
                    "37,83\n" +
                    "38,83\n" +
                    "39,84\n" +
                    "40,84\n" +
                    "41,84\n" +
                    "42,84\n" +
                    "43,85\n" +
                    "44,85\n" +
                    "45,85\n" +
                    "46,85\n" +
                    "47,86\n" +
                    "48,86\n" +
                    "49,86\n" +
                    "50,86\n" +
                    "51,86\n" +
                    "52,87\n" +
                    "53,87\n" +
                    "54,87\n" +
                    "55,87\n" +
                    "56,88\n" +
                    "57,88\n" +
                    "58,88\n" +
                    "59,88\n" +
                    "60,88\n" +
                    "61,89\n" +
                    "62,89\n" +
                    "63,89\n" +
                    "64,89\n" +
                    "65,89\n" +
                    "66,89\n" +
                    "67,90\n" +
                    "68,90\n" +
                    "69,90\n" +
                    "70,90\n" +
                    "71,90\n" +
                    "72,90\n" +
                    "73,90\n" +
                    "74,90\n" +
                    "75,90\n" +
                    "76,90\n" +
                    "77,91\n" +
                    "78,91\n" +
                    "79,91\n" +
                    "80,91\n" +
                    "81,91\n" +
                    "82,91\n" +
                    "83,91\n" +
                    "84,91\n" +
                    "85,92\n" +
                    "86,92\n" +
                    "87,92\n" +
                    "88,92\n" +
                    "89,92\n" +
                    "90,92\n" +
                    "91,92\n" +
                    "92,92\n" +
                    "93,92\n" +
                    "94,92\n" +
                    "95,92\n" +
                    "96,92\n" +
                    "97,93\n" +
                    "98,93\n" +
                    "99,93\n" +
                    "100,93\n" +
                    "101,93\n" +
                    "102,93\n" +
                    "103,93\n" +
                    "104,93\n" +
                    "105,93\n" +
                    "106,93\n" +
                    "107,93\n" +
                    "108,94\n" +
                    "109,94\n" +
                    "110,94\n" +
                    "111,94\n" +
                    "112,94\n" +
                    "113,94\n" +
                    "114,94\n" +
                    "115,94\n" +
                    "116,94\n" +
                    "117,94\n" +
                    "118,94\n" +
                    "119,94\n" +
                    "120,95\n" +
                    "121,95\n" +
                    "122,95\n" +
                    "123,95\n" +
                    "124,95\n" +
                    "125,95\n" +
                    "126,95\n" +
                    "127,95\n" +
                    "128,95\n" +
                    "129,95\n" +
                    "130,95\n" +
                    "131,95\n" +
                    "132,95\n" +
                    "133,95\n" +
                    "134,95\n" +
                    "135,95\n" +
                    "136,95\n" +
                    "137,95\n" +
                    "138,95\n" +
                    "139,95\n" +
                    "140,95\n" +
                    "141,95\n" +
                    "142,95\n" +
                    "143,95\n" +
                    "144,95\n" +
                    "145,95\n" +
                    "146,96\n" +
                    "147,96\n" +
                    "148,96\n" +
                    "149,96\n" +
                    "150,96\n" +
                    "151,96\n" +
                    "152,96\n" +
                    "153,96\n" +
                    "154,96\n" +
                    "155,96\n" +
                    "156,96\n" +
                    "157,96\n" +
                    "158,96\n" +
                    "159,96\n" +
                    "160,96\n" +
                    "161,96\n" +
                    "162,96\n" +
                    "163,96\n" +
                    "164,96\n" +
                    "165,96\n" +
                    "166,96\n" +
                    "167,96\n" +
                    "168,96\n" +
                    "169,96\n" +
                    "170,96\n" +
                    "171,96\n" +
                    "172,96\n" +
                    "173,96\n" +
                    "174,96\n" +
                    "175,97\n" +
                    "176,97\n" +
                    "177,97\n" +
                    "178,97\n" +
                    "179,97\n" +
                    "180,97\n" +
                    "181,97\n" +
                    "182,97\n" +
                    "183,97\n" +
                    "184,97\n" +
                    "185,97\n" +
                    "186,97\n" +
                    "187,97\n" +
                    "188,97\n" +
                    "189,97\n" +
                    "190,97\n" +
                    "191,97\n" +
                    "192,97\n" +
                    "193,97\n" +
                    "194,97\n" +
                    "195,97\n" +
                    "196,97\n" +
                    "197,97\n" +
                    "198,97\n" +
                    "199,97\n" +
                    "200,97\n" +
                    "201,97\n" +
                    "202,97\n" +
                    "203,97\n" +
                    "204,97\n" +
                    "205,97\n" +
                    "206,97\n" +
                    "207,97\n" +
                    "208,97\n" +
                    "209,97\n" +
                    "210,97\n" +
                    "211,97\n" +
                    "212,97\n" +
                    "213,97\n" +
                    "214,97\n" +
                    "215,97\n" +
                    "216,97\n" +
                    "217,97\n" +
                    "218,97\n" +
                    "219,97\n" +
                    "220,98\n" +
                    "221,98\n" +
                    "222,98\n" +
                    "223,98\n" +
                    "224,98\n" +
                    "225,98\n" +
                    "226,98\n" +
                    "227,98\n" +
                    "228,98\n" +
                    "229,98\n" +
                    "230,98\n" +
                    "231,98\n" +
                    "232,98\n" +
                    "233,98\n" +
                    "234,98\n" +
                    "235,98\n" +
                    "236,98\n" +
                    "237,98\n" +
                    "238,98\n" +
                    "239,98\n" +
                    "240,98\n" +
                    "241,98\n" +
                    "242,98\n" +
                    "243,98\n" +
                    "244,98\n" +
                    "245,98\n" +
                    "246,98\n" +
                    "247,98\n" +
                    "248,98\n" +
                    "249,98\n" +
                    "250,98\n" +
                    "251,98\n" +
                    "252,98\n" +
                    "253,98\n" +
                    "254,98\n" +
                    "255,98\n" +
                    "256,98\n" +
                    "257,98\n" +
                    "258,98\n" +
                    "259,98\n" +
                    "260,98\n" +
                    "261,98\n" +
                    "262,98\n" +
                    "263,98\n" +
                    "264,98\n" +
                    "265,98\n" +
                    "266,98\n" +
                    "267,98\n" +
                    "268,98\n" +
                    "269,98\n" +
                    "270,98\n" +
                    "271,98\n" +
                    "272,98\n" +
                    "273,98\n" +
                    "274,98\n" +
                    "275,98\n" +
                    "276,98\n" +
                    "277,98\n" +
                    "278,98\n" +
                    "279,98\n" +
                    "280,98\n" +
                    "281,98\n" +
                    "282,98\n" +
                    "283,98\n" +
                    "284,98\n" +
                    "285,98\n" +
                    "286,98\n" +
                    "287,98\n" +
                    "288,98\n" +
                    "289,98\n" +
                    "290,98\n" +
                    "291,98\n" +
                    "292,98\n" +
                    "293,98\n" +
                    "294,99\n";

    private static final int MAX_LIGHT = 294;
    private static final int MAX_LIGHT_PERCENTILE = 99;

    private ImmutableMap<Integer, Integer> distributionLookup;

    public LightData() {
        final Map<Integer, Integer> temp = new HashMap<>();
        final String[] rows = DISTRIBUTION_DATA.split("\n");
        for(String row : rows) {
            final String[] parts = row.split(",");
            final int lux = Integer.parseInt(parts[0]);
            final int percentile =  (int) Float.parseFloat(parts[1]);
            temp.put(lux, percentile);
        }
        this.distributionLookup = ImmutableMap.copyOf(temp);
    }

    public int getLightPercentile(final int lightValue) {
        if (lightValue > MAX_LIGHT) {
                return MAX_LIGHT_PERCENTILE;
        }
        return this.distributionLookup.get(lightValue);
    }

}
