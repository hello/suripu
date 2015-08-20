package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 8/19/15.
 */
public class BedLightDurationMsgEN {
    public static Text getLittleLight() {
        return new Text("Hello, Darkness",
                "You don't have a light on. Good."
        );
    }

    public static Text getMediumLight() {
        return new Text("Crepuscular",
                "You have a light on. Bad."
        );
    }

    public static Text getHighLight() {
        return new Text("Night owl",
                "You have a light on. Bad."
        );
    }

}
