package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class LightMsgEN {
    public static Text getLightDark(final int medianLight, final int percentile) {
        return new Text("Hello, Darkness",
                String.format("Your bedroom light condition of %d lux is perfect, ", medianLight) +
                        String.format("it is **dimmer than** %d%% of all Sense users.", 100 - percentile)
        );
    }

    public static Text getLightNotDarkEnough(final int medianLight, final int percentile) {
        return new Text("Hello, Dark Room",
                String.format("Your bedroom light condition of %d lux is close to ideal, ", medianLight) +
                        String.format("it is **dimmer than** %d%% of all Sense users.", 100 - percentile));
    }

    public static Text getLightALittleBright(final int medianLight, final int percentile) {
        return new Text("Hmm, It's A Little Bright",
                String.format("Your bedroom light of %d lux is a little brighter than ideal, ", medianLight) +
                        String.format("it is **brighter than** %d%% of all Sense users.", percentile) +
                        "\n\nTry dimming the light a little before bedtime.");

    }

    public static Text getLightQuiteBright(final int medianLight, final int percentile) {
        return new Text("Hello, Light",
                String.format("Your bedroom is too bright (%d lux) for ideal sleep conditions, ", medianLight) +
                        String.format("it is **brighter than** %d%% of all Sense users.", percentile) +
                        "\n\nTry changing to a lower wattage light bulb, or turning off the light 15 minutes before bedtime.");
    }

    public static Text getLightTooBright(final int medianLight, final int percentile) {
        return new Text("Time to Dim It Down",
                String.format("Your bedroom light of %d lux is as bright as a warehouse aisle! ", medianLight) +
                        String.format("It is **brighter than** %d%% of all Sense users.", percentile) +
                        "\n\nChanging to a lower wattage light bulb might help improve your sleep."
                );
    }

    public static Text getLightWayTooBright(final int medianLight, final int percentile) {
        return new Text("It's Way Too Bright",
                String.format("Your bedroom light of %d lux is as bright as an office room! ", medianLight) +
                        String.format("It's **brighter than** %d%% of all Sense users.", percentile) +
                        "\n\nChanging to a lower wattage light bulb might help improve your sleep.");
    }

}