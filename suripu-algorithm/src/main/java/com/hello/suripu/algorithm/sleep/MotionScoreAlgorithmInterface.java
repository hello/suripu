package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.Segment;

import java.util.List;

/**
 * Created by benjo on 2/13/15.
 */
public interface MotionScoreAlgorithmInterface {
    List<Segment> getSleepEvents(boolean debugMode) throws AlgorithmException;
}
