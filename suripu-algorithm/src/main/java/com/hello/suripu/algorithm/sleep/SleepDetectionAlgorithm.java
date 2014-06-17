package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;

/**
 * Created by pangwu on 6/11/14.
 */
public abstract class SleepDetectionAlgorithm {

    private final DataSource<AmplitudeData> dataSource;
    private final int smoothWindow;

    public SleepDetectionAlgorithm(final DataSource<AmplitudeData> dataSource, final int smoothWindow){
        this.dataSource = dataSource;
        this.smoothWindow = smoothWindow;
    }

    protected DataSource<AmplitudeData> getDataSource(){
        return this.dataSource;
    }

    protected int getSmoothWindow(){
        return this.smoothWindow;
    }

    public abstract Segment getSleepPeriod(final DateTime dateOfTheNight) throws AlgorithmException;
}
