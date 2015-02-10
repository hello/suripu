package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 2/9/15.
 */
public class IntroductionMsgEn {
    public static Text getWelcomeMessage() {
        return new Text("Nice To Meet You",
                "Welcome to Sense. " +
                        "This is where you'll see personalized **Sleep Insights** " +
                        "related to your sleep patterns and behavior.\n\n" +
                        "From time to time, you'll also be shown **Questions** in this space. " +
                        "These questions will help us provide more accurate and detailed " +
                        "insights about what's affecting your sleep. Let's get started.");
    }

    public static Text getSleepTipsMessage() {
        return new Text("Sleep Tips",
                "Having **healthy sleep habits** is crucial to a good night's sleep. Try to:\n\n" +
                        "- Stick to the same sleep and wake up time.\n" +
                        "- Avoid naps, caffeine drinks and heavy meals close to bedtime.\n" +
                        "- Exercise frequently.\n" +
                        "- Wind down about an hour before sleep.\n" +
                        "- Manage your circadian rhythms.\n");
    }

    public static Text getSleepDurationMessage(final int minHours, final int maxHours, final int absoluteMinHours, final int absoluteMaxHours) {
        return new Text("How Much Sleep Do We Need?",
                "The National Sleep Foundation recently published a recommendation of " +
                        String.format("%d to %d", minHours, maxHours) +
                        " hours for your age group. While there may be some individual variability, you should sleep " +
                        String.format("**no less than %d**", absoluteMinHours) + " and " +
                        String.format("**no more than %d**", absoluteMaxHours) + " hours."
        );
    }
}
