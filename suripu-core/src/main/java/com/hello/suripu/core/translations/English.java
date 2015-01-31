package com.hello.suripu.core.translations;


import com.hello.suripu.core.processors.insights.TemperatureHumidity;

public class English {

    /* BEGIN Events Declaration */

    public final static String FALL_ASLEEP_MESSAGE = "You fell asleep.";
    public final static String WAKE_UP_MESSAGE = "Good morning.";
    public final static String SUNSET_MESSAGE = "";
    public final static String SUNRISE_MESSAGE = "Rise and shine.";
    public final static String IN_BED_MESSAGE = "You went to bed.";
    public final static String OUT_OF_BED_MESSAGE = "You got out of bed.";
    public final static String LIGHT_MESSAGE = "Your room lit up.";
    public final static String LIGHTS_OUT_MESSAGE = "The lights went out in your room.";
    public final static String MOTION_MESSAGE = "You were moving around quite a bit.";
    public final static String PARTNER_MOTION_MESSAGE = "Your partner was moving quite a bit.";
    public final static String SLEEP_MOTION_MESSAGE = "You were tossing and turning.";
    public final static String ALARM_NORMAL_MESSAGE = "Your alarm rang at **%s**.";
    public final static String ALARM_SMART_MESSAGE = "Your Smart Alarm rang at **XX%s**.\nYou asked to be awoken by **YY%s**.";
    public final static String NULL_MESSAGE = "";


    /* END Events Declaration */


    /* BEGIN Room State Declaration */

    // Be careful with changing the markdown & string specifiers i.e. **XX%s**, it could break the code

    // Loading States
    public final static String LOADING_TEMPERATURE_MESSAGE = "Waiting for data.";
    public final static String LOADING_HUMIDITY_MESSAGE = "Waiting for data.";
    public final static String LOADING_PARTICULATES_MESSAGE = "Waiting for data.";
    public final static String LOADING_LIGHT_MESSAGE = "Waiting for data.";
    public final static String LOADING_SOUND_MESSAGE = "Waiting for data.";

    // Unknown States
    public final static String UNKNOWN_TEMPERATURE_MESSAGE = "Could not retrieve the current temperature.";
    public final static String UNKNOWN_HUMIDITY_MESSAGE = "Could not retrieve the current humidity.";
    public final static String UNKNOWN_PARTICULATES_MESSAGE = "Could not retrieve current AQI.";
    public final static String UNKNOWN_LIGHT_MESSAGE = "Could not retrieve the current light level.";
    public final static String UNKNOWN_SOUND_MESSAGE = "Could not retrieve the current sound level.";

    // Advice
    public final static String TEMPERATURE_ADVICE_MESSAGE_C = String.format(
            "People tend to sleep best when temperature is between **%d°C** and **%d°C**.",
            TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS, TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS);

    public final static String TEMPERATURE_ADVICE_MESSAGE_F = String.format(
            "People tend to sleep best when temperature is between **%d°F** and **%d°F**.",
            TemperatureHumidity.IDEAL_TEMP_MIN, TemperatureHumidity.IDEAL_TEMP_MAX);

    public final static String HUMIDITY_ADVICE_MESSAGE = String.format(
            "People tend to sleep best when humidity is between **%d%%** and **%d%%**.",
            TemperatureHumidity.IDEAL_HUMIDITY_MIN, TemperatureHumidity.IDEAL_HUMIDITY_MAX);

    public final static String PARTICULATES_ADVICE_MESSAGE = "The lower the AQI in your bedroom, the better you sleep.";
    public final static String LIGHT_ADVICE_MESSAGE = "For ideal sleep, your bedroom should be as dark as possible.";
    public final static String SOUND_ADVICE_MESSAGE = "For ideal sleep, your bedroom should be as quiet as possible.";

    // Temperature Conditions
    public final static String LOW_TEMPERATURE_MESSAGE = "It’s **pretty cold** in here.";
    public final static String HIGH_TEMPERATURE_MESSAGE = "It’s **pretty hot** in here.";
    public final static String IDEAL_TEMPERATURE_MESSAGE = "The temperature is **just right**.";

    // Humidity Conditions
    public final static String LOW_HUMIDITY_PRE_SLEEP_MESSAGE = "It was **pretty dry** in here.";
    public final static String HIGH_HUMIDITY_PRE_SLEEP_MESSAGE = "It was**pretty humid** in here.";
    public final static String IDEAL_HUMIDITY_PRE_SLEEP_MESSAGE = "The humidity was **just right**.";

    public final static String LOW_HUMIDITY_MESSAGE = "It's **slightly dry** in here.";
    public final static String HIGH_HUMIDITY_MESSAGE = "It's **slightly humid** in here.";
    public final static String IDEAL_HUMIDITY_MESSAGE = "Humidity is **just right**.";

    // Particulates Conditions (Air Quality)
    public final static String VERY_HIGH_PARTICULATES_PRE_SLEEP_MESSAGE = "AQI was at an **UNHEALTHY** level.";
    public final static String HIGH_PARTICULATES_PRE_SLEEP_MESSAGE = "AQI was **moderately high**.";
    public final static String IDEAL_PARTICULATES_PRE_SLEEP_MESSAGE = "The particulates level was **just right** last night.";

    public final static String VERY_HIGH_PARTICULATES_MESSAGE = "AQI is at an **unhealthy** level.";
    public final static String HIGH_PARTICULATES_MESSAGE = "AQI is **moderately high**.";
    public final static String IDEAL_PARTICULATES_MESSAGE = "AQI is **just right**.";

    // Light Conditions.
    public final static String IDEAL_LIGHT_PRE_SLEEP_MESSAGE = "The light level was **perfect**.";
    public final static String WARNING_LIGHT_PRE_SLEEP_MESSAGE = "The light level was **higher** than ideal.";
    public final static String ALERT_LIGHT_PRE_SLEEP_MESSAGE = "The light level was **way too high**.";

    public final static String IDEAL_LIGHT_MESSAGE = "The light is **just right**.";
    public final static String WARNING_LIGHT_MESSAGE = "It is slightly **too bright** now.";
    public final static String ALERT_LIGHT_MESSAGE = "It is **far too bright** now.";

    // Sound Conditions
    public final static String IDEAL_SOUND_PRE_SLEEP_MESSAGE = "Your room's sound level was **just right**.";
    public final static String WARNING_SOUND_PRE_SLEEP_MESSAGE = "Your room was **a little noisy**.";
    public final static String ALERT_SOUND_PRE_SLEEP_MESSAGE = "Your room was **too noisy**.";

    public final static String IDEAL_SOUND_MESSAGE = "The sound level is **just right**.";
    public final static String WARNING_SOUND_MESSAGE = "Your room is **a little noisy**.";
    public final static String ALERT_SOUND_MESSAGE = "Your room is **too noisy**.";


    public final static String LOW_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **too cold** for a good night's sleep.";
    public final static String HIGH_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **too hot** for a good night's sleep.";
    public final static String IDEAL_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **just right** for a good night's sleep.";

    // Temperature pre-sleep conditions
    public final static String LOW_TEMPERATURE_PRE_SLEEP_MESSAGE = "It was **too cold** in your room.";
    public final static String HIGH_TEMPERATURE_PRE_SLEEP_MESSAGE = "It was **too hot** in your room.";
    public final static String IDEAL_TEMPERATURE_PRE_SLEEP_MESSAGE = "Temperature was **just right**.";

    /* END Current Room State Declaration */

}
