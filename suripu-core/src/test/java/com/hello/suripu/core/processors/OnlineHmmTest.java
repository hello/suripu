package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by benjo on 9/15/15.
 */
public class OnlineHmmTest {

    final static class LocalFeatureExtractionDAO implements FeatureExtractionModelsDAO {
        FeatureExtractionModelData deserialization = null;

        public LocalFeatureExtractionDAO() {
            try {
                deserialization = new FeatureExtractionModelData(Optional.<UUID>absent());
                deserialization.deserialize(HmmUtils.loadFile("fixtures/algorithm/featureextractionlayer.bin"));
            }
            catch (IOException exception) {
                TestCase.assertTrue(false);
                deserialization = null;
            }
        }

        @Override
        public FeatureExtractionModelData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, Optional<UUID> uuidForLogger) {
            return deserialization;
        }
    };

    final static class LocalOnlineHmmModelsDAO implements OnlineHmmModelsDAO  {

        public LocalOnlineHmmModelsDAO(boolean startEmpty) {
            priorByDate = Maps.newTreeMap();

            if (!startEmpty) {
                //get model
                try {
                    final byte[] protobuf = HmmUtils.loadFile("fixtures/algorithm/default_model.bin");
                    final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);

                    TestCase.assertTrue(model.isPresent());


                    final DateTime dateTime = new DateTime(0);

                    priorByDate.put(dateTime, new OnlineHmmData(model.get(), OnlineHmmScratchPad.createEmpty()));

                } catch (IOException exception) {
                    TestCase.assertTrue(false);
                }
            }


        }

        public final TreeMap<DateTime, OnlineHmmData> priorByDate;

        @Override
        public OnlineHmmData getModelDataByAccountId(Long accountId, DateTime date) {
            final Map.Entry<DateTime,OnlineHmmData> entry = priorByDate.floorEntry(date);

            if (entry == null) {
                return OnlineHmmData.createEmpty();
            }

            return entry.getValue();
        }

        @Override
        public boolean updateModelPriors(Long accountId, DateTime date, OnlineHmmPriors priors) {

            OnlineHmmData newOnlineHmmData = new OnlineHmmData(priors,OnlineHmmScratchPad.createEmpty());

            if (priorByDate.containsKey(date)) {
                newOnlineHmmData = new OnlineHmmData(priors,priorByDate.get(date).scratchPad);
            }

            priorByDate.put(date, newOnlineHmmData);
            return true;
        }

        @Override
        public boolean updateModelPriorsAndZeroOutScratchpad(Long accountId, DateTime date, OnlineHmmPriors priors) {
            final OnlineHmmData newOnlineHmmData = new OnlineHmmData( priors,OnlineHmmScratchPad.createEmpty());
            priorByDate.put(date,newOnlineHmmData);
            return true;
        }

        @Override
        public boolean updateScratchpad(Long accountId, DateTime date, OnlineHmmScratchPad scratchPad) {

            final DateTime key  =  priorByDate.floorKey(date);

            if (key != null) {
                priorByDate.put(key,new OnlineHmmData(priorByDate.get(key).modelPriors,scratchPad));
                return true;
            }

            return false;
        }
    };

    static AllSensorSampleList getTypicalDayOfSense(final DateTime startTime, final DateTime endTime, int tzOffset) {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final List<Sample> light = Lists.newArrayList();
        final List<Sample> soundCount = Lists.newArrayList();
        final List<Sample> waves = Lists.newArrayList();

        final long tstart = startTime.withZone(DateTimeZone.UTC).getMillis();
        final long tstop = endTime.withZone(DateTimeZone.UTC).getMillis();



        for (long t = tstart; t < tstop; t += 60000L) {
            final double percent =  ((double)(t - tstart) / (double)(tstop - tstart));

            if (percent < 0.33) {
                light.add(new Sample(t,50.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,1.0f,tzOffset));
            }
            else if (percent >= 0.33 && percent < 0.66) {
                light.add(new Sample(t,0.05f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,0.0f,tzOffset));
            }
            else {
                light.add(new Sample(t,50.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,1.0f,tzOffset));
            }
        }

        allSensorSampleList.add(Sensor.LIGHT,light);
        allSensorSampleList.add(Sensor.SOUND_NUM_DISTURBANCES,soundCount);
        allSensorSampleList.add(Sensor.WAVE_COUNT,waves);

        return allSensorSampleList;
    }

    static ImmutableList<TrackerMotion> getTypicalDayOfPill(final DateTime startTime, final DateTime endTime, int tzOffset) {
        final List<TrackerMotion> trackerMotions = Lists.newArrayList();
        final long tstart = startTime.withZone(DateTimeZone.UTC).getMillis();
        final long tstop = endTime.withZone(DateTimeZone.UTC).getMillis();

        for (long t = tstart; t < tstop; t += 60000L) {
            final double percent = ((double) (t - tstart) / (double) (tstop - tstart));

            if (percent < 0.39 && percent > 0.25) {
                if (t % 60000L * 30L == 0) {
                    trackerMotions.add(new TrackerMotion(0, 0, 0L, t, 5000, tzOffset, 5000L, 1L, 2L));
                }
            }
            else if (percent >= 0.33 && percent < 0.66) {
                if (t % 60000L * 10L == 0) {
                    trackerMotions.add(new TrackerMotion(0, 0, 0L, t, 5000, tzOffset, 5000L, 1L, 1L));
                }
            }
            else{

            }

        }

        return ImmutableList.copyOf(trackerMotions);

    }

    @Test
    public void testNormalSequenceOfEvents() {
        final LocalOnlineHmmModelsDAO modelsDAO = new LocalOnlineHmmModelsDAO(true);
        final LocalFeatureExtractionDAO localFeatureExtractionDAO = new LocalFeatureExtractionDAO();


        final OnlineHmm onlineHmm = new OnlineHmm(localFeatureExtractionDAO,modelsDAO,Optional.<UUID>absent());

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(18);


        AllSensorSampleList senseData = getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = getTypicalDayOfPill(startTime,endTime,0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        ////--------------------
        //step 1) make sure we save off default model on first day
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());

        //make sure evaluating again doesn't screw things up
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());



        ////--------------------
        //step 2) the second day.... with FEEDBACK!
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getTypicalDayOfSense(startTime,endTime,0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final List<TimelineFeedback> timelineFeedbacks = Lists.newArrayList();
        final TimelineFeedback feedbackForNight2 = new TimelineFeedback(date,"00:00","23:00", Event.Type.SLEEP,Optional.of(0L),Optional.of(endTime.getMillis()));
        timelineFeedbacks.add(feedbackForNight2);
        final OneDaysSensorData oneDaysSensorData2 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(timelineFeedbacks),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData2,true,false);

        //make sure that no new entries were created, and that there is a scratchpad
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertFalse(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());


        //run again, this time with no update, and make sure that no new entries are created
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData2,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertFalse(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());



        ////--------------------
        //step 3) the third day, make sure a new model got generated from the scratchpad
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getTypicalDayOfSense(startTime,endTime,0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final OneDaysSensorData oneDaysSensorData3 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData3,false,false);

        TestCase.assertTrue(modelsDAO.priorByDate.size() == 2);

    }


    @Test
    public void testDelayedFeedback() {
        final LocalOnlineHmmModelsDAO modelsDAO = new LocalOnlineHmmModelsDAO(true);
        final LocalFeatureExtractionDAO localFeatureExtractionDAO = new LocalFeatureExtractionDAO();


        final OnlineHmm onlineHmm = new OnlineHmm(localFeatureExtractionDAO,modelsDAO,Optional.<UUID>absent());

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(18);


        AllSensorSampleList senseData = getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = getTypicalDayOfPill(startTime,endTime,0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        ////--------------------
        //step 1) make sure we save off default model on first day
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());



        ////--------------------
        //step 2) the second day.... with FEEDBACK, but feedback from one day after the timeline!
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getTypicalDayOfSense(startTime,endTime,0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final List<TimelineFeedback> timelineFeedbacks = Lists.newArrayList();
        final TimelineFeedback feedbackForNight2 = new TimelineFeedback(date,"00:00","23:00", Event.Type.SLEEP,Optional.of(0L),Optional.of(endTime.plusDays(1).getMillis()));
        timelineFeedbacks.add(feedbackForNight2);
        final OneDaysSensorData oneDaysSensorData2 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(timelineFeedbacks),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,oneDaysSensorData2,true,false);

        //make sure that no new entries were created, and that there is a scratchpad
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());



    }

}
