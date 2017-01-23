package com.hello.suripu.core.processors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.hello.suripu.core.metrics.DeviceEvents;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RingProcessorBugTest {
    private final static String SENSE_ID1 = "9A730831AF0DB34D";
    private final static String SENSE_ID_CRASH = "9A730831AF0DB34E";


    private final SenseEventsDAO senseEventsDAO = new SenseEventsDAO() {
        @Override
        public List<DeviceEvents> get(String deviceId, DateTime start, Integer limit) {
            return null;
        }

        @Override
        public List<DeviceEvents> get(String deviceId, DateTime start) {
            return null;
        }

        @Override
        public List<DeviceEvents> getAlarms(String deviceId, DateTime start, DateTime end) {
            if (deviceId == SENSE_ID1) {
                final DeviceEvents deviceEvent = new DeviceEvents(SENSE_ID1, start,  Sets.newHashSet("alarm:ring"));
                final List<DeviceEvents> deviceEvents = new ArrayList<>();
                deviceEvents.add(deviceEvent);
                return deviceEvents;
            }
            return new ArrayList();
        }

        @Override
        public Integer write(List<DeviceEvents> deviceEventsList) {
            return null;
        }
    };


    @Test public void testSmartAlarmNeverTriggers() throws IOException {

        final String alarm1String = "[{\"year\":0,\"month\":0,\"day_of_month\":0,\"hour\":5,\"minute\":40,\"day_of_week\":[1,2,3,4,5],\"repeated\":true,\"enabled\":true,\"editable\":true,\"smart\":true,\"sound\":{\"id\":7,\"name\":\"Bounce\",\"url\":\"https://hello-audio.s3.amazonaws.com/ringtones/Bounce.mp3?x-amz-security-token=FQoDYXdzELv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDExoRnU%2FEAbEsz0bbyKcAykAo2mvQP6By2jgx8dooZdj%2FHMBjdDzlB1jIi2FR%2FTrdW7Bcc36q9erIwSJwlETzPDBU5GPWKJZ7rUu7kM3fht7tgSatvlLFUCbGWOkIr3yBv8M4FCswc5RmyyZObTZkRKhCdYtAeQD4JBHg2sApTS%2F8LwU3B0NX6JZpUHjYPw9w0KlcjDQdNPyFHubWOGMQcGcuHn4j0JfLBZS4dV%2Fj%2F02fgsf1accBxGyOCWS7pAjR3WIAK71XqjMLU%2FAo3nCoq6HI%2BaL87w0nvmPfTPkl1%2FCtoBBFG9FhX%2BKWVquMu2VtuiAvRTxk%2BTc7pDClqKo0E9KJ2zH2CGHFFXoAWj6bxTyq6ebV%2F7YnIcg7yoY5jiE6Klru473ebmLYwdQksBZxt%2BZ2DG7%2B7S0kjGyC2svAwY9Y7oG%2B9KIjAfnnG%2Fjo5Kn%2FJ8nQmuUry1iUJ9sWO4GvW11Rsv937kUwzcoAP5KbiLAGmdnGdrizKFiyYmeNcWHy2qb1JMELnJrTXpAmZPY6XzcxQTi99cErEGzlQJ%2FYXrE%2F0Lcmp90IIF5zhgo8qjxwAU%3D&AWSAccessKeyId=ASIAIH52UPHIRAV5P4UQ&Expires=1478857739&Signature=O%2Fkzlum%2BlIGu1Qj96lVab8nmluA%3D\"},\"id\":\"1e91dfbc-0534-4228-a14d-d2b3e75010d1\",\"source\":\"MOBILE_APP\",\"expansions\":[]},{\"year\":2016,\"month\":12,\"day_of_month\":18,\"hour\":7,\"minute\":30,\"day_of_week\":[7],\"repeated\":true,\"enabled\":false,\"editable\":true,\"smart\":true,\"sound\":{\"id\":5,\"name\":\"Dusk\",\"url\":\"https://hello-audio.s3.amazonaws.com/ringtones/Dusk.mp3?x-amz-security-token=FQoDYXdzECMaDHo71M7v9GOTrMvrvCK3AysT4e6Gj0q6MHdFeR%2BESj%2FAv1iNKL%2F7yrFSpdIZ2lYmrCPRhReVVIFgbzinHdBOHa1W9YZk3Mon5%2FVLoHei%2BU%2BEdLecECxJ1oX5iUyhytwyLdHsl8m%2FJoeMv2IP8%2FyGtKa9P2%2FmnZQet2G7dQ25GE%2BmHSHQlNaWvaZiYI6AcMIhNE6K8cUt4L3jhZ%2BtWRnQIGPWZ5pOnOwIw2vwiO1MubmBow12k9ijnX8A3kmlgLRmqmMYxDrePWthrHFPG8FEOU8VdyWnGFntejIMi8mg7IinXJy6HTBZvy9oQDsotBdUm7MYmkeSVtDOt3bNicsM%2FKgbaZfOdcuJfBIvOJmvD4o7twMNPOD5p4zU3hD8JpnA5kdFvJrqJoqVeXmFAGKcWMdzKJwm8xyRBgZsVz9r8IgGWYASFO0ciY9AZnhd6VoF%2BHfNE2oeD1UxUelRUj5qQdG%2B%2BrqxB7iFRAdN4CLGnZkWj5987tqZ1bbHnJx2yfNOyShaunEqZi1OHMBlQSKAWpTlVdWhQSlFh%2FiE77qkhlo2F5N%2F4t4TX%2FZ0iBMGFUH13p81HRi4oxToYoe5hRn%2Fde1nB%2F0JHP0o%2B9nXwgU%3D&AWSAccessKeyId=ASIAIZROIGZHNUNEUSXQ&Expires=1482635718&Signature=TM2PATcKP3Lga4SzR%2Fjb917IWaY%3D\"},\"id\":\"9567877d-5fab-4b20-8bab-b1f46d507f68\",\"source\":\"MOBILE_APP\",\"expansions\":[]},{\"year\":2016,\"month\":12,\"day_of_month\":24,\"hour\":7,\"minute\":30,\"day_of_week\":[],\"repeated\":false,\"enabled\":false,\"editable\":true,\"smart\":true,\"sound\":{\"id\":5,\"name\":\"Dusk\",\"url\":\"https://hello-audio.s3.amazonaws.com/ringtones/Dusk.mp3?x-amz-security-token=FQoDYXdzELT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDCd5SyHW0TZ3NWmTxCK3A4NgmsoSgy4Kzol%2BYD7EXV16U5G2cFR35zPgEXj9v3UI0xd40U%2FMjXDmeNw18rtIIEKYyT9iRsJ%2BznWZYxluI2opile5aTI6Mkqgy7nQ5tMB8sQwIT%2FSA4q%2B4zZGl0TsRNQBQ%2BfKe8brKcoOy8K2YDfokVvjGz0AXaPiJWaifOrZpcWcJui8RA%2Fgm4H0FSt8ZxgtWavIIW%2BlyI9ksTawkXEy1Lpn5fRtaU4vviEt0zsaJU2aK5%2F59MYnhoyv7Fr9Yh1at%2BIDtXtV9N0zFlmvD26SNvyLTQt2R6y%2Fjqp4lfs%2FUGrjAlfegYtiuiqmpvcIAqT3sAknD94sSzY%2FWKL%2BbIBEDNkG8Q6%2FacyBGiO%2B%2FUeOdyhQInQx%2B740oLgLKfQsfNXNSiWu2OCMdTZQ1H4xhrdR4PSuoNm2hs0Ic5gB0SgPTjjY2JyDc8K6cDJ1Y7isyZ5aL1OzlFwLrR%2BlJZY3FVyhY7l%2B7VK%2FKkWBTSFRcgtuct9palp79WysiHHfYDJ0X%2BWGeaQ5TpCVfJ7bM%2BxSkHhyH0T5jJqmtHe1JODY%2BP5gUCKKyPuHxy1IWP9DAKX7NV97py719W4ohsL3wgU%3D&AWSAccessKeyId=ASIAJG54LMIZA3362WXA&Expires=1483155321&Signature=dqNmdBQRMl%2FMApddhxr9gFZh5Bc%3D\"},\"id\":\"1a9d8dc8-7f25-42c5-916d-752bd51fdd9e\",\"source\":\"MOBILE_APP\",\"expansions\":[]}]";
        final String alarm2String = "[{\"year\":2016,\"month\":9,\"day_of_month\":19,\"hour\":6,\"minute\":15,\"day_of_week\":[],\"repeated\":false,\"enabled\":true,\"editable\":true,\"smart\":true,\"sound\":{\"id\":5,\"name\":\"Dusk\",\"url\":\"\"},\"id\":\"906B5EFB-0FFE-4390-9BAD-6C9467502006\",\"source\":\"MOBILE_APP\"}]";
        final ObjectMapper mapper = new ObjectMapper();
        final List<Alarm> alarmList = mapper.readValue(alarm1String, new TypeReference<List<Alarm>>() { });
        final List<Alarm> alarmList2 = mapper.readValue(alarm2String, new TypeReference<List<Alarm>>() { });
        final boolean hasRecentAlarm = true;


        /*
        an 10 02:25:44 ip-10-0-1-54 suripu-workers-alarm-prod:  INFO  [2017-01-10 10:25:43,811] com.hello.suripu.core.processors.RingProcessor: action=set-smart-progressive-alarm account_id=45699 device_id=9A730831AF0DB34D original_ring_time=2017-01-10T05:40:00.000-05:00 updated_ring_time 2017-01-10T05:27:00.000-05:00
         */


        final long[] soundIds = new long[]{};
        final RingTime ringTime = new RingTime(
                1484044800000L, // 5:40 Jan 10th
                1484044800000L,
                soundIds,
                true
        );


        final RingTime ringTime2 = new RingTime(
                1474278420000L,
                1474280100000L,
                soundIds,
                true
        );


        final UserInfo userInfo = new UserInfo(
                SENSE_ID1,
                45699L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/New_York")),
                Optional.absent(),
                0L
        );


        final UserInfo userInfo2 = new UserInfo(
                SENSE_ID1,
                56478L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/New_York")),
                Optional.absent(),
                0L
        );



        final List<UserInfo> userInfos = Lists.newArrayList(userInfo, userInfo2);
        final DateTime beforeWorkerUpdatedIt = new DateTime(2017,1,10, 10,20,0, DateTimeZone.UTC);
        final RingTime computedRingtime = RingProcessor.getNextRingTimeForSense(SENSE_ID1, userInfos, beforeWorkerUpdatedIt, hasRecentAlarm, senseEventsDAO);
        assertEquals(computedRingtime.actualRingTimeUTC, ringTime.actualRingTimeUTC);



        final RingTime ringTimeUpdated = new RingTime(
                1484044020000L, // Human time (GMT): Tue, 10 Jan 2017 10:27:00 GMT
                1484044800000L,
                soundIds,
                true
        );

        final UserInfo userInfoUpdated = new UserInfo(
                SENSE_ID1,
                45699L,
                alarmList,
                Optional.of(ringTimeUpdated),
                Optional.of(DateTimeZone.forID("America/New_York")),
                Optional.absent(),
                0L
        );


        final UserInfo userInfoCrashedUpdated = new UserInfo(
                SENSE_ID_CRASH,
                45699L,
                alarmList,
                Optional.of(ringTimeUpdated),
                Optional.of(DateTimeZone.forID("America/New_York")),
                Optional.absent(),
                0L
        );



        final UserInfo userInfoCrashed2 = new UserInfo(
                SENSE_ID_CRASH,
                56478L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/New_York")),
                Optional.absent(),
                0L
        );


        userInfos.set(0, userInfoUpdated);
        final DateTime afterWorkerUpdatedIt = new DateTime(2017,1,10, 10,25,0, DateTimeZone.UTC);

        final RingTime computedRingTime2 = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, afterWorkerUpdatedIt, hasRecentAlarm, senseEventsDAO, true);
        assertEquals("after adjusted", ringTimeUpdated.actualRingTimeUTC, computedRingTime2.actualRingTimeUTC);


        final DateTime afterWorkerUpdatedItAndAfterSupposedToRing = new DateTime(2017,1,10, 10,29,0, DateTimeZone.UTC);

        final RingTime computedRingTime3a = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, afterWorkerUpdatedItAndAfterSupposedToRing, hasRecentAlarm, senseEventsDAO, true);
        assertEquals("after adjusted and actual ring time", 0, computedRingTime3a.actualRingTimeUTC);

        //if sense crashed without ringing
        userInfos.set(0, userInfoCrashedUpdated);
        userInfos.set(1, userInfoCrashed2);

        final RingTime computedRingTime3b = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID_CRASH, userInfos, afterWorkerUpdatedItAndAfterSupposedToRing, false, senseEventsDAO, true);
        assertEquals("after adjusted and crashed without ringing", ringTimeUpdated.actualRingTimeUTC, computedRingTime3b.actualRingTimeUTC); //should give the previous ring time to ring immediately

        userInfos.set(0, userInfoUpdated);
        userInfos.set(1, userInfo2);

        //but sense may have crashed with ringing
        final RingTime computedRingTime3c = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, afterWorkerUpdatedItAndAfterSupposedToRing, false, senseEventsDAO, true);
        assertEquals("after adjusted and crashed with ringing", 0, computedRingTime3c.actualRingTimeUTC);


        final DateTime expectedRingTime = new DateTime(2017,1,10, 10,40,0, DateTimeZone.UTC);

        final RingTime computedRingTime4 = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, expectedRingTime, hasRecentAlarm, senseEventsDAO, true);
        assertEquals("expected ring time", 0, computedRingTime4.expectedRingTimeUTC);

        final DateTime expectedRingTime2 = new DateTime(2017,1,10, 10,40,20, DateTimeZone.UTC);

        final RingTime computedRingTime5 = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, expectedRingTime2,hasRecentAlarm, senseEventsDAO, true);
        assertEquals("expected ring time + 20seconds", 0, computedRingTime5.expectedRingTimeUTC);

        final DateTime afterExpectedRingtime = new DateTime(2017,1,10, 10,41,01, DateTimeZone.UTC);

        final RingTime computedRingTime6 = RingProcessor.getNextRingTimeForSenseWithFutureAlarm(SENSE_ID1, userInfos, afterExpectedRingtime, hasRecentAlarm, senseEventsDAO, true);
        assertEquals("expected ring time + 1 minute", new DateTime(2017,1,11,10,40, DateTimeZone.UTC).getMillis(), computedRingTime6.expectedRingTimeUTC);


    }
}
