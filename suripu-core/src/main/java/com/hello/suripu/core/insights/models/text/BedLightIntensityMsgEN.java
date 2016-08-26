package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 8/28/15.
 */
public class BedLightIntensityMsgEN {

    public static Text getGoodHabits(final int morningRatio) {
        return new Text("Early bird gets the worm",
                "You're exposed to less light at night than you are in the morning, which is ideal. " +
                        "This keeps your biological clock calibrated, " +
                        "and makes it easier to fall asleep at night.");
    }

    public static Text getMoreThanOne(final int nightRatio) {
        return new Text("Who's on first?",
                "You're exposed to a bit more light at night than you are in the morning. " +
                        "This can throw your biological clock out of sync, and make it harder to fall asleep at night. " +
                        "Try dimming your lights at night, and opening your drapes to let in some natural light in the morning.");
    }

    public static Text getMoreThanTwo(final int nightRatio) {
        return new Text("What happens once will never...",
                "You're exposed to more light at night than you are in the morning. " +
                        "This can throw your biological clock out of sync, and make it harder to fall asleep at night. " +
                        "Try dimming your lights at night, and opening your drapes to let in some natural light in the morning.");
    }
    public static Text getMoreThanThree(final int nightRatio) {
        return new Text("Three time's the charm",
                "You're exposed to significantly more light at night than you are in the morning. " +
                        "This can throw your biological clock out of sync, and make it much harder to fall asleep at night. " +
                        "Try dimming your lights at night, and opening your drapes to let in some natural light in the morning.");
    }






}
