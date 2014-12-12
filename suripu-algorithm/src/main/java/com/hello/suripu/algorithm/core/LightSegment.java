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

    private long startTimestamp = 0;
    private long endTimestamp = 0;
    private int offsetMillis = 0;
    private Type segmentType = Type.NONE;

    public LightSegment() {}

    public LightSegment(final long startTimestamp, final long endTimestamp, final int offsetMillis) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.offsetMillis = offsetMillis;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getOffsetMillis() {
        return offsetMillis;
    }

    public void setOffsetMillis(int offsetMillis) {
        this.offsetMillis = offsetMillis;
    }

    public long getDuration(){
        return getEndTimestamp() - getStartTimestamp();
    }

    public void setType(final Type segmentType) {this.segmentType = segmentType;}

    public Type getType() {return this.segmentType;}

}
