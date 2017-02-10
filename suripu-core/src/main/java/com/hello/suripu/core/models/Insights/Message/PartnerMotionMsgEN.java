package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 10/8/15.
 */
public class PartnerMotionMsgEN {


    public static Text getBadPartner(final Integer percentage) {
        return new Text("The partner",
                String.format("Last week, your partner moved %d%% more than you. ", percentage) +
                "Your partner's movements can affect the quality of your sleep.");
    }

    public static Text getTwiceBadPartner() {
        return new Text("The 2X partner",
                "Last week, your partner moved twice as much as you. Your partner's movements can affect the quality of your sleep.");
    }

    public static Text getVeryBadPartner() {
        return new Text("The >2X partner",
                "Last week, your partner moved more than twice as much as you. Your partner's movements can affect the quality of your sleep.");
    }

    public static Text getEgalitarian() {
        return new Text("Match made in heaven",
                "Last week, you moved about the same amount as your partner.");
    }

    public static Text getBadMe(final Integer percentage) {
        return new Text("It's me, not you",
                String.format("Last week, you moved %d%% more than your partner. ", percentage) +
                "Your movements can affect the quality of your partner's sleep.");
    }

    public static Text getTwiceBadMe() {
        return new Text("The 2X me",
                "Last week, you moved twice as much as your partner. Your movements can affect the quality of your partner's sleep.");
    }

    public static Text getVeryBadMe() {
        return new Text("The >2X me",
                "Last week, you moved more than twice as much as your partner. Your movements can affect the quality of your partner's sleep.");
    }

}
