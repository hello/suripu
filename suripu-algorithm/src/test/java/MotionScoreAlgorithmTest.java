import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import com.hello.suripu.algorithm.pdf.RankPowerScoringFunction;
import com.hello.suripu.algorithm.sleep.InternalScore;
import com.hello.suripu.algorithm.sleep.MotionScoreAlgorithm;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.SleepDataScoringFunction;
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
        final LinearRankAscendingScoringFunction linearRankAscendingScoringFunction = new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0d, 1d});

        Map<Long, Double> rankingMap = linearRankAscendingScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(0d));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(1d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(2d / tsArray.size()));
        assertThat(linearRankAscendingScoringFunction.getScore(0L, rankingMap), is(0d));

        // Test order by DESC
        final LinearRankDescendingScoringFunction linearRankDescendingScoringFunction = new LinearRankDescendingScoringFunction(1d, 0, new double[]{0d, 1d});
        rankingMap = linearRankDescendingScoringFunction.getPDF(tsArray);
        assertThat(rankingMap.size(), is(3));
        assertThat(rankingMap.get(now.getMillis()), is(3d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(1).getMillis()), is(2d / tsArray.size()));
        assertThat(rankingMap.get(now.plusMinutes(2).getMillis()), is(1d / tsArray.size()));

        assertThat(linearRankDescendingScoringFunction.getScore(0L, rankingMap), is(0d));

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

        final RankPowerScoringFunction rankPowerScoringFunction = new RankPowerScoringFunction(10);
        Map<Double, Double> pdf = rankPowerScoringFunction.getPDF(ampArray);

        // We should expect a linear result
        assertThat(rankPowerScoringFunction.getScore(startAmplitude, pdf),
                is(Math.pow(0, 10)));
        assertThat(rankPowerScoringFunction.getScore(startAmplitude + 1, pdf),
                is(Math.pow(1d / 3, 10)));
        assertThat(rankPowerScoringFunction.getScore(startAmplitude + 2, pdf),
                is(Math.pow(2d / 3, 10)));


        // Test something not exists in the ranking map
        assertThat(rankPowerScoringFunction.getScore(0d, pdf), is(0d));
    }


    /*
    * This test is to make sure I made no stupid mistakes in porting algorithm from python to java
     */
    @Test
    public void testJavaCodeWorksTheSameAsPythonPrototype(){
        // first light out: 1417598760000
        final DateTime firstLightOutTime = new DateTime(1417598760000L, DateTimeZone.UTC);
        final MotionFixtureCSVDataSource dataSource = new MotionFixtureCSVDataSource("pang_motion_2014_12_02_gap_filled.csv");
        // Raw data count 791
        assertThat(dataSource.getDataForDate(new DateTime(2014, 12, 02, 0, 0, DateTimeZone.UTC)).size(), is(791));

        final AmplitudeDataPreprocessor smoother = new MaxAmplitudeAggregator(10 * DateTimeConstants.MILLIS_PER_MINUTE);
        final List<AmplitudeData> smoothedData = smoother.process(dataSource.getDataForDate(new DateTime(2014, 12, 02, 0, 0, DateTimeZone.UTC)));

        final ArrayList<SleepDataScoringFunction> scoringFunctions = new ArrayList<>();
        scoringFunctions.add(new AmplitudeDataScoringFunction(10));
        scoringFunctions.add(new LightOutScoringFunction(firstLightOutTime, 3d));

        final Map<Long, List<AmplitudeData>> matrix = MotionScoreAlgorithm.createFeatureMatrix(smoothedData);
        for(final Long timestamp:matrix.keySet()){
            final List<AmplitudeData> dataVector = matrix.get(timestamp);
            if(timestamp < firstLightOutTime.getMillis()){
                dataVector.add(new AmplitudeData(timestamp, 0d, dataVector.get(0).offsetMillis));
            }else{
                dataVector.add(new AmplitudeData(timestamp, 1d, dataVector.get(0).offsetMillis));
            }
        }
        final MotionScoreAlgorithm algorithm = new MotionScoreAlgorithm(matrix, 2, smoothedData.size(), scoringFunctions);
        final List<Segment> sleepSegments = algorithm.getSleepEvents();
        //final Segment sleepSegment = sleepSegments.get(1);
        final Segment goToBedSegment = sleepSegments.get(0);
        final Segment outOfBedSegment = sleepSegments.get(3);


        // Out put from python script suripu_light_test.py:
        /*
        sleep at 2014-12-03 01:39:00, prob: 1.06747942852, amp: 5471
        wake up at 2014-12-03 07:09:00, prob: 0.0924221378596, amp: 518
        */

        final DateTime goToBedTime = new DateTime(goToBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBedSegment.getOffsetMillis()));
        //final DateTime sleepTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis()));
        final DateTime outOfBedTime = new DateTime(outOfBedSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBedSegment.getOffsetMillis()));

        final DateTime goToBedLocalUTC = new DateTime(goToBedTime.getYear(), goToBedTime.getMonthOfYear(), goToBedTime.getDayOfMonth(), goToBedTime.getHourOfDay(), goToBedTime.getMinuteOfHour(), DateTimeZone.UTC);
        //final DateTime sleepLocalUTC = new DateTime(sleepTime.getYear(), sleepTime.getMonthOfYear(), sleepTime.getDayOfMonth(), sleepTime.getHourOfDay(), sleepTime.getMinuteOfHour(), DateTimeZone.UTC);
        final DateTime outOfBedLocalUTC = new DateTime(outOfBedTime.getYear(), outOfBedTime.getMonthOfYear(), outOfBedTime.getDayOfMonth(), outOfBedTime.getHourOfDay(), outOfBedTime.getMinuteOfHour(), DateTimeZone.UTC);

        assertThat(goToBedLocalUTC, is(new DateTime(2014, 12, 03, 1, 39, DateTimeZone.UTC)));
        assertThat(outOfBedLocalUTC, is(new DateTime(2014, 12, 03, 7, 9, DateTimeZone.UTC)));
    }
}
