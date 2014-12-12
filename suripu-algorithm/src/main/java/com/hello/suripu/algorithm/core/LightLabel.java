package com.hello.suripu.algorithm.core;

/**
 * Created by kingshy on 12/11/14.
 */
public class LightLabel{
    public enum Type {
        NONE {
            public String toString() {return "";}
        },
        LIGHTS_OUT {
            public String toString() {return "Lights out";}
        },
        LIGHT_SPIKE{
            public String toString() {return "Light";}
        },
        DAYLIGHT{
            public String toString() {return "Daylight";}
        },
        LOW_DAYLIGHT{
            public String toString() {return "Daylight low";}
        },
        SUNLIGHT_SPIKE{
            public String toString() {return "Daylight spike";}
        };
    }
}
