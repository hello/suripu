package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAOFromFile;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import com.hello.suripu.core.processors.OnlineHmmTest.*;


/**
 * Created by benjo on 11/4/15.
 */
public class OnlineHmmTortureTest {

    public final static String SEED_MODEL_RESOURCE_OLD = "fixtures/algorithm/normal3.model";
    public final static String ENSEMBLE_MODELS_RESOURCE_OLD = "fixtures/algorithm/normal3ensemble.model";
    public final static String FEATURE_EXTRACTION_RESOURCE_NEW = "fixtures/algorithm/featureextractionnew.bin";

    @Test
    public void testWithMisMatchedFeaturesAndModels() {

        final LocalOnlineHmmModelsDAO modelsDAO = LocalOnlineHmmModelsDAO.create();
        final LocalFeatureExtractionDAO localFeatureExtractionDAO =  LocalFeatureExtractionDAO.create(FEATURE_EXTRACTION_RESOURCE_NEW);

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAOFromFile(
                HmmUtils.getPathFromResourcePath(ENSEMBLE_MODELS_RESOURCE_OLD),
                HmmUtils.getPathFromResourcePath(SEED_MODEL_RESOURCE_OLD));

        final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, localFeatureExtractionDAO,modelsDAO, Optional.<UUID>absent());

        modelsDAO.setZeroCounts();

        DateTime date = DateTimeUtil.ymdStringToDateTime("2015-09-01");
        DateTime startTime = date.withHourOfDay(18);
        DateTime endTime = startTime.plusHours(16);


        final AllSensorSampleList senseData = OnlineHmmTest.getTypicalDayOfSense(startTime, endTime, 0);
        final ImmutableList<TrackerMotion> pillData = OnlineHmmTest.getTypicalDayOfPill(startTime, endTime, 0);

        final OneDaysSensorData oneDaysSensorData = new OneDaysSensorData(senseData,pillData,ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST),date,startTime,endTime,endTime,0);

        ////--------------------
        //step 1) make sure we save off default model on first day
        SleepEvents<Optional<Event>> sleepEvents = onlineHmm.predictAndUpdateWithLabels(0, date, startTime, endTime, endTime, oneDaysSensorData, false, false);

        Iterator<Optional<Event>> it  = sleepEvents.toList().iterator();

        while (it.hasNext()) {
            final Optional<Event> eventOptional = it.next();

            TestCase.assertFalse(eventOptional.isPresent());
        }

    }
}
