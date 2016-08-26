package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 9/18/15.
 */
public class HumidityMsgEN {

    public static Text getLowHumidity() {
        return new Text("Superdry",
                "It's **too dry** in your bedroom. In addition to drying out your skin, dry air can irritate your throat and nasal passages, which can make it difficult to fall asleep.");
    }

    public static Text getIdealHumidity() {
        return new Text("Temperate Zone",
                "The humidity level in your bedroom is **ideal for restful sleep**.");
    }

    public static Text getHighHumidity() {
        return new Text("The Life Aquatic",
                "It's **a bit too humid** in your bedroom. Sustained levels of high humidity can lead to mold growth, which can affect your sleep if you suffer from mold allergies.");
    }
}