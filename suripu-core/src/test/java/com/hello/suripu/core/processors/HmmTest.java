package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import org.junit.Test;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by benjo on 3/3/15.
 */
public class HmmTest {

    private static long t0 = 1425420828000L;
    private static long tf = t0 + 3600 * 8 * 1000;
    private static long tc1 = t0 + 3600 * 6 * 1000;
    private static long tc2 = t0 + 3600 * 7 * 1000;


    static private SleepHmmDAO sleepHmmDAO = new SleepHmmDAO() {
        private static final String protoData = "CgItMRIXSE1NXzIwMTUtMDMtMDRfMTI6NTM6NDUaOSABKAEwAFoSCWHHl+XhuvU/ESaFaBeTbPQ/YgkJv9bYnfsUlT9qEgmWRn/xI0bsPwn1ywV04M69Pxo5IAEoATAAWhIJ56JN37EyGkARugcyQ1jc+j9iCQklAYysT1SeP2oSCXFQG2ABfeo/CQm+kn/6C8Y/GjkgASgAMABaEgkDW47nqZYcQBFCCl1WHJz9P2IJCZDtxLTHRglAahIJ0sQKXYsWxj8Jzk69KF166j8aOSABKAAwAFoSCVuxUyNtTPQ/EWVRF7tnDfg/YgkJrSD4CYxSFUBqEgko9ZbjyYzSPwlyhTQOm7nmPxo5IAAoADACWhIJ0YA22MAL0j8RxGdTUXtn2z9iCQmqpWjs9tztP2oSCUGJIXuXd+0/Ce628yZEQ7Q/GjkgACgAMANaEgmC2bpgwaTOPxHUZKhXEuzTP2IJCbNas4edCQtAahIJj6xmdcDU6j8JrUxlKv6sxD8aOSABKAAwAFoSCS1XjsY0Svo/EUUSz+Vf+vM/YgkJkjxtzcI1AUBqEgnkCE37KvTcPwmVe1mC6oXhPxo5IAEoADAAWhIJovav+7tUFUARMLKs+AZX9j9iCQnQD7qyEOH6P2oSCcQrG+j5reI/CX2oyS8MpNo/IAgpmR8/20T57T8psuxMpoU5mj8pG3zl4gJJlT8pJqvpDtxUkT8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApl5CqgND+oD8pLia0pTO27T8p8VwGcNY/oz8ppvYrA62HRz8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApbvvZvmZTyj8pEo82usxY5D8pTC5F9Euktz8pcWFSvIDusD8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAAp5gvWWI6Mrj8pSn8Japx/4T8pXT/SYDUv2T8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApXbBoM4WknT8ph/+6dpyX7D8pE05Adrndoz8pMDdJD/dcoD8pYjCRqCvKez8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApIdD1pk0C3j8pBBiFLNn+4D8pAAAAAAAAAAApAAAAAAAAAAAphi7lS6dQuT8p+5rH97ozmD8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAAp+MNO6KuR5z8phmbZWYUKwj8poy7YTWF+ej8prpviwrtsxz8pAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApAAAAAAAAAAApMqaYLQejij8pOEb1L0iF6T8xAQAAAAAA8D8xbHbt7PqCPDwx7SFlPPXNbDUx6LMJBRFZbzQxxL0qjznVSzox+22ixAkhZTYx/uIV5tON6zoxitdANpNEqTY=";

        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            Optional<SleepHmmWithInterpretation> ret = Optional.absent();

            BASE64Decoder decoder = new BASE64Decoder();
            try {
                final byte[] decodedBytes = decoder.decodeBuffer(protoData);

                final SleepHmmProtos.SleepHmm proto = SleepHmmProtos.SleepHmm.parseFrom(decodedBytes);

                ret = SleepHmmWithInterpretation.createModelFromProtobuf(proto);


            } catch (IOException e) {
                e.printStackTrace();
            }

            return ret;
        }

    };

    @Test
    public void TestHmm() {
        final int offset = -3600*1000;
        final Optional<SleepHmmWithInterpretation>  hmm = sleepHmmDAO.getLatestModelForDate(0,0);

        assertTrue(hmm.isPresent());

        final AllSensorSampleList sensorSampleList = new AllSensorSampleList();
        final List<TrackerMotion> motionList = new ArrayList<TrackerMotion>();

        for (int i = 0; i < 24; i++) {
            /*
            @JsonProperty("id") final long id,
            @JsonProperty("account_id") final long accountId,
            @JsonProperty("tracker_id") final Long trackerId,
            @JsonProperty("timestamp") final long timestamp,
            @JsonProperty("value") final int value,
            @JsonProperty("timezone_offset") final int timeZoneOffset,
            final Long motionRange,
            final Long kickOffCounts,
            final Long onDurationInSeconds
            */
            final long t = t0 + i*3600/3*1000;
            final TrackerMotion m = new TrackerMotion(0,0,0L,t,500,offset,0L,1L,1L);

            motionList.add(m);

        }

        List<Sample> light = new ArrayList<Sample>();

        for (int i = 1; i < 7*3600; i++) {
            final Long t = t0 + i*60*1000;
            final Sample s = new Sample(t,0.1f,offset);
            light.add(s);
        }

        sensorSampleList.add(Sensor.LIGHT,light);

        Optional<SleepHmmWithInterpretation.SleepHmmResult> res = hmm.get().getSleepEventsUsingHMM(sensorSampleList,motionList,t0,tf,tc1);
        Optional<SleepHmmWithInterpretation.SleepHmmResult> res2 = hmm.get().getSleepEventsUsingHMM(sensorSampleList,motionList,t0,tf,tc2);

        assertTrue(res.isPresent());
        assertTrue(res2.isPresent());

        assertTrue(res.get().fallAsleep.isPresent());

        final int expectedLength1 =  6*60 / SleepHmmWithInterpretation.NUM_MINUTES_IN_WINDOW;
        final int expectedLength2 =  7*60 / SleepHmmWithInterpretation.NUM_MINUTES_IN_WINDOW;

        assertTrue(res.get().path.size() ==  expectedLength1);
        assertTrue(res2.get().path.size() ==  expectedLength2);



    }
}