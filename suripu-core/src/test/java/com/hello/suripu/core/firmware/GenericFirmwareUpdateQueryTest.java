package com.hello.suripu.core.firmware;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericFirmwareUpdateQueryTest {

    private final String fwVersion = "abc";
    private final String fwVersion2 = "def";
    private final String group = "group";

    private GenericFirmwareUpdateQuery queryOne;
    private GenericFirmwareUpdateQuery queryOnePrime;
    private GenericFirmwareUpdateQuery queryTwo;

    @Before
    public void setUp() {
        queryOne = new GenericFirmwareUpdateQuery(group, fwVersion, HardwareVersion.SENSE_ONE);
        queryOnePrime = new GenericFirmwareUpdateQuery(group, fwVersion, HardwareVersion.SENSE_ONE);
        queryTwo = new GenericFirmwareUpdateQuery(group, fwVersion2, HardwareVersion.SENSE_ONE);
    }

    @Test
    public void testObjectBehavior() {
        assertThat(queryOne.equals(queryOnePrime), is(true));
        assertThat(queryOne.equals(queryTwo), is(false));
        assertThat(queryOne.hashCode() == queryOnePrime.hashCode(), is(true));
    }

    @Test
    public void testContains() {
        final Set<GenericFirmwareUpdateQuery> queries = Sets.newHashSet(queryOne);
        assertThat(queries.contains(queryOne), is(true));
        assertThat(queries.contains(queryOnePrime), is(true));
    }
}
