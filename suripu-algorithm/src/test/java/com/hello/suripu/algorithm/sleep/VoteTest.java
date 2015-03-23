package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.sleep.Vote;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/19/15.
 */
public class VoteTest extends CSVFixtureTest {

    @Test
    public void testTrim(){
        final List<AmplitudeData> original = new ArrayList<>();
        final List<AmplitudeData> expected = new ArrayList<>();

        final DateTime now = DateTime.now();
        final DateTime trimTime = now.plusMinutes(11);
        for(int i = 0; i < 60; i++){
            original.add(new AmplitudeData(now.plusMinutes(i).getMillis(), i, 0));
            if(!now.plusMinutes(i).isBefore(trimTime)){
                expected.add(original.get(original.size() - 1));
            }
        }

        final List<AmplitudeData> actual = Vote.trim(original, trimTime.getMillis(), original.get(original.size() - 1).timestamp);
        assertThat(actual.size(), is(expected.size()));

        for(int i = 0; i < actual.size(); i++){
            assertThat(actual.get(i).timestamp, is(expected.get(i).timestamp));
            assertThat(actual.get(i).offsetMillis, is(expected.get(i).offsetMillis));
            assertThat(actual.get(i).amplitude, is(actual.get(i).amplitude));
        }
    }
}
