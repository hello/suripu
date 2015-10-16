package com.hello.suripu.core.processors.insights;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.Message.ParticulatesAnomalyMsgEN;
import com.hello.suripu.core.models.Insights.Message.ParticulatesLevelMsgEN;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 10/12/15.
 */
public class ParticulatesInsightsTest {
    private final Float NORMAL_PARTICULATE_LEVEL = 50.0f;

    @Test
    public void test_getHistoryDust_1() {

        final Float currentDust = 33.0f;
        final List<Float> dustList = Lists.newArrayList(currentDust);

        final Float historyDust = Particulates.getHistoryDust(dustList, currentDust);
        final Float expectedHistoryDust = currentDust;

        assertThat(historyDust, is(expectedHistoryDust));
    }

    @Test
    public void test_getHistoryDust_2() {

        final Float currentDust = 33.0f;
        final List<Float> dustList = Lists.newArrayList(0.0f, 10.0f, 20.0f, currentDust);

        final Float historyDust = Particulates.getHistoryDust(dustList, currentDust);
        final Float expectedHistoryDust = 10.0f;

        assertThat(historyDust, is(expectedHistoryDust));
    }

    @Test
    public void test_getAnomalyTest_1() {

        final Float dustDiff = -1 * Particulates.PARTICULATE_SIG_DIFF;
        final Float historyDust = NORMAL_PARTICULATE_LEVEL;
        final Float currentDust = historyDust + dustDiff;
        final Float percent = -100.0f * dustDiff / historyDust;

        final String myTitle = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).title;
        final String expectedTitle = ParticulatesAnomalyMsgEN.getAirImprovement(currentDust.intValue(), historyDust.intValue(), percent.intValue()).title;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).message;
        final String expectedMessage = ParticulatesAnomalyMsgEN.getAirImprovement(currentDust.intValue(), historyDust.intValue(), percent.intValue()).message;
        assertThat(myMessage, is(expectedMessage));

    }

    @Test
    public void test_getAnomalyTest_2() {

        final Float dustDiff = 2 * Particulates.PARTICULATE_SIG_DIFF;
        final Float historyDust = NORMAL_PARTICULATE_LEVEL;
        final Float currentDust = historyDust + dustDiff;
        final Float percent = 100.0f * dustDiff / historyDust;

        final String myTitle = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).title;
        final String expectedTitle = ParticulatesAnomalyMsgEN.getAirWorse(currentDust.intValue(), historyDust.intValue(), percent.intValue()).title;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).message;
        final String expectedMessage = ParticulatesAnomalyMsgEN.getAirWorse(currentDust.intValue(), historyDust.intValue(), percent.intValue()).message;
        assertThat(myMessage, is(expectedMessage));

    }

    @Test
    public void test_getAnomalyTest_3() {

        final Float dustDiff = 5 * Particulates.PARTICULATE_SIG_DIFF;
        final Float historyDust = NORMAL_PARTICULATE_LEVEL;
        final Float currentDust = historyDust + dustDiff;
        final Float percent = 100.0f * dustDiff / historyDust;

        final String myTitle = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).title;
        final String expectedTitle = ParticulatesAnomalyMsgEN.getAirVeryWorse(currentDust.intValue(), historyDust.intValue(), percent.intValue()).title;;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getAnomalyText(currentDust, historyDust, dustDiff).message;
        final String expectedMessage = ParticulatesAnomalyMsgEN.getAirVeryWorse(currentDust.intValue(), historyDust.intValue(), percent.intValue()).message;;
        assertThat(myMessage, is(expectedMessage));

    }

    @Test
    public void test_getConstantTest() {

        final Float currentDust = Particulates.PARTICULATE_DENSITY_MAX_IDEAL - 10.0f;

        final String myTitle = Particulates.getConstantText(currentDust).title;
        final String expectedTitle = ParticulatesLevelMsgEN.getAirIdeal().title;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getConstantText(currentDust).message;
        final String expectedMessage = ParticulatesLevelMsgEN.getAirIdeal().message;
        assertThat(myMessage, is(expectedMessage));

    }

    @Test
    public void test_getHistoryTest() {

        final Float currentDust = (Particulates.PARTICULATE_DENSITY_MAX_IDEAL + Particulates.PARTICULATE_DENSITY_MAX_WARNING) / 2.0f;

        final String myTitle = Particulates.getConstantText(currentDust).title;
        final String expectedTitle = ParticulatesLevelMsgEN.getAirHigh().title;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getConstantText(currentDust).message;
        final String expectedMessage = ParticulatesLevelMsgEN.getAirHigh().message;
        assertThat(myMessage, is(expectedMessage));

    }

    @Test
    public void test_getAvgAirQuality() {

        final Float currentDust = Particulates.PARTICULATE_DENSITY_MAX_WARNING + 10.0f;

        final String myTitle = Particulates.getConstantText(currentDust).title;
        final String expectedTitle = ParticulatesLevelMsgEN.getAirWarningHigh().title;
        assertThat(myTitle, is(expectedTitle));

        final String myMessage = Particulates.getConstantText(currentDust).message;
        final String expectedMessage = ParticulatesLevelMsgEN.getAirWarningHigh().message;
        assertThat(myMessage, is(expectedMessage));

    }
}
