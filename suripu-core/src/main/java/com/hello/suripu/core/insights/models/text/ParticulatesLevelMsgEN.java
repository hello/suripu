package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 10/7/15.
 */
public class ParticulatesLevelMsgEN {

    private final static String MICROGRAM_PER_CUBIC_METER = "\u00B5g/m\u00b3";

    public static Text getAirIdeal() {
        return new Text("Constant Good", "The air quality in your bedroom is **ideal**.");
    }

    public static Text getAirHigh() {
        return new Text("Constant Bad", "The air quality in your bedroom is **less than ideal**.");
    }

    public static Text getAirWarningHigh() {
        return new Text("Constant Very Bad", "The air quality in your bedroom is significantly less than ideal.");
    }
}
