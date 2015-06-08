package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.MotionScore;
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
    public void testNoNegativeScores() {
//        final List<Integer> sleepDurationMinutes = ContiguousSet.create(Range.closed(1, 2000), DiscreteDomain.integers()).asList();
        final List<TrackerMotion> trackerMotionList = trackerMotionList("fixtures/tracker_motion/2015-05-08.csv");
//        final MotionScore  score = SleepScoreUtils.getSleepMotionScore(new DateTime(2015, 5, 8, 20, 0,0), trackerMotionList, 1431179880000L, 1431180000000L);
        final MotionScore  score = SleepScoreUtils.getSleepMotionScore(new DateTime(2015, 5, 8, 20, 0,0), trackerMotionList, 0L, 0L);
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
        assertThat(score.isPresent(), is(Boolean.FALSE));
    }
}
