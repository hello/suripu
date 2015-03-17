package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import junit.framework.TestCase;
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

    private static final int NUM_MINUTES_IN_WINDOW = 15;
    private static long t0 = 1425420828000L;
    private static long tf = t0 + 3600 * 8 * 1000;
    private static long tc1 = t0 + 3600 * 6 * 1000;
    private static long tc2 = t0 + 3600 * 7 * 1000;


    static private SleepHmmDAO sleepHmmDAO = new SleepHmmDAO() {
        private static final String protoData = "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g2gIfZ+EPynbfR8tqJrDPykwStU+LmWcPynkslH8qwO1PyleDNhmcStNPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnn0SzD+7u0Pyk1coX6u27nPympwuCwrGgQPyl7+tolqJ+6PymKWfg2kN2oPynyhUj4M3ehPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAAClGqBpiD1+jPynODLAFPaIoPym6WX8YX3LtPymwWpeKpV+UPym4aSdRMFWWPynkTnUrrMcPPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnbFCTL+qaTPyk+1nNQwFWAPylzLy3dXgN8Pym+akvFTk3uPylA/rHzCQeKPyklUUDIvv95PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkJ3n0oAbG7PynMKc4C1FN7PylnL8vbare0PymAOm8WA3CGPynxjrR4A/HkPym4QFlvzfmsPylDBmvbUw+1PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnk3SMJwryQPykARhXTwiJ8Pym3q4cl2VO/PilCxXHYm3GRPylCPC1c7MuTPym5xV9wBADkPymLqCGbvy/UPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkHQJHdRDiGPymRDpbKVcTtPymyjJiUChOPPykms8k3V/+aPyndduV/96hzPykc3sy4E8yJPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACm6JBRf8UWxPykLv4ZzVU/APynJyzt3bMPpPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmxvr3d+1iQPykAAAAAAAAAACl7sQ+yMaGYPykAAAAAAAAAACnpUinL2OyFPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkeCcu4QOfqPym7/Ke708m7PylTVIU1Q+bkPSmmqseGnsjQPykfstGKRdCOPinyC4Jyg7m6PykaKNFsOM5VPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkZPlmzmDnkPykKSSeCY9k7Pil0sYJa3COoPykdV+rUssihPykOD13SkUYKPyldARZt+7G1PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAAClbOOVZht6vPykAAAAAAAAAACmWEIKNJvqQPylPxxTr1CToPzE7H7DoK1GKPTEomVDd9I8yOjG8lv/////vPzFHRaDI7jLCOzF1FMZ0L/73OjGzst32L/UwODFqyWbI18tbODHU2MJjWALUMzEPzy0bs613OTGdTH3aYhskOzF4iTtlXg6wODkAAAAAAABOQEEAAAAAAHDHQEkAAAAAAAAwQFEAAAAAAAAQQFhNYgdkZWZhdWx0";

        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            Optional<SleepHmmWithInterpretation> ret = Optional.absent();

            BASE64Decoder decoder = new BASE64Decoder();
            try {
                final byte[] decodedBytes = decoder.decodeBuffer(protoData);

                final SleepHmmProtos.SleepHmmModelSet proto = SleepHmmProtos.SleepHmmModelSet.parseFrom(decodedBytes);

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

        Optional<SleepHmmWithInterpretation.SleepHmmResult> res = hmm.get().getSleepEventsUsingHMM(sensorSampleList,motionList,t0 + offset,tf + offset,tc1);
        Optional<SleepHmmWithInterpretation.SleepHmmResult> res2 = hmm.get().getSleepEventsUsingHMM(sensorSampleList,motionList,t0 + offset,tf + offset,tc2);

        TestCase.assertTrue(res.isPresent());
        TestCase.assertTrue(res2.isPresent());

        SleepHmmWithInterpretation.SleepHmmResult r = res.get();

        TestCase.assertTrue(r.sleepEvents.size() == 4);
        boolean foundOne = false;
        for (Event e : r.sleepEvents) {

            if (e.getType() == Event.Type.SLEEP) {
                foundOne = true;
            }
        }

        TestCase.assertTrue(foundOne);

        final int expectedLength1 =  6*60 / NUM_MINUTES_IN_WINDOW;
        final int expectedLength2 =  7*60 / NUM_MINUTES_IN_WINDOW;

        TestCase.assertTrue(res.get().path.size() == expectedLength1);
        TestCase.assertTrue(res2.get().path.size() == expectedLength2);



    }
}