package com.hello.suripu.core.insights.models;

/**
 * Created by kingshy on 2/9/15.
 */
public class SleepDuration {

    public static class recommendation {
        public int minHours;
        public int maxHours;
        public int absoluteMinHours;
        public int absoluteMaxHours;

        public recommendation(final int minHours, final int maxHours, final int absoluteMinHours, final int absoluteMaxHours) {
            this.minHours = minHours;
            this.maxHours = maxHours;
            this.absoluteMinHours = absoluteMinHours;
            this.absoluteMaxHours = absoluteMaxHours;
        }
    }

    public static recommendation getSleepDurationRecommendation(final int userAgeInYears)  {
        // see http://www.prnewswire.com/news-releases/expert-panel-recommends-new-sleep-durations-300028815.html
        int minHours, maxHours, absoluteMin, absoluteMax;
        if (userAgeInYears == 0) {
            // no DOB, assume it's an adult
            minHours = 7;
            maxHours = 9;
            absoluteMin = 6;
            absoluteMax = 10;
        } else if (userAgeInYears >= 65) { // older adults
            minHours = 7;
            maxHours = 8;
            absoluteMin = 5;
            absoluteMax = 9;
        } else if (userAgeInYears >= 26) { // adults
            minHours = 7;
            maxHours = 9;
            absoluteMin = 6;
            absoluteMax = 10;
        } else if (userAgeInYears >= 18) { // young adults
            minHours = 7;
            maxHours = 9;
            absoluteMin = 6;
            absoluteMax = 11;
        } else if (userAgeInYears >= 14) { // teenagers
            minHours = 8;
            maxHours = 10;
            absoluteMin = 7;
            absoluteMax = 11;
        } else if (userAgeInYears >= 6) { // school age children
            minHours = 9;
            maxHours = 11;
            absoluteMin = 7;
            absoluteMax = 12;
        } else {
            // preschoolers
            minHours = 10;
            maxHours = 13;
            absoluteMin = 8;
            absoluteMax = 14;
        }
        return new recommendation(minHours, maxHours, absoluteMin, absoluteMax);
    }
}
