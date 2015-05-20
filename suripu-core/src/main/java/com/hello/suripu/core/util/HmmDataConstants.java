package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 2/22/15.
 */
public class HmmDataConstants {

    /*  Sensor data indices */
    final static public int NUM_DATA_DIMENSIONS = 7;
    final static public int LIGHT_INDEX = 0;
    final static public int MOT_COUNT_INDEX = 1;
    final static public int DISTURBANCE_INDEX = 2;
    final static public int LOG_SOUND_COUNT_INDEX = 3;
    final static public int NATURAL_LIGHT_FILTER_INDEX = 4;
    final static public int PARTNER_MOT_COUNT_INDEX = 5;
    final static public int PARTNER_DISTURBANCE_INDEX = 6;



    /* Sleep depth constants  */
    final static public int SLEEP_DEPTH_NONE = 0;
    final static public int SLEEP_DEPTH_LIGHT = 66;
    final static public int SLEEP_DEPTH_REGULAR = 100;
    final static public int SLEEP_DEPTH_DISTURBED = 33;


    /* Misc */
    final static public int HEARTBEAT_VALUE = -1; //value for pilldata that is a heartbeat, not an actual measurement

    /*  some defaults in case of protobuf fail, or a constant is not included  */
    final static public int NUM_MODEL_PARAMS = 50; //probably on the low side now, but oh well.  size of A matrix + num free observation params
    final static public double AUDIO_DISTURBANCE_THRESHOLD_DB = 60.0;
    final static public double PILL_MAGNITUDE_DISTURBANCE_THRESHOLD = 12000;
    final static public double NATURAL_LIGHT_FILTER_START_HOUR = 16.0; //24 HOURS FORMAT
    final static public double NATURAL_LIGHT_FILTER_STOP_HOUR = 4.0; //4am
    final static public int NUM_MINUTES_IN_MEAS_PERIOD = 15;
    final static public boolean DEFAULT_IS_USING_INTERVAL_SEARCH = true;
    final static public double DEFAULT_LIGHT_PRE_MULTIPLIER = 4.0;
    final static public double DEFAULT_LIGHT_FLOOR_LUX = 0.0; //lux
    final static public boolean DEFAULT_USE_WAVE_COUNTS_FOR_DISTURBANCES = true;
    final static public double DEFAULT_AUDIO_ABOVE_BACKGROUND_THRESHOLD_DB = 0.0;

}
