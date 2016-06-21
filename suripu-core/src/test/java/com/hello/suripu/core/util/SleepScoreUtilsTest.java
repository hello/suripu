package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 2/25/15.
 */
public class SleepScoreUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreUtilsTest.class);

    @Test
    public void testPerfectScore() {
        final int age = 23;
        final List<Integer> sleepDurationMinutes = Lists.newArrayList(420, 450, 480, 510, 540);

        for (int i = 0; i < sleepDurationMinutes.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, sleepDurationMinutes.get(i));
            LOGGER.trace("value {} -> {}", sleepDurationMinutes.get(i), score);
            assertThat(score, is(SleepScoreUtils.DURATION_MAX_SCORE));
        }
    }

    @Test
    public void testTerribleScores() {
        final List<Integer> ages = Lists.newArrayList(16, 23, 27, 68);
        final List<Integer> duration = Lists.newArrayList(330, 299, 299, 790);
        for (int i = 0; i < duration.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(ages.get(i), duration.get(i));
            LOGGER.trace("value {} -> {}", duration.get(i), score);
            assertThat(score, is(SleepScoreUtils.DURATION_MIN_SCORE));
        }
    }

    @Test
    public void testRandomScores() {
        final int age = 23;
        final List<Integer> duration = Lists.newArrayList(330, 400, 600, 650);
        final List<Integer> correct = Lists.newArrayList(27, 68, 68, 59);
        for (int i = 0; i < duration.size(); i++) {
            final int score = SleepScoreUtils.getSleepDurationScore(age, duration.get(i));
            LOGGER.trace("value {} -> {}", duration.get(i), score);
            assertThat(score, is(correct.get(i)));
        }

    }

    @Test
    public void testScoresV3WithParam() {
        final int age = 23;
        final int durThreshold = 540;
        final List<Integer> sleepDurationMinutes = Lists.newArrayList(80, 299, 360, 510, 780);
        final List<Integer> correct = Lists.newArrayList(26, 45, 56, 92, 83);
        for (int i = 0; i < sleepDurationMinutes.size(); i++) {
            final int score = SleepScoreUtils.getSleepScoreDurationV3(1001L, age, durThreshold, sleepDurationMinutes.get(i));
            LOGGER.info("value {} -> {}", sleepDurationMinutes.get(i), score);
            assertThat(score, is(correct.get(i)));
        }
    }

    @Test
    public void testScoresV3WithoutParam() {
        final int age = 23;
        final int durThreshold = 0;
        final List<Integer> sleepDurationMinutes = Lists.newArrayList(80, 299, 360, 510, 780);
        final List<Integer> correct = Lists.newArrayList(26, 49, 63, 96, 83);
        for (int i = 0; i < sleepDurationMinutes.size(); i++) {
            final int score = SleepScoreUtils.getSleepScoreDurationV3(1001L, age, durThreshold, sleepDurationMinutes.get(i));
            LOGGER.info("value {} -> {}", sleepDurationMinutes.get(i), score);
            assertThat(score, is(correct.get(i)));
        }
    }

    @Test
    public void testScoresV4(){
        final int age = 23;
        final List<Integer> durThreshold = Lists.newArrayList(480, 495, 440, 480, 491);
        final List<Integer> sleepDurationMinutes = Lists.newArrayList(424, 495, 458, 560, 462);

        final List<Float> motionFrequency = Lists.newArrayList(0.1108f, 0.0343f, 0.0786f, 0f, 0.1429f);
        final List<Integer> timesAwake = Lists.newArrayList(2, 1, 2, 0, 4);
        final List<Integer> agitatedSleepDuration = Lists.newArrayList(9, 0, 4, 0, 17);
        final List<Integer> correct = Lists.newArrayList(67, 91, 82, 95, 61);
        for (int i = 0; i < sleepDurationMinutes.size(); i++) {
            final int durScoreV3 = SleepScoreUtils.getSleepScoreDurationV3(1001L, age, durThreshold.get(i), sleepDurationMinutes.get(i));
            final int score = SleepScoreUtils.getSleepScoreDurationV4(1001L, durScoreV3, motionFrequency.get(i), timesAwake.get(i), agitatedSleepDuration.get(i));
            assertThat(score, is(correct.get(i)));
        }
    }

    private List<TrackerMotion> trackerMotionList(String fixturePath) {
        final URL fixtureCSVFile = Resources.getResource(fixturePath);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final TrackerMotion trackerMotion = new TrackerMotion(
                        Long.parseLong(columns[0].trim()), // id
                        Long.parseLong(columns[1].trim()), // account_id
                        Long.parseLong(columns[2].trim()), // tracker_id
                        DateTime.parse(columns[4].trim(), DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT)).getMillis(), // ts
                        Integer.valueOf(columns[3].trim()), // svm_no_gravity
                        Integer.valueOf(columns[5].trim()), // tz offset
                        // skipping local_utc
                        Long.parseLong(columns[7].trim()),
                        Long.parseLong(columns[8].trim()),
                        Long.parseLong(columns[9].trim())
                        );
                //if(trackerMotion.value > 0){
                trackerMotions.add(trackerMotion);
                //}
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return trackerMotions;
    }

    @Test
    public void testAgitatedSleep(){
        final List<TrackerMotion> trackerMotionList = trackerMotionList("fixtures/tracker_motion/2015-05-08.csv");
        int  agitatedSleep= SleepScoreUtils.getAgitatedSleep(trackerMotionList, 1431089780000L, 1431188031000L);
        assertThat(agitatedSleep , is(22));
    }


    @Test
    public void testNoNegativeScores() {
//        final List<Integer> sleepDurationMinutes = ContiguousSet.create(Range.closed(1, 2000), DiscreteDomain.integers()).asList();
        final List<TrackerMotion> trackerMotionList = trackerMotionList("fixtures/tracker_motion/2015-05-08.csv");
        MotionScore  score = SleepScoreUtils.getSleepMotionScore(new DateTime(2015, 5, 8, 20, 0,0), trackerMotionList, 1431179880000L, 1431180000000L);
        assertThat(score.score  >= 0, is(Boolean.TRUE));
        score = SleepScoreUtils.getSleepMotionScore(new DateTime(2015, 5, 8, 20, 0,0), trackerMotionList, 0L, 0L);
        assertThat(score.score  >= 0, is(Boolean.TRUE));
    }

    @Test
    public void testEmptyTrackerMotionList() {
        final Optional<MotionScore> score = SleepScoreUtils.getSleepMotionScoreMaybe(new DateTime(2015, 5, 8, 20, 0, 0), Collections.EMPTY_LIST, 0L, 0L);
        assertThat(score.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testAlmostEmptyTrackerMotionList() {
        final List<TrackerMotion> trackerMotionList = trackerMotionList("fixtures/tracker_motion/2015-05-08.csv");
        final Optional<MotionScore> score = SleepScoreUtils.getSleepMotionScoreMaybe(new DateTime(2015, 5, 8, 20, 0, 0),trackerMotionList.subList(0,2), 0L, 0L);
        assertThat(score.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void testCalculateSoundScore() {
        final List<Integer> soundEventCounts = Lists.newArrayList(1, 2, 3, 4, 5, 6);
        final List<Integer> expectedScores = Lists.newArrayList(80, 60, 40, 20, 0, 0);
        for (int i = 0, soundEventCountsSize = soundEventCounts.size(); i < soundEventCountsSize; i++) {
            Integer soundEventCount = soundEventCounts.get(i);
            Integer expectedScore = expectedScores.get(i);
            assertThat(SleepScoreUtils.calculateSoundScore(soundEventCount), is(expectedScore));
        }
    }

    @Test
    public void testCalculateSensorAverageInTimeRange() {
        final List<Sample> samples = Lists.newArrayList(
            new Sample(50, 30f, 0),
            new Sample(30, 5f, 0),
            new Sample(10, 39.3f, 0),
            new Sample(5, 12f, 0),
            new Sample(96, 77f, 0),
            new Sample(40, 9f, 0),
            new Sample(78, 3f, 0),
            new Sample(60, 22f, 0),
            new Sample(30, 1f, 0)
        );
        final long startTime = 20L;
        final long endTime = 70L;
        final float average = SleepScoreUtils.calculateSensorAverageInTimeRange(samples, startTime, endTime);
        assertThat(average, is(13.4f));
    }

    @Test
    public void testCalculateTemperatureScore() {
        // The average of the samples is used. So one sample == the value of that sample.

        final List<Sample> good = Lists.newArrayList(new Sample(0, 23f, 0));
        assertThat(SleepScoreUtils.calculateTemperatureScore(good, 0, 0), is(100));

        final List<Sample> warningHigh = Lists.newArrayList(new Sample(0, 24f, 0));
        assertThat(SleepScoreUtils.calculateTemperatureScore(warningHigh, 0, 0), is(75));

        final List<Sample> warningLow = Lists.newArrayList(new Sample(0, 14f, 0));
        assertThat(SleepScoreUtils.calculateTemperatureScore(warningLow, 0, 0), is(75));

        final List<Sample> alertHigh = Lists.newArrayList(new Sample(0, 30f, 0));
        assertThat(SleepScoreUtils.calculateTemperatureScore(alertHigh, 0, 0), is(50));

        final List<Sample> alertLow = Lists.newArrayList(new Sample(0, 7f, 0));
        assertThat(SleepScoreUtils.calculateTemperatureScore(alertLow, 0, 0), is(50));
    }

    @Test
    public void testCalculateHumidityScore() {
        // The average of the samples is used. So one sample == the value of that sample.

        final List<Sample> good = Lists.newArrayList(new Sample(0, 40f, 0));
        assertThat(SleepScoreUtils.calculateHumidityScore(good, 0, 0), is(100));

        final List<Sample> warningHigh = Lists.newArrayList(new Sample(0, 65f, 0));
        assertThat(SleepScoreUtils.calculateHumidityScore(warningHigh, 0, 0), is(75));

        final List<Sample> warningLow = Lists.newArrayList(new Sample(0, 25f, 0));
        assertThat(SleepScoreUtils.calculateHumidityScore(warningLow, 0, 0), is(75));

        final List<Sample> alertHigh = Lists.newArrayList(new Sample(0, 80f, 0));
        assertThat(SleepScoreUtils.calculateHumidityScore(alertHigh, 0, 0), is(50));

        final List<Sample> alertLow = Lists.newArrayList(new Sample(0, 15f, 0));
        assertThat(SleepScoreUtils.calculateHumidityScore(alertLow, 0, 0), is(50));
    }

    @Test
    public void testCalculateAggregateEnvironmentScore() {
        assertThat(SleepScoreUtils.calculateAggregateEnvironmentScore(100, 100, 100, 100, 100), is(100));
        assertThat(SleepScoreUtils.calculateAggregateEnvironmentScore(50, 100, 100, 50, 50), is(70));
        assertThat(SleepScoreUtils.calculateAggregateEnvironmentScore(100, 50, 100, 75, 50), is(75));
        assertThat(SleepScoreUtils.calculateAggregateEnvironmentScore(100, 75, 100, 50, 75), is(80));
    }

    @Test
    public void testCalculateAggregateEnvironmentScoreBound() {
        final int temperatureScore = new Random().nextInt(101);
        final int humdityScore = new Random().nextInt(101);
        final int soundScore = new Random().nextInt(101);
        final int lightScore = new Random().nextInt(101);
        final int particulateScore = new Random().nextInt(101);
        final int envScore = SleepScoreUtils.calculateAggregateEnvironmentScore(temperatureScore, humdityScore, soundScore, lightScore, particulateScore);
        assertThat(envScore > Collections.max(asList(temperatureScore, humdityScore, soundScore, lightScore, particulateScore)), is(false));
        assertThat(envScore < Collections.min(asList(temperatureScore, humdityScore, soundScore, lightScore, particulateScore)), is(false));
        assertThat(envScore <= 100 && envScore >=0, is(true));
    }

}
