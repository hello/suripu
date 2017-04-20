package com.hello.suripu.core.translations;


import com.google.common.collect.Lists;

import java.util.List;

public class English {

    /* BEGIN Events Declaration */

    public final static String FALL_ASLEEP_MESSAGE = "You fell asleep.";
    public final static String WAKE_UP_MESSAGE = "Good morning.";
    public final static String WAKE_UP_DISTURBANCE_MESSAGE = "You woke up.";
    public final static String FALL_ASLEEP_DISTURBANCE_MESSAGE = "You went back to bed.";
    public final static String WAKESLEEP_DISTURBANCE_MESSAGE = "You briefly woke up.";
    public final static String SUNSET_MESSAGE = "";
    public final static String SUNRISE_MESSAGE = "Rise and shine.";
    public final static String IN_BED_MESSAGE = "You went to bed.";
    public final static String OUT_OF_BED_MESSAGE = "You got out of bed.";
    public final static String LIGHT_MESSAGE = "There was a light disturbance.";
    public final static String LIGHTS_OUT_MESSAGE = "The lights went out in your room.";
    public final static String MOTION_MESSAGE = "You were moving around quite a bit.";
    public final static String PARTNER_MOTION_MESSAGE = "You and your partner were both moving around.";
    public final static String SLEEP_MOTION_MESSAGE = "You were tossing and turning.";
    public final static String ALARM_DEFAULT_MESSAGE = "Your alarm rang.";
    public final static String ALARM_NORMAL_MESSAGE = "Your alarm rang at **%s**.";
    public final static String ALARM_NOT_SO_SMART_MESSAGE = "Your Smart Alarm rang at **%s**.";
    public final static String ALARM_SMART_MESSAGE = "Your Smart Alarm rang at **%s**.\nYou set it to wake you up by **%s**.";
    public final static String NOISE_MESSAGE = "There was a noise disturbance.";
    public final static String NULL_MESSAGE = "";
    public final static String MORNING_WAKE_UP_MESSAGE = "Good morning.";
    public final static String AFTERNOON_WAKE_UP_MESSAGE = "Good afternoon.";
    public final static String NIGHT_WAKE_UP_MESSAGE = "Good evening.";


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
    public final static String TEMPERATURE_ADVICE_MESSAGE_C = "People tend to sleep best when temperature is between **%d°C** and **%d°C**.";
    public final static String TEMPERATURE_ADVICE_MESSAGE_F = "People tend to sleep best when temperature is between **%d°F** and **%d°F**.";

    public final static String HUMIDITY_ADVICE_MESSAGE =
            "People tend to sleep best when humidity is between **30%** and **60%**.";

    public final static String PARTICULATES_ADVICE_MESSAGE =
            "For the best sleep, the air in your bedroom should be as clean as possible. Ideally, it should be below **80µg/m³**.";

    public final static String LIGHT_ADVICE_MESSAGE =
            "For ideal sleep, your bedroom should be as dark as possible, which is below **2lux**.";

    public final static String SOUND_ADVICE_MESSAGE =
            "For ideal sleep, your bedroom should be as quiet as possible, which is usually below **40dB**.";

    // Sensor Conditions
    // Temperature
    public final static String LOW_TEMPERATURE_WARNING_MESSAGE = "It's **pretty cold** in here.";
    public final static String HIGH_TEMPERATURE_WARNING_MESSAGE = "It's **pretty hot** in here.";

    public final static String LOW_TEMPERATURE_ALERT_MESSAGE = "It's far **too cold** in here.";
    public final static String HIGH_TEMPERATURE_ALERT_MESSAGE = "It's far **too hot** in here.";

    public final static String IDEAL_TEMPERATURE_MESSAGE = "The temperature is **just right**.";

    // Humidity
    public final static String LOW_HUMIDITY_WARNING_MESSAGE = "It's **slightly dry** in here.";
    public final static String HIGH_HUMIDITY_WARNING_MESSAGE = "It's **slightly humid** in here.";

    public final static String LOW_HUMIDITY_ALERT_MESSAGE = "It's far **too dry** in here.";
    public final static String HIGH_HUMIDITY_ALERT_MESSAGE = "It's far **too humid** in here.";

    public final static String IDEAL_HUMIDITY_MESSAGE = "The humidity is **just right**.";

    // Particulates (Air Quality) 
    public final static String VERY_HIGH_PARTICULATES_MESSAGE = "The air quality is **poor**.";
    public final static String HIGH_PARTICULATES_MESSAGE = "The air quality is **not ideal**.";
    public final static String IDEAL_PARTICULATES_MESSAGE = "The air quality is **just right**.";

    // Light
    public final static String IDEAL_LIGHT_MESSAGE = "The light level is **just right**.";
    public final static String WARNING_LIGHT_MESSAGE = "It's a bit **too bright** in here.";
    public final static String ALERT_LIGHT_MESSAGE = "It's far **too bright** in here.";

    // Sound
    public final static String IDEAL_SOUND_MESSAGE = "The noise level is **just right**.";
    public final static String WARNING_SOUND_MESSAGE = "It's a **little noisy** in here.";
    public final static String ALERT_SOUND_MESSAGE = "It's far **too noisy** in here.";


    public final static String LOW_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **too cold** for a good night's sleep.";
    public final static String HIGH_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **too hot** for a good night's sleep.";
    public final static String IDEAL_TEMPERATURE_MESSAGE_EXPANDED = "Your current room's temperature is **just right** for a good night's sleep.";


    // Pre-Sleep condition messages in time-line tab
    // Temperature
    public final static String LOW_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE = "It was **pretty cold** in your room.";
    public final static String HIGH_TEMPERATURE_PRE_SLEEP_WARNING_MESSAGE = "It was **pretty hot** in your room.";

    public final static String LOW_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE = "It was far **too cold** in your room.";
    public final static String HIGH_TEMPERATURE_PRE_SLEEP_ALERT_MESSAGE = "It was far **too hot** in your room.";

    public final static String IDEAL_TEMPERATURE_PRE_SLEEP_MESSAGE = "The temperature was **just right**.";

    // Humidity
    public final static String LOW_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE = "It was **pretty dry** in your room.";
    public final static String HIGH_HUMIDITY_PRE_SLEEP_WARNING_MESSAGE = "It was **pretty humid** in your room.";

    public final static String LOW_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE = "It was far **too dry** in your room.";
    public final static String HIGH_HUMIDITY_PRE_SLEEP_ALERT_MESSAGE = "It was far far **too humid** in your room.";

    public final static String IDEAL_HUMIDITY_PRE_SLEEP_MESSAGE = "The humidity was **just right**.";

    // Particulates
    public final static String VERY_HIGH_PARTICULATES_PRE_SLEEP_MESSAGE = "The air quality was **poor**.";
    public final static String HIGH_PARTICULATES_PRE_SLEEP_MESSAGE = "The air quality was **marginal**.";
    public final static String IDEAL_PARTICULATES_PRE_SLEEP_MESSAGE = "The air quality was **good**.";

    // Light
    public final static String IDEAL_LIGHT_PRE_SLEEP_MESSAGE = "Your room's light level was **just right**.";
    public final static String WARNING_LIGHT_PRE_SLEEP_MESSAGE = "Your room was a bit **too bright**.";
    public final static String ALERT_LIGHT_PRE_SLEEP_MESSAGE = "It was far **too bright** in your room.";

    //Sound
    public final static String IDEAL_SOUND_PRE_SLEEP_MESSAGE = "Your room's noise level was **just right**.";
    public final static String WARNING_SOUND_PRE_SLEEP_MESSAGE = "Your room was **a little noisy**.";
    public final static String ALERT_SOUND_PRE_SLEEP_MESSAGE = "Your room was far **too noisy**.";

    /* END Current Room State Declaration */


    // Recommendations

    public final static String RECOMMENDATION_TEMP_TOO_HIGH = "\n\nTry lowering your thermostat setting or opening a window.";
    public final static String RECOMMENDATION_TEMP_TOO_LOW = "\n\nTry raising the thermostat setting, or closing any open windows";

    public final static String RECOMMENDATION_HUMIDITY_TOO_LOW = "\n\nTry using a humidifier if your bedroom continues to be dry.";
    public final static String RECOMMENDATION_HUMIDITY_TOO_HIGH = "\n\nTry using a dehumidifier if your bedroom continues to be humid.";


    public final static String RECOMMENDATION_LIGHT_TOO_HIGH = "\n\nMake sure all your lights are off. You may want to consider blackout curtains.";
    public final static String RECOMMENDATION_SOUND_TOO_HIGH = "\n\nTry shutting windows if it's noisy outside, or consider ear plugs.";

    public final static String RECOMMENDATION_PARTICULATES_TOO_HIGH = "\n\nTry opening a window to bring in some fresh air. If these conditions persist, you may want to consider a HEPA filter.";
    // TIMELINE
    public final static String TIMELINE_NO_SLEEP_DATA = "There was no sleep data recorded for this night.";
    public final static String TIMELINE_NOT_ENOUGH_SLEEP_DATA = "Some sleep data was recorded, but not enough to generate a Sleep Timeline for this night.";

    // Trends
    public final static String TRENDS_SCORE_AVERAGE = "AVERAGE SLEEP SCORE";
    public final static String TRENDS_SCORE_OVER_TIME = "SLEEP SCORE OVER TIME";
    public final static String TRENDS_DURATION_AVERAGE = "AVERAGE SLEEP DURATION";
    public final static String TRENDS_DURATION_OVER_TIME = "SLEEP DURATION OVER TIME";




    // EMAIL
    public final static String EMAIL_PASSWORD_RESET_HTML_TEMPLATE = "<html>\n" +
            "<head>\n" +
            "    <title>Password Reset</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <p>Hello %s,</p>\n" +
            "    <p>Someone has requested a link to change your password, and you can do this through the link below.</p>\n" +
            "    <p><a href=\"%s\">Change my password</a></p>\n" +
            "    <p>If you didn't request this, please ignore this email.</p>\n" +
            "    <p>Your password won't change until you access the link above and create a new one.</p>\n" +
            "</body>\n" +
            "</html>";

    public final static String EMAIL_PASSWORD_RESET_SUBJECT = "Reset your account password";


    public final static String TIMELINE_UNAVAILABLE = "Your timeline is currently unavailable. Please try again later.";
    public final static String FEEDBACK_INCONSISTENT = "This adjustment could not be made because it conflicts with previous adjustments.";
    public final static String FEEDBACK_AT_INVALID_TIME = "This adjustment could not be made because it is too early or too late.";
    public final static String FEEDBACK_CAUSED_INVALID_SLEEP_SCORE = "This adjustment could not be made because it would result in an invalid sleep score.";

    public final static String ERROR_CLOCK_OUT_OF_SYNC = "Your device's time is significantly different from our reference time. From your device's Settings app, please enable automatic Date & Time, or enter the correct time manually.";

    // Trends V2 graph
    public final static String GRAPH_TITLE_SLEEP_SCORE = "Sleep Score";
    public final static String GRAPH_TITLE_SLEEP_DEPTH = "Sleep Depth";
    public final static String GRAPH_TITLE_SLEEP_DURATION = "Sleep Duration";

    public final static List<String> DAY_OF_WEEK_NAMES = Lists.newArrayList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
    public final static List<String> MONTH_OF_YEAR_NAMES = Lists.newArrayList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

    public final static String ANNOTATION_AVERAGE = "Avg. Overall";
    public final static String ANNOTATION_WEEKDAYS = "Avg. Weekdays";
    public final static String ANNOTATION_WEEKENDS = "Avg. Weekends";

    public final static String SLEEP_DEPTH_LIGHT = "LIGHT";
    public final static String SLEEP_DEPTH_MEDIUM = "MEDIUM";
    public final static String SLEEP_DEPTH_SOUND = "DEEP";
}
