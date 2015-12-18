package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 9/18/15.
 */
public class HumidityMsgEN {

    public static Text getLowHumidity() {
        return new Text("Superdry",
                "Your bedroom is **too dry**. " +
                        "In addition to drying out your skin, dry air can irritate your throat and nasal passages, " +
                        "which can make it more difficult for you to fall asleep.");
    }

    public static Text getIdealHumidity() {
        return new Text("Temperate Zone",
                "Your bedroom has the **ideal humidity level**.");
    }

    public static Text getHighHumidity() {
        return new Text("The Life Aquatic",
                "Your bedroom is **a bit too humid**. " +
                        "Sustained levels of high humidity can lead to mold growth, which can affect your sleep if you suffer from mold allergies.");
    }
}