package com.hello.suripu.algorithm.core;

public class LightSegment {
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

    public final long startTimestamp;
    public final long endTimestamp;
    public final int offsetMillis;
    public final Type segmentType;

    public LightSegment(final long startTimestamp, final long endTimestamp, final int offsetMillis, final Type segmentType) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.offsetMillis = offsetMillis;
        this.segmentType = segmentType;
    }

    public static LightSegment updateWithSegmentType(final LightSegment lightSegment, final Type segmentType) {
        return new LightSegment(lightSegment.startTimestamp,
                lightSegment.endTimestamp,
                lightSegment.offsetMillis,
                segmentType);

    }

}
