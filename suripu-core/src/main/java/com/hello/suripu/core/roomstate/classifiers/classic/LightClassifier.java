package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;

public class LightClassifier implements Classifier {
    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        Condition condition = Condition.IDEAL;;
        String idealConditions = English.LIGHT_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_LIGHT_PRE_SLEEP_MESSAGE: English.IDEAL_LIGHT_MESSAGE;;

        if (value >  Lights.LIGHT_LEVEL_ALERT) {
            condition = Condition.ALERT;
            idealConditions += English.RECOMMENDATION_LIGHT_TOO_HIGH;
            message = (preSleep) ? English.ALERT_LIGHT_PRE_SLEEP_MESSAGE : English.ALERT_LIGHT_MESSAGE;

        } else if (value > Lights.LIGHT_LEVEL_WARNING) {
            condition = Condition.WARNING;
            idealConditions += English.RECOMMENDATION_LIGHT_TOO_HIGH;
            message = (preSleep) ? English.WARNING_LIGHT_PRE_SLEEP_MESSAGE: English.WARNING_LIGHT_MESSAGE;
        }

        return new State(value, message, idealConditions, condition, whenUTC, State.Unit.LUX);
    }
}
