import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.InternalScore;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import com.hello.suripu.algorithm.sleep.SleepDetectionAlgorithm;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.SleepDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.SleepTimeScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.WakeUpTimeScoringFunction;
import com.hello.suripu.algorithm.utils.MaxAmplitudeAggregator;
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
    public void testLinearScoringFunction(){
        final List<Long> tsArray = new ArrayList<>();
        final DateTime now = DateTime.now();
        tsArray.add(now.getMillis());
        tsArray.add(now.plusMinutes(1).getMillis());
        tsArray.add(now.plusMinutes(2).getMillis());
        final WakeUpTimeScoringFunction wakeUpTimeScoringFunction = new WakeUpTimeScoringFunction(0);

        Map<Long, Double> rankingMap = wakeUpTimeScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(0d));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(2d / tsArray.size()));
        assertThat(wakeUpTimeScoringFunction.getScore(0L, rankingMap), is(0d));

        // Test order by DESC
        final SleepTimeScoringFunction sleepTimeScoringFunction = new SleepTimeScoringFunction();
        rankingMap = sleepTimeScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(2d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(0d / tsArray.size()));

        assertThat(sleepTimeScoringFunction.getScore(0L, rankingMap), is(0d));

    }

    @Test
    public void testGetHighestScore(){
        final ArrayList<InternalScore> scores = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            scores.add(new InternalScore(i, Double.valueOf(i)));
        }

        final Optional<InternalScore> highestScore = MotionScoreAlgorithm.getHighestScore(scores);
        assertThat(highestScore.isPresent(), is(true));
        assertThat(highestScore.get().score, is(Double.valueOf(scores.size() - 1)));


        final ArrayList<InternalScore> emptyScores = new ArrayList<>();
        assertThat(MotionScoreAlgorithm.getHighestScore(emptyScores).isPresent(), is(false));
    }


    @Test
    public void testGetScoreFromMotionPolyPDF(){
        final List<Double> ampArray = new ArrayList<>();
        final Double startAmplitude = 1d;
        ampArray.add(startAmplitude);
        ampArray.add(startAmplitude + 1);
        ampArray.add(startAmplitude + 2);

        final MotionScoringFunction motionScoringFunction = new MotionScoringFunction(10);
        Map<Double, Double> pdf = motionScoringFunction.getPDF(ampArray);

        // We should expect a linear result
        assertThat(motionScoringFunction.getScore(startAmplitude, pdf),
                is(Math.pow(0, 10)));
        assertThat(motionScoringFunction.getScore(startAmplitude + 1, pdf),
                is(Math.pow(1d / 3, 10)));
        assertThat(motionScoringFunction.getScore(startAmplitude + 2, pdf),
                is(Math.pow(2d / 3, 10)));


        // Test something not exists in the ranking map
        assertThat(motionScoringFunction.getScore(0d, pdf), is(0d));
    }


    /*
    * This test is to make sure I made no stupid mistakes in porting algorithm from python to java
     */
    @Test
    public void testJavaCodeWorksTheSameAsPythonPrototype(){
        final MotionFixtureCSVDataSource dataSource = new MotionFixtureCSVDataSource("pang_motion_2014_12_11_gap_filled.csv");
        assertThat(dataSource.getDataForDate(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC)).size(), is(437));

        final AmplitudeDataPreprocessor smoother = new MaxAmplitudeAggregator(10 * DateTimeConstants.MILLIS_PER_MINUTE);
        final List<AmplitudeData> smoothedData = smoother.process(dataSource.getDataForDate(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC)));

        final ArrayList<SleepDataScoringFunction> scoringFunctions = new ArrayList<>();
        scoringFunctions.add(new AmplitudeDataScoringFunction(10, 0.5));

        final SleepDetectionAlgorithm algorithm = new MotionScoreAlgorithm(MotionScoreAlgorithm.getMatrix(smoothedData), 1, smoothedData.size(), scoringFunctions);
        final Segment sleepSegment = algorithm.getSleepPeriod(new DateTime(2014, 12, 11, 0, 0, DateTimeZone.UTC));


        // Out put from python script:
        /*
        sleep at 2014-12-12 03:03:00, prob: 0.55139287169, amp: 15103
        wake up at 2014-12-12 07:23:00, prob: 0.479932601157, amp: 1547
        */

        final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime wakeUpTime = new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime wakeUpLocalUTC = new DateTime(wakeUpTime.getYear(), wakeUpTime.getMonthOfYear(), wakeUpTime.getDayOfMonth(), wakeUpTime.getHourOfDay(), wakeUpTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(sleepLocalUTC, is(new DateTime(2014, 12, 12, 3, 3, DateTimeZone.UTC)));
        assertThat(wakeUpLocalUTC, is(new DateTime(2014, 12, 12, 7, 23, DateTimeZone.UTC)));
    }
}
