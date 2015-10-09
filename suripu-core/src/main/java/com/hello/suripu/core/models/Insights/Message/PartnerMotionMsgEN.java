package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 10/8/15.
 */
public class PartnerMotionMsgEN {


    public static Text getBadPartner(final Integer percentage) {
        return new Text("The partner",
                String.format("Your partner moves %d%% more than you. ", percentage) +
                "Read on about your partner's impact on your sleep.");
    }

    public static Text getEgalitarian() {
        return new Text("Match made in heaven",
                "You move about the same amount as your partner. Read on about the link between your partner and your sleep.");
    }

    public static Text getBadMe(final Integer percentage) {
        return new Text("It's me, not you",
                String.format("You move %d%% more than your partner. ", percentage) +
                "Read on about your impact on your partner's sleep.");
    }

}
