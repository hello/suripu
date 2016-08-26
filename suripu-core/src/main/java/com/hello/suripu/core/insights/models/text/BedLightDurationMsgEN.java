package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 8/19/15.
 */
public class BedLightDurationMsgEN {
    public static Text getLittleLight() {
//        not actually used - will need to update text if we do use.
        return new Text("Doing well",
                "Your bedroom is dim at night, which is great for your sleep."
        );
    }

    public static Text getMediumLight() {
        return new Text("Crepuscular",
                "There seems to be a bright light on in your bedroom late into your night. If you have trouble falling asleep, consider moving any pre-sleep activities outside of the bedroom."
        );
    }

    public static Text getHighLight() {
        return new Text("Night owl",
                "There seems to be a bright light on in your bedroom late into your night. If you have trouble falling asleep, consider moving any pre-sleep activities outside of the bedroom."
        );
    }

}
