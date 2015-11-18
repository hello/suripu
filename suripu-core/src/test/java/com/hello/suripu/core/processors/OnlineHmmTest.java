package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.EvaluationResult;
import com.hello.suripu.core.algorithmintegration.MultiEvalHmmDecodedResult;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelLearner;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAOFromFile;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by benjo on 9/15/15.
 */
public class OnlineHmmTest {

    public final static String SEED_MODEL_RESOURCE = "fixtures/algorithm/normal3.model";
    public final static String ENSEMBLE_MODELS_RESOURCE = "fixtures/algorithm/normal3ensemble.model";
    public final static String FEATURE_EXTRACTION_RESOURCE = "fixtures/algorithm/featureextractionlayer.bin";



    public final static class LocalFeatureExtractionDAO implements FeatureExtractionModelsDAO {
        FeatureExtractionModelData deserialization = null;

        public static LocalFeatureExtractionDAO create(final String resourcePath) {
            final LocalFeatureExtractionDAO localFeatureExtractionDAO = new LocalFeatureExtractionDAO();
            localFeatureExtractionDAO.initialize(resourcePath);
            return localFeatureExtractionDAO;
        }

        private LocalFeatureExtractionDAO() {
        }

        private void initialize(final String resourcePath) {
            try {
                deserialization = new FeatureExtractionModelData(Optional.<UUID>absent());
                deserialization.deserialize(HmmUtils.loadFile(resourcePath,true));
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

    public final static class LocalOnlineHmmModelsDAO implements OnlineHmmModelsDAO  {

        public int putModelCounts = 0;
        public int putScratchpadCounts = 0;
        public int getCounts = 0;

        public static LocalOnlineHmmModelsDAO create(boolean startEmpty) {
            return new LocalOnlineHmmModelsDAO(startEmpty); //start empty

        }

        public static LocalOnlineHmmModelsDAO create() {
            return new LocalOnlineHmmModelsDAO(true); //start empty
        }


        private LocalOnlineHmmModelsDAO(boolean startEmpty) {
            priorByDate = Maps.newTreeMap();

            if (!startEmpty) {
                //get model
                try {
                    final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3.model",false);
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
            getCounts++;

            final Map.Entry<DateTime,OnlineHmmData> entry = priorByDate.floorEntry(date);

            if (entry == null) {
                return OnlineHmmData.createEmpty();
            }

            return entry.getValue();
        }

        /*
        @Override
        public boolean updateModelPriors(Long accountId, DateTime date, OnlineHmmPriors priors) {

            OnlineHmmData newOnlineHmmData = new OnlineHmmData(priors,OnlineHmmScratchPad.createEmpty());

            if (priorByDate.containsKey(date)) {
                newOnlineHmmData = new OnlineHmmData(priors,priorByDate.get(date).scratchPad);
            }

            priorByDate.put(date, newOnlineHmmData);
            return true;
        }
        */

        @Override
        public boolean updateModelPriorsAndZeroOutScratchpad(Long accountId, DateTime date, OnlineHmmPriors priors) {
            putModelCounts++;
            final OnlineHmmData newOnlineHmmData = new OnlineHmmData( priors,OnlineHmmScratchPad.createEmpty());
            priorByDate.put(date,newOnlineHmmData);
            return true;
        }

        @Override
        public boolean updateScratchpad(Long accountId, DateTime date, OnlineHmmScratchPad scratchPad) {
            putScratchpadCounts++;
            final DateTime key  =  priorByDate.floorKey(date);

            if (key != null) {
                priorByDate.put(key,new OnlineHmmData(priorByDate.get(key).modelPriors,scratchPad));
                return true;
            }

            return false;
        }

        public void setZeroCounts() {
            putModelCounts = 0;
            putScratchpadCounts = 0;
            getCounts = 0;
        }
    };

    public static AllSensorSampleList getTypicalDayOfSense(final DateTime startTime, final DateTime endTime, int tzOffset) {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final List<Sample> light = Lists.newArrayList();
        final List<Sample> soundCount = Lists.newArrayList();
        final List<Sample> waves = Lists.newArrayList();

        final long tstart = startTime.withZone(DateTimeZone.UTC).getMillis();
        final long tstop = endTime.withZone(DateTimeZone.UTC).getMillis();



        for (long t = tstart; t < tstop; t += 60000L) {
            final double percent =  ((double)(t - tstart) / (double)(tstop - tstart));

            if (percent < 0.33) {
                light.add(new Sample(t,20.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,0.0f,tzOffset));
            }
            else if (percent >= 0.33 && percent < 0.66) {
                light.add(new Sample(t,0.05f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,0.0f,tzOffset));
            }
            else {
                light.add(new Sample(t,20.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,0.0f,tzOffset));
            }
        }

        allSensorSampleList.add(Sensor.LIGHT,light);
        allSensorSampleList.add(Sensor.SOUND_NUM_DISTURBANCES,soundCount);
        allSensorSampleList.add(Sensor.WAVE_COUNT,waves);

        return allSensorSampleList;
    }

    public static AllSensorSampleList getWeirdDayOfSenseData(final DateTime startTime, final DateTime endTime, int tzOffset) {
        final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final List<Sample> light = Lists.newArrayList();
        final List<Sample> soundCount = Lists.newArrayList();
        final List<Sample> waves = Lists.newArrayList();

        final long tstart = startTime.withZone(DateTimeZone.UTC).getMillis();
        final long tstop = endTime.withZone(DateTimeZone.UTC).getMillis();



        for (long t = tstart; t < tstop; t += 60000L) {
            final double percent =  ((double)(t - tstart) / (double)(tstop - tstart));

            if (percent < 0.33) {
                light.add(new Sample(t,0.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,1.0f,tzOffset));
            }
            else if (percent >= 0.33 && percent < 0.66) {
                light.add(new Sample(t,100.0f,tzOffset));
                waves.add(new Sample(t,1.0f,tzOffset));
                soundCount.add(new Sample(t,100.0f,tzOffset));
            }
            else {
                light.add(new Sample(t,0.0f,tzOffset));
                waves.add(new Sample(t,0.0f,tzOffset));
                soundCount.add(new Sample(t,1.0f,tzOffset));
            }
        }

        allSensorSampleList.add(Sensor.LIGHT,light);
        allSensorSampleList.add(Sensor.SOUND_NUM_DISTURBANCES,soundCount);
        allSensorSampleList.add(Sensor.WAVE_COUNT,waves);

        return allSensorSampleList;
    }

    public static ImmutableList<TrackerMotion> getTypicalDayOfPill(final DateTime startTime, final DateTime endTime, int tzOffset) {
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
    public void testShortenedDay() {
        final LocalOnlineHmmModelsDAO modelsDAO = LocalOnlineHmmModelsDAO.create();
        final LocalFeatureExtractionDAO localFeatureExtractionDAO =  LocalFeatureExtractionDAO.create(FEATURE_EXTRACTION_RESOURCE);

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAOFromFile(
                HmmUtils.getPathFromResourcePath(ENSEMBLE_MODELS_RESOURCE),
                HmmUtils.getPathFromResourcePath(SEED_MODEL_RESOURCE));

        final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, localFeatureExtractionDAO, modelsDAO, Optional.<UUID>absent());

        modelsDAO.setZeroCounts();

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);


        AllSensorSampleList senseData = getTypicalDayOfSense(startTime, endTime, 0);
        ImmutableList<TrackerMotion> pillData = getTypicalDayOfPill(startTime, endTime, 0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData, pillData, ImmutableList.copyOf(Collections.EMPTY_LIST), ImmutableList.copyOf(Collections.EMPTY_LIST), 0);


        final DateTime currentTime = endTime.minusHours(7);
        final SleepEvents<Optional<Event>> sleepEvents = onlineHmm.predictAndUpdateWithLabels(0, date, startTime, endTime, currentTime, oneDaysSensorData, false, false);

        //make sure out-of-bed is before current time... remember timezone offset is zero
        TestCase.assertTrue(sleepEvents.outOfBed.get().getStartTimestamp() <= currentTime.getMillis());


    }



    @Test
    public void testNormalSequenceOfEvents() {
        final LocalOnlineHmmModelsDAO modelsDAO = LocalOnlineHmmModelsDAO.create();
        final LocalFeatureExtractionDAO localFeatureExtractionDAO =  LocalFeatureExtractionDAO.create(FEATURE_EXTRACTION_RESOURCE);

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAOFromFile(
                HmmUtils.getPathFromResourcePath(ENSEMBLE_MODELS_RESOURCE),
                HmmUtils.getPathFromResourcePath(SEED_MODEL_RESOURCE));

        final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, localFeatureExtractionDAO,modelsDAO,Optional.<UUID>absent());

        modelsDAO.setZeroCounts();

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);


        AllSensorSampleList senseData = getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = getTypicalDayOfPill(startTime,endTime,0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        ////--------------------
        //step 1) make sure we DO NOT save a model on first day
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 0);

        //okay go on and create the models
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData,true,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);



        ////--------------------
        //step 2) the second day.... with FEEDBACK!
        //but make sure it's going to adapt to the noisy day
        modelsDAO.setZeroCounts();
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getWeirdDayOfSenseData(startTime, endTime, 0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final List<TimelineFeedback> timelineFeedbacks = Lists.newArrayList();
        final TimelineFeedback feedbackForNight2 = new TimelineFeedback(null, date,"00:00","01:00", Event.Type.SLEEP,Optional.of(0L),Optional.of(endTime.getMillis()));
        final TimelineFeedback feedbackForNight2Wake = new TimelineFeedback(null, date,"00:00","6:30", Event.Type.WAKE_UP,Optional.of(0L),Optional.of(endTime.getMillis()));

        final TimelineFeedback feedbackForNight2bed = new TimelineFeedback(null, date,"00:00","23:00", Event.Type.IN_BED,Optional.of(0L),Optional.of(endTime.getMillis()));

        timelineFeedbacks.add(feedbackForNight2);
        timelineFeedbacks.add(feedbackForNight2bed);
        timelineFeedbacks.add(feedbackForNight2Wake);
        final OneDaysSensorData oneDaysSensorData2 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(timelineFeedbacks),0);

        //////////////////////////////////////
        // since there is feedback for this day,
        // I expect the scratchpad of the previous day
        // to be updated
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData2,true,false);

        //make sure that no new entries were created, and that there is a scratchpad with two entries
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.paramsByOutputId.size() == 2);
        TestCase.assertTrue(modelsDAO.putScratchpadCounts == 1);
        TestCase.assertTrue(modelsDAO.putModelCounts == 0);
        TestCase.assertTrue(modelsDAO.getCounts == 1);

        //run again, this time with no update, and make sure that no new entries are created
        modelsDAO.setZeroCounts();
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData2,false,false);
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.paramsByOutputId.size() == 2);
        TestCase.assertTrue(modelsDAO.putScratchpadCounts == 0);
        TestCase.assertTrue(modelsDAO.putModelCounts == 0);
        TestCase.assertTrue(modelsDAO.getCounts == 1);


        ////--------------------
        //step 3) the third day, make sure a new model got generated from the scratchpad
        modelsDAO.setZeroCounts();
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getTypicalDayOfSense(startTime,endTime,0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final OneDaysSensorData oneDaysSensorData3 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData3,false,false);

        //make sure the model was only put once
        TestCase.assertTrue(modelsDAO.putModelCounts == 1);

        //make sure the no scratchpads were put
        TestCase.assertTrue(modelsDAO.putScratchpadCounts == 0);

        //and make sure we did only one get
        TestCase.assertTrue(modelsDAO.getCounts == 1);

        //test that a new prior got created
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 2);

        //and it's for today
        TestCase.assertTrue(modelsDAO.priorByDate.get(date) != null);

        //and it's not empty
        TestCase.assertFalse(modelsDAO.priorByDate.get(date).modelPriors.isEmpty());

        //also test that yesterday's scratchpad is still around
        TestCase.assertFalse(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());

        //and ensure that today's scratchpad is empty
        TestCase.assertTrue(modelsDAO.priorByDate.get(date).scratchPad.isEmpty());

        //redo get, make sure we don't update the models again
        modelsDAO.setZeroCounts();
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData3,false,false);
        TestCase.assertTrue(modelsDAO.putModelCounts == 0);


        ////--------------------
        //step 4) on the fourth day of christmas, my unit-test gave to me:
        //   A day where SLEEP-0 will be selected, but we want SLEEP-2 to be produced in the scratchpad
        //   with our bug that we found, SLEEP-0 would produce a scratchpad of SLEEP-1 (just stupidly incrementing the selected model, not the entire model set)
        modelsDAO.setZeroCounts();
        date = date.plusDays(1);
        startTime = startTime.plusDays(1);
        endTime = endTime.plusDays(1);
        senseData = getTypicalDayOfSense(startTime, endTime, 0);
        pillData = getTypicalDayOfPill(startTime,endTime,0);
        final TimelineFeedback feedbackForNight4 = new TimelineFeedback(null, date,"00:00","23:00", Event.Type.SLEEP,Optional.of(0L),Optional.of(endTime.getMillis()));

        final OneDaysSensorData oneDaysSensorData4 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Lists.newArrayList(feedbackForNight4)),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData4,true,false);

        final String scratchpadId = modelsDAO.priorByDate.lastEntry().getValue().scratchPad.paramsByOutputId.get("SLEEP").id;

        TestCase.assertEquals("SLEEP-0-custom",scratchpadId);
    }


    @Test
    public void testDelayedFeedback() {
        final LocalOnlineHmmModelsDAO modelsDAO = LocalOnlineHmmModelsDAO.create();
        final LocalFeatureExtractionDAO localFeatureExtractionDAO =  LocalFeatureExtractionDAO.create(FEATURE_EXTRACTION_RESOURCE);

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAOFromFile(
                HmmUtils.getPathFromResourcePath(ENSEMBLE_MODELS_RESOURCE),
                HmmUtils.getPathFromResourcePath(SEED_MODEL_RESOURCE));

        final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, localFeatureExtractionDAO,modelsDAO,Optional.<UUID>absent());

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);


        AllSensorSampleList senseData = getTypicalDayOfSense(startTime,endTime,0);
        ImmutableList<TrackerMotion> pillData = getTypicalDayOfPill(startTime,endTime,0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),0);

        ////--------------------
        //step 1) make sure we save off default model on first day
        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData,true,false);
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
        final TimelineFeedback feedbackForNight2 = new TimelineFeedback(0L, date,"00:00","23:00", Event.Type.SLEEP,Optional.of(0L),Optional.of(endTime.plusDays(1).getMillis()));
        timelineFeedbacks.add(feedbackForNight2);
        final OneDaysSensorData oneDaysSensorData2 = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(timelineFeedbacks),0);

        onlineHmm.predictAndUpdateWithLabels(0, date,startTime,endTime,endTime,oneDaysSensorData2,true,false);

        //make sure that no new entries were created, and that there is a scratchpad
        TestCase.assertTrue(modelsDAO.priorByDate.size() == 1);
        TestCase.assertTrue(modelsDAO.priorByDate.firstEntry().getValue().scratchPad.isEmpty());



    }

    @Test
    public void testLabelArrayBounds() {

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAOFromFile(
                HmmUtils.getPathFromResourcePath(ENSEMBLE_MODELS_RESOURCE),
                HmmUtils.getPathFromResourcePath(SEED_MODEL_RESOURCE));

        final Multimap<String, MultiEvalHmmDecodedResult> modelEvaluations = ArrayListMultimap.create();
        final Map<String, MultiEvalHmmDecodedResult> predictions = Maps.newHashMap();

        final double pathcost = 99.99;
        final int [] path = {0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2};
        modelEvaluations.put("foo",new MultiEvalHmmDecodedResult(path,pathcost,"foobars"));

        final EvaluationResult evaluationResult = new EvaluationResult(modelEvaluations,predictions);

        final OnlineHmmModelLearner learner = new OnlineHmmModelLearner(Optional.<UUID>absent());


        final Map<String,Map<Integer,Integer>> labels = Maps.newHashMap();
        final Map<Integer,Integer> exceedTimeBoundsLabel = Maps.newHashMap();

        exceedTimeBoundsLabel.put(-1,0);
        exceedTimeBoundsLabel.put(1,0);
        exceedTimeBoundsLabel.put(2,3);
        exceedTimeBoundsLabel.put(3,-2);
        exceedTimeBoundsLabel.put(4,0);
        exceedTimeBoundsLabel.put(path.length + 100,0);

        labels.put("foo",exceedTimeBoundsLabel);

        //the test is that we do not get an exception
        learner.updateModelWeights(evaluationResult,defaultModelEnsembleDAO.getDefaultModelEnsemble(),labels);



    }

}
