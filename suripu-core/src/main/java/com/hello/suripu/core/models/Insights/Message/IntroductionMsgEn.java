package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 2/9/15.
 */
public class IntroductionMsgEn {
    public static Text getWelcomeMessage() {
        return new Text("Nice To Meet You",
                "Welcome to Sense. " +
                        "This is where you'll see personalized **Sleep Insights** " +
                        "related to your sleeping patterns, habits and bedroom.\n\n" +
                        "From time to time, you'll also be shown **Questions**. " +
                        "Answering these questions will help Sense learn more about you, " +
                        "to provide more accurate and detailed " +
                        "insights over time.");
    }

    public static Text getSleepTipsMessage() {
        return new Text("Sleep Tips",
                "Having **healthy sleep habits** is crucial to a good night's sleep. " +
                        "Some quick tips:\n\n" +
                        "- Try to stick to the same sleep and wake up time.\n" +
                        "- Avoid naps, caffeine drinks and heavy meals close to bedtime.\n" +
                        "- Exercise frequently.\n" +
                        "- Wind down about an hour before sleep.\n");
    }

    public static Text getSleepDurationMessage(final int minHours, final int maxHours, final int absoluteMinHours, final int absoluteMaxHours) {
        return new Text("How Much Sleep Do We Need?",
                "The National Sleep Foundation recently published a recommendation of " +
                        String.format("%d to %d", minHours, maxHours) +
                        " hours of sleep for people your age. " +
                        "While a few people do need a lot less or more, aim to sleep " +
                        String.format("**no less than %d**", absoluteMinHours) + " and " +
                        String.format("**no more than %d**", absoluteMaxHours) + " hours a night."
        );
    }
}
