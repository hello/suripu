import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.InternalScore;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import com.hello.suripu.algorithm.sleep.SleepDetectionAlgorithm;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 12/14/14.
 */
public class MotionScoreAlgorithmTest {

    @Test
    public void testGetRankingPositionMap(){
        final List<Comparable> tsArray = new ArrayList<>();
        final DateTime now = DateTime.now();
        tsArray.add(now.getMillis());
        tsArray.add(now.plusMinutes(1).getMillis());
        tsArray.add(now.plusMinutes(2).getMillis());
        Map<Comparable, Double> rankingMap = MotionScoreAlgorithm.getRankingPositionMap(tsArray, false);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(0d));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(2d / tsArray.size()));

        // Test order by DESC
        rankingMap = MotionScoreAlgorithm.getRankingPositionMap(tsArray, true);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(2d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(0d / tsArray.size()));

    }

    @Test
    public void testGetHighestScore(){
        final ArrayList<InternalScore> scores = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            scores.add(new InternalScore(null, Double.valueOf(i)));
        }

        final Optional<InternalScore> highestScore = MotionScoreAlgorithm.getHighestScore(scores);
        assertThat(highestScore.isPresent(), is(true));
        assertThat(highestScore.get().score, is(Double.valueOf(scores.size() - 1)));


        final ArrayList<InternalScore> emptyScores = new ArrayList<>();
        assertThat(MotionScoreAlgorithm.getHighestScore(emptyScores).isPresent(), is(false));
    }

    @Test
    public void testGetScoreFromTimeLinearPDF(){
        final List<Comparable> tsArray = new ArrayList<>();
        final DateTime now = DateTime.now();
        tsArray.add(now.getMillis());
        tsArray.add(now.plusMinutes(1).getMillis());
        tsArray.add(now.plusMinutes(2).getMillis());
        Map<Comparable, Double> rankingMap = MotionScoreAlgorithm.getRankingPositionMap(tsArray, false);

        // We should expect a linear result
        assertThat(MotionScoreAlgorithm.getScoreFromTimeLinearPDF(now.getMillis(), rankingMap),
                is(rankingMap.get(now.getMillis())));
        assertThat(MotionScoreAlgorithm.getScoreFromTimeLinearPDF(now.plusMinutes(1).getMillis(), rankingMap),
                is(rankingMap.get(now.plusMinutes(1).getMillis())));
        assertThat(MotionScoreAlgorithm.getScoreFromTimeLinearPDF(now.plusMinutes(2).getMillis(), rankingMap),
                is(rankingMap.get(now.plusMinutes(2).getMillis())));


        // Test something not exists in the ranking map
        assertThat(MotionScoreAlgorithm.getScoreFromTimeLinearPDF(0L, rankingMap), is(0d));
    }

    @Test
    public void testGetScoreFromMotionPolyPDF(){
        final List<Comparable> ampArray = new ArrayList<>();
        final Double startAmplitude = 1d;
        ampArray.add(startAmplitude);
        ampArray.add(startAmplitude + 1);
        ampArray.add(startAmplitude + 2);
        Map<Comparable, Double> rankingMap = MotionScoreAlgorithm.getRankingPositionMap(ampArray, false);

        // We should expect a linear result
        assertThat(MotionScoreAlgorithm.getScoreFromMotionPolyPDF(startAmplitude, rankingMap),
                is(Math.pow(rankingMap.get(startAmplitude), 10)));
        assertThat(MotionScoreAlgorithm.getScoreFromMotionPolyPDF(startAmplitude + 1, rankingMap),
                is(Math.pow(rankingMap.get(startAmplitude + 1), 10)));
        assertThat(MotionScoreAlgorithm.getScoreFromMotionPolyPDF(startAmplitude + 2, rankingMap),
                is(Math.pow(rankingMap.get(startAmplitude + 2), 10)));


        // Test something not exists in the ranking map
        assertThat(MotionScoreAlgorithm.getScoreFromMotionPolyPDF(0d, rankingMap), is(0d));
    }


    /*
    * This test is to make sure I made no stupid mistakes in porting algorithm from python to java
     */
    @Test
    public void testJavaCodeWorksTheSameAsPythonPrototype(){
        final MotionFixtureCSVDataSource dataSource = new MotionFixtureCSVDataSource("pang_motion_2014_12_11_gap_filled.csv");
        assertThat(dataSource.getDataForDate(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC)).size(), is(437));

        final SleepDetectionAlgorithm algorithm = new MotionScoreAlgorithm(dataSource, 10 * DateTimeConstants.MILLIS_PER_MINUTE);
        final Segment sleepSegment = algorithm.getSleepPeriod(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC), Optional.<DateTime>absent());


        // Out put from python script:
        /*
        sleep at 2014-12-12 03:03:00, prob: 0.55139287169, amp: 15103
        wake up at 2014-12-12 07:23:00, prob: 0.548869568492, amp: 1547
        */

        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime wakeUpTime = new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(sleepLocalUTC, is(new DateTime(2014, 12, 12, 3, 3, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2014, 12, 12, 7, 23, DateTimeZone.UTC)));
    }
}
