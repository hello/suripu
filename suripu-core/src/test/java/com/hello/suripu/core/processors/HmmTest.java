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
        private static final String protoData = "CgItMRIXSE1NXzIwMTUtMDItMjdfMTI6MzA6MDUaMAoJCRTunPuzEdo/EgkJHutKVkMIjD8aEglM6xr6wcbvPwlOnoryAp98PyABKAEwABowCgkJ4vgrAUi9FUASCQmyimU5+7SlPxoSCR2wjfgtRO8/CYf7Se5Aepc/IAEoATAAGjAKCQlVulsJPAwYQBIJCRm5m+bMcAlAGhIJQinqidIG6j8JT1tX2LXkxz8gASgAMAAaMAoJCWmI1OmcBfA/EgkJDwDPmsE7G0AaEgmAzU/CG7rsPwmZloHtIS+6PyABKAAwABowCgkJexSuR+F6hD8SCQmF81/tzdXqPxoSCVu0NmUFxe8/CWO8pWRNfX0/IAAoADACGjAKCQl7FK5H4XqEPxIJCZy3y0vS3QhAGhIJumaaTSmW7z8JolFmmax1ij8gACgAMAMaMAoJCc3l2nGBfQBAEgkJl3nol9i08D8aEgkrix2B3EHvPwkGm07cb8SXPyAAKAAwARowCgkJU/EbcVk60D8SCQklJcCZLAYMQBoSCTapxHCl9uk/CRRb7TxqJcg/IAEoADAAGjAKCQnZIYahV2QPQBIJCYU5udlVQwlAGhIJO5Lvh40o7D8J5muDwJO7vj8gASgAMAAgCSkeM9N+K4jtPymd04LDzDmgPyneodF4ea+fPym1YoRL+q6NPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkasHm0GOWZPymuIgefUeLtPymOHK8MP92kPynHxr1DUTUWPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmWUIvusyXJPymRwsvcCU3kPykaPv2Qine3PykRCo6rvtSzPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnePmKZJLyiPyky8IS/fC/hPykA18ntgUnbPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnXrcnC6/frPylAN79jr7O0Pyllb1K7gO+TPykj7tIPMs+UPyn2O6AyXdR1PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkVaOFD4yfaPynaSw9eDuziPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnSNp8tK3iQPynGoWNULIGmPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACllH4YEP1vqPyn+M9GmBvhsPSkkfjTmZse9PynmyyvQg0W8Pyk1jKBm1PSYPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACn6OH5r+U3kPyljJB3dvYbNPynYVRjkNltHPiliCVRHSebUPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkH6yV0q/GKPCmLLX9W24zlPzE57g2GX6ASNzEAAAAAAADwPzECRAX+V6gzNzGbWAwPzNxUJDFcHF7QWKZXITEOG2nf6IFNCTF9ERonWhGAOzGHaYKudBeDMzHycvSZcYETOA==";


        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            Optional<SleepHmmWithInterpretation> ret = Optional.absent();

            BASE64Decoder decoder = new BASE64Decoder();
            try {
                byte[] decodedBytes = decoder.decodeBuffer(protoData);

                SleepHmmProtos.SleepHmm proto = SleepHmmProtos.SleepHmm.parseFrom(decodedBytes);

                ret = Optional.of(SleepHmmWithInterpretation.createModelFromProtobuf(proto));


            } catch (IOException e) {
                e.printStackTrace();
            }

            return ret;
        }

    };

    @Test
    public void TestHmm() {
        final int offset = -3600*1000;
        Optional<SleepHmmWithInterpretation>  hmm = sleepHmmDAO.getLatestModelForDate(0,0);

        assertTrue(hmm.isPresent());

        AllSensorSampleList sensorSampleList = new AllSensorSampleList();
        List<TrackerMotion> motionList = new ArrayList<TrackerMotion>();

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
            TrackerMotion m = new TrackerMotion(0,0,0L,t,500,offset,0L,1L,1L);

            motionList.add(m);

        }

        List<Sample> light = new ArrayList<Sample>();

        for (int i = 1; i < 7*3600; i++) {
            Long t = t0 + i*60*1000;
            Sample s = new Sample(t,0.1f,offset);
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