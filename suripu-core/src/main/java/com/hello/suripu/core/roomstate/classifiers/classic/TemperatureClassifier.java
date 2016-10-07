package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;

public class TemperatureClassifier implements Classifier {

    public final static String DEFAULT_TEMP_UNIT = "c";

    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        String idealTempConditions;
        if (unit.equals(DEFAULT_TEMP_UNIT)) {
            idealTempConditions = String.format(English.TEMPERATURE_ADVICE_MESSAGE_C,
                    TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS, TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS);
        } else {
            idealTempConditions = String.format(English.TEMPERATURE_ADVICE_MESSAGE_F,
                    TemperatureHumidity.IDEAL_TEMP_MIN, TemperatureHumidity.IDEAL_TEMP_MAX);

        }

        Condition condition = Condition.IDEAL;
        String message = (preSleep) ? English.IDEAL_TEMPERATURE_PRE_SLEEP_MESSAGE: English.IDEAL_TEMPERATURE_MESSAGE;

        if (value > (float) TemperatureHumidity.ALERT_TEMP_MAX_CELSIUS) {
            condition = Condition.ALERT;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_HIGH;
            message = (preSleep) ? English.HIGH_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE: English.HIGH_TEMPERATURE_ALERT_MESSAGE;

        } else if (value > (float) TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS) {
            condition = Condition.WARNING;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_HIGH;
            message = (preSleep) ? English.HIGH_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE: English.HIGH_TEMPERATURE_WARNING_MESSAGE;

        } else if (value  < (float) TemperatureHumidity.ALERT_TEMP_MIN_CELSIUS) {
            condition = Condition.ALERT;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_LOW;
            message = (preSleep) ? English.LOW_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE: English.LOW_TEMPERATURE_ALERT_MESSAGE;

        } else if (value < (float) TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS) {
            condition = Condition.WARNING;
            idealTempConditions += English.RECOMMENDATION_TEMP_TOO_LOW;
            message = (preSleep) ? English.LOW_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE: English.LOW_TEMPERATURE_WARNING_MESSAGE;
        }

        return new State(value, message, idealTempConditions, condition, whenUTC, State.Unit.CELCIUS);
    }
}
