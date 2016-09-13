package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;

public class ParticulatesClassifier implements Classifier {
    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        // see http://www.sparetheair.com/aqi.cfm

        String idealParticulatesConditions = English.PARTICULATES_ADVICE_MESSAGE;
        Condition condition = Condition.ALERT;
        String message = (preSleep) ? English.VERY_HIGH_PARTICULATES_PRE_SLEEP_MESSAGE: English.VERY_HIGH_PARTICULATES_MESSAGE;;

        if (value <= Particulates.PARTICULATE_DENSITY_MAX_IDEAL) {
            condition = Condition.IDEAL;
            message = (preSleep) ? English.IDEAL_PARTICULATES_PRE_SLEEP_MESSAGE : English.IDEAL_PARTICULATES_MESSAGE;
        } else if (value <= Particulates.PARTICULATE_DENSITY_MAX_WARNING) {
            condition = Condition.WARNING;
            message = (preSleep) ? English.HIGH_PARTICULATES_PRE_SLEEP_MESSAGE : English.HIGH_PARTICULATES_MESSAGE;
        }


        if (condition != Condition.IDEAL && !preSleep) {
            idealParticulatesConditions += English.RECOMMENDATION_PARTICULATES_TOO_HIGH;
        }

        return new State(value, message, idealParticulatesConditions, condition, whenUTC, State.Unit.AQI); // this should be State.Unit.MICRO_G_M3 but clients rely on string AQI
    }
}
