package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 10/8/15.
 */
public class PartnerMotionMsgEN {


    public static Text getBadPartner(final Integer percentage) {
        return new Text("The partner",
                String.format("Last night, your partner moved %d%% more than you. ", percentage) +
                "Your partner's movements can affect the quality of your sleep.");
    }

    public static Text getEgalitarian() {
        return new Text("Match made in heaven",
                "Last night, you moved about the same amount as your partner.");
    }

    public static Text getBadMe(final Integer percentage) {
        return new Text("It's me, not you",
                String.format("Last night, you moved %d%% more than your partner. ", percentage) +
                "Your movements can affect the quality of your partner's sleep.");
    }

}
