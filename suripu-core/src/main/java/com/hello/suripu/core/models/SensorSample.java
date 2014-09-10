package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SensorSample {

    @JsonProperty("datetime_utc")
    public final DateTime dateTime;

    @JsonProperty("value")
    public final float val;

    @JsonProperty("timezone_offset")
    public final int timeZoneOffset;

    public SensorSample(final DateTime dateTime, final float val, final int timeZoneOffset) {

        this.dateTime = dateTime;
        this.val = val;
        this.timeZoneOffset = timeZoneOffset;
    }

    public byte[] getBytes() {
        this.toString();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeBytes(this.toString());
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }

    }
}
