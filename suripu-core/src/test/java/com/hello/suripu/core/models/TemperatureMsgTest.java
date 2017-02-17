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
        final String unit = TemperatureUnit.FAHRENHEIT.toString();

        Boolean contains;

        Text text;
        text = TemperatureMsgEN.getTempMsgPerfect(10, unit);
        contains = text.message.contains(unit);
        LOGGER.debug("perfect msg: {}", text.message);
        assertThat(contains, is(true));

        text = TemperatureMsgEN.getTempMsgTooCold(10, unit, 10);
        contains = text.message.contains(unit);
        LOGGER.debug("too cold msg: {}", text.message);
        assertThat(contains, is(true));

        text = TemperatureMsgEN.getTempMsgTooHot(10, unit, 10);
        contains = text.message.contains(unit);
        LOGGER.debug("too hot msg: {}", text.message);
        assertThat(contains, is(true));

    }
}
