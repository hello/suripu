package com.hello.suripu.core.roomstate.classifiers;

import com.hello.suripu.core.roomstate.State;
import org.joda.time.DateTime;

public interface Classifier {

    State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit);
}
