package com.hello.suripu.core.firmware;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenseFirmwareUpdateQueryTest {

    private final String senseId = "sense";
    private final String fwVersion = "abc";
    private final String fwVersion2 = "def";
    private final String group = "group";

    private SenseFirmwareUpdateQuery queryOne;
    private SenseFirmwareUpdateQuery queryOnePrime;
    private SenseFirmwareUpdateQuery queryTwo;

    @Before
    public void setUp() {
        queryOne = SenseFirmwareUpdateQuery.forSense(senseId, group, fwVersion, HardwareVersion.SENSE_ONE);
        queryOnePrime = SenseFirmwareUpdateQuery.forSense(senseId, group, fwVersion, HardwareVersion.SENSE_ONE);
        queryTwo = SenseFirmwareUpdateQuery.forSense(senseId, group, fwVersion2, HardwareVersion.SENSE_ONE);
    }

    @Test
    public void testObjectBehavior() {
        assertThat(queryOne.equals(queryOnePrime), is(true));
        assertThat(queryOne.equals(queryTwo), is(false));
        assertThat(queryOne.hashCode() == queryOnePrime.hashCode(), is(true));
    }

    @Test
    public void testContains() {
        final Set<SenseFirmwareUpdateQuery> queries = Sets.newHashSet(queryOne);
        assertThat(queries.contains(queryOne), is(true));
        assertThat(queries.contains(queryOnePrime), is(true));
    }
}
