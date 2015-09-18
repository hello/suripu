package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 9/18/15.
 */
public class HumidityMsgEN {
    public static Text getVeryLowHumidity() {
        return new Text("Superdry",
                "Your bedroom is a lot more dry than ideal. Air that is too dry can irritate your throat and nasal passages and dry out your skin. " +
                        "\n\n Like having allergies, this can make it more difficult for you to fall asleep.");
    }

    public static Text getLowHumidity() {
        return new Text("California 2014",
                "Your bedroom is more dry than ideal. Air that is too dry can irritate your throat and nasal passages and dry out your skin. " +
                        "\n\nLike having allergies, this can make it more difficult for you to fall asleep. ");

    }

    public static Text getIdealHumidity() {
        return new Text("Happy as a clam",
                "Your bedroom has the ideal humidity level. Read on to learn how humidity impacts your sleep.");

    }

    public static Text getHighHumidity() {
        return new Text("Foggy Bottom",
                "Your bedroom is more humid than ideal. " +
                        "\n\n Sustained levels of high humidity can lead to mold growth, which can affect your sleep if you suffer from mold allergies.");

    }

    public static Text getVeryHighHumidity() {
        return new Text("Watergate",
                "Your bedroom is a lot more humid than ideal. " +
                        "\n\n Sustained levels of high humidity can lead to mold growth, which can affect your sleep if you suffer from mold allergies.");

    }
}