package com.hello.suripu.core.models;

import com.hello.suripu.core.models.Insights.Message.TemperatureMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.preferences.TemperatureUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 1/26/15.
 */
public class TemperatureMsgTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TemperatureMsgTest.class);

    @Test
    public void testMessages() {
        final String message;
        final String unit = TemperatureUnit.FAHRENHEIT.toString();

        Boolean contains;

        message = TemperatureMsgEN.getCommonMsg(18, 20, unit);
        contains = message.contains(unit);

        LOGGER.debug("common msg: {}", message);
        assertThat(contains, is(true));

        Text text;
        text = TemperatureMsgEN.getTempMsgPerfect(message);
        contains = text.message.contains(unit);
        LOGGER.debug("perfect msg: {}", text.message);
        assertThat(contains, is(true));

        text = TemperatureMsgEN.getTempMsgTooCold(message, 10, unit);
        contains = text.message.contains(unit);
        LOGGER.debug("too cold msg: {}", text.message);
        assertThat(contains, is(true));

        text = TemperatureMsgEN.getTempMsgTooHot(message, 35, unit);
        contains = text.message.contains(unit);
        LOGGER.debug("too hot msg: {}", text.message);
        assertThat(contains, is(true));

        text = TemperatureMsgEN.getTempMsgBad(message, 16, 20, unit);
        contains = text.message.contains(unit);
        LOGGER.debug("too hot msg: {}", text.message);
        assertThat(contains, is(true));

    }
}
