package com.hello.suripu.core.roomstate.classifiers.classic;

import com.hello.suripu.core.processors.insights.SoundLevel;
import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.State;
import com.hello.suripu.core.roomstate.classifiers.Classifier;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;

public class SoundClassifier implements Classifier {
    @Override
    public State classify(Float value, DateTime whenUTC, Boolean preSleep, String unit) {
        // see http://www.noisehelp.com/noise-level-chart.html

        Condition condition = Condition.IDEAL;;
        String idealSoundCondition = English.SOUND_ADVICE_MESSAGE;
        String message = (preSleep) ? English.IDEAL_SOUND_PRE_SLEEP_MESSAGE: English.IDEAL_SOUND_MESSAGE;;

        if (value > SoundLevel.SOUND_LEVEL_ALERT) {
            // lawn mower
            condition = Condition.ALERT;
            idealSoundCondition += English.RECOMMENDATION_SOUND_TOO_HIGH;
            message = (preSleep) ? English.ALERT_SOUND_PRE_SLEEP_MESSAGE : English.ALERT_SOUND_MESSAGE;

        } else if (value > SoundLevel.SOUND_LEVEL_WARNING) {
            condition = Condition.WARNING;
            idealSoundCondition += English.RECOMMENDATION_SOUND_TOO_HIGH;
            message = (preSleep) ? English.WARNING_SOUND_PRE_SLEEP_MESSAGE: English.WARNING_SOUND_MESSAGE;
        }

        return new State(value, message, idealSoundCondition, condition, whenUTC, State.Unit.DB);
    }
}
