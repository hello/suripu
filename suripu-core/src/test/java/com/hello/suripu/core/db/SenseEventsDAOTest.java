package com.hello.suripu.core.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hello.suripu.core.metrics.DeviceEvents;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenseEventsDAOTest {


    @Test public void testTransformEmptyList() {
        final List<DeviceEvents> deviceEventsList = Lists.newArrayList();
        final Multimap<String, String> groupedEvents = SenseEventsDAO.transform(deviceEventsList);
        assertThat(groupedEvents.isEmpty(), is(true));
    }

    @Test public void testTransformCollide() {
        final List<DeviceEvents> deviceEventsList = Lists.newArrayList();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Set<String> s = Sets.newHashSet("hello : world");

        final DeviceEvents deviceEvents = new DeviceEvents("ABC", now, s);
        deviceEventsList.add(deviceEvents);

        final Set<String> s2 = Sets.newHashSet("hello! : world!");
        final DeviceEvents deviceEvents2 = new DeviceEvents("ABC", now, s2);
        deviceEventsList.add(deviceEvents2);


        final Multimap<String, String> groupedEvents = SenseEventsDAO.transform(deviceEventsList);
        assertThat(groupedEvents.asMap().size(), is(1));
        final String key = "ABC|" + now.toString("yyyy-MM-dd HH:mm:ss");
        final Collection<String> res = groupedEvents.get(key);
        assertThat(res == null, is(false));
        assertThat(res.size(), is(2));
    }

    @Test public void testTransformDontCollide() {
        final List<DeviceEvents> deviceEventsList = Lists.newArrayList();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Set<String> s = Sets.newHashSet("hello : world");
        final DeviceEvents deviceEvents = new DeviceEvents("ABC", now, s);
        deviceEventsList.add(deviceEvents);

        final Set<String> s2 = Sets.newHashSet("hello! : world!");
        final DeviceEvents deviceEvents2 = new DeviceEvents("ABC", now.plusSeconds(1), s2);
        deviceEventsList.add(deviceEvents2);


        final Multimap<String, String> groupedEvents = SenseEventsDAO.transform(deviceEventsList);
        assertThat(groupedEvents.size(), is(2));
        final String key = "ABC|" + now.toString("yyyy-MM-dd HH:mm:ss");
        final Collection<String> res = groupedEvents.get(key);
        assertThat(res == null, is(false));
        assertThat(res.size(), is(1));
    }
}
