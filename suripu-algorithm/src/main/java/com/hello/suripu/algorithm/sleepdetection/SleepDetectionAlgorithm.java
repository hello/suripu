package com.hello.suripu.algorithm.sleepdetection;

import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;

/**
 * Created by pangwu on 6/11/14.
 */
public interface SleepDetectionAlgorithm {
    Segment getSleepPeriod(final DataSource<AmplitudeData> dataSource, final DateTime dateOfTheNight) throws AlgorithmException;
}
