package com.hello.suripu.core.algorithmintegration;

/**
 * Created by benjo on 11/2/15.
 */
public class SensorDataTimeSpanInfo {
    private final OffsetJump offsetJump;
    public final long startTimeUTC;
    public final long endTimeUTC;

    protected static class OffsetJump {
        final int timzeoneBeforeJump;
        final int timezoneAfterJump;
        final long timeOfJumpUTC;

        public OffsetJump(int timzeoneBeforeJump, int timezoneAfterJump, long timeOfJumpUTC) {
            this.timzeoneBeforeJump = timzeoneBeforeJump;
            this.timezoneAfterJump = timezoneAfterJump;
            this.timeOfJumpUTC = timeOfJumpUTC;
        }
    }

    public SensorDataTimeSpanInfo(OffsetJump offsetJump, long startTimeUTC, long endTimeUTC) {
        this.offsetJump = offsetJump;
        this.startTimeUTC = startTimeUTC;
        this.endTimeUTC = endTimeUTC;
    }

    public SensorDataTimeSpanInfo(long startTimeUTC, long endTimeUTC) {
        this.offsetJump = new OffsetJump(0,0,0);
        this.startTimeUTC = startTimeUTC;
        this.endTimeUTC = endTimeUTC;
    }

    public int getOffsetAtTime(final long timeUTC) {
        if (timeUTC > offsetJump.timeOfJumpUTC) {
            return offsetJump.timezoneAfterJump;
        }
        else {
            return offsetJump.timzeoneBeforeJump;
        }
    }
}
