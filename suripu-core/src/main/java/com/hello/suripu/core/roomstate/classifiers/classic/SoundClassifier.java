package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import org.joda.time.DateTime;

public class SoundClassifier implements Classifier {
    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        return new State(value,"IDK", "lol", Condition.UNKNOWN, whenUTC, State.Unit.DB);
    }
}
