package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;

public class HumidityClassifier implements Classifier {
    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        Condition condition = Condition.IDEAL;;
        String idealHumidityConditions = English.HUMIDITY_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_HUMIDITY_PRE_SLEEP_MESSAGE: English.IDEAL_HUMIDITY_MESSAGE;

        if (value < (float) TemperatureHumidity.ALERT_HUMIDITY_LOW) {
            condition = Condition.ALERT;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_LOW;
            message = (preSleep) ? English.LOW_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE : English.LOW_HUMIDITY_ALERT_MESSAGE;

        } else if (value  < (float) TemperatureHumidity.IDEAL_HUMIDITY_MIN) {
            condition = Condition.WARNING;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_LOW;
            message = (preSleep) ? English.LOW_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE : English.LOW_HUMIDITY_WARNING_MESSAGE;

        } else if (value > (float) TemperatureHumidity.IDEAL_HUMIDITY_MAX) {
            condition = Condition.WARNING;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_HIGH;
            message = (preSleep) ? English.HIGH_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE : English.HIGH_HUMIDITY_WARNING_MESSAGE;

        } else if (value > (float) TemperatureHumidity.ALERT_HUMIDITY_HIGH) {
            condition = Condition.ALERT;
            idealHumidityConditions += English.RECOMMENDATION_HUMIDITY_TOO_HIGH;
            message = (preSleep) ? English.HIGH_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE : English.HIGH_HUMIDITY_ALERT_MESSAGE;
        }

        return new State(value, message, idealHumidityConditions, condition, whenUTC, State.Unit.PERCENT);

    }
}
