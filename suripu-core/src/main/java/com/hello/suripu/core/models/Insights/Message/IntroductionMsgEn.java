package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 2/9/15.
 */
public class IntroductionMsgEn {
    public static Text getWelcomeMessage() {
        return new Text("Nice To Meet You",
                "Welcome to Sense. This is where you'll see personalized Sleep Insights related to your sleeping patterns, habits, and bedroom."
        );
    }

    public static Text getSleepTipsMessage() {
        return new Text("Sleep Tips",
                "Your activity throughout the day can have a profound effect on your sleep. " +
                        "Over time, Sense will help you discover the specific factors that are affecting your sleep. " +
                        "For now, here are some general tips to help you get started."
        );
    }

    public static Text getSleepDurationMessage() {
        return new Text("How Much Sleep Do We Need?",
                "While getting a good night's sleep is crucial, the most important thing is to focus on sleeping better, not more. " +
                        "Fitful sleep will leave you feeling tired, even if you sleep for 8 hours. " +
                        "However, getting only a few hours of great sleep isn't enough either."
        );
    }
}
