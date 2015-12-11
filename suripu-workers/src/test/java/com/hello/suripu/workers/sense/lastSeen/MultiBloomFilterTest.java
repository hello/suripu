package com.hello.suripu.workers.sense.lastSeen;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class MultiBloomFilterTest {
    MultiBloomFilter multiBloomFilter;
    @Before
    public void setUp() {
        multiBloomFilter = new MultiBloomFilter(2, 6, 3, 4000, 0.05);
    }

    @Test
    public void testAlternateBloomFilterExpiration(){
        final DateTime dt = DateTime.now(DateTimeZone.UTC);
        multiBloomFilter.initializeAllBloomFilters(dt);

        final DateTime dtCycleOne = dt.plusMillis(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000 + 1);
        assertThat(multiBloomFilter.hasExpired(dtCycleOne, 0), is(true));
        assertThat(multiBloomFilter.hasExpired(dtCycleOne, 1), is(false));

        multiBloomFilter.resetAllBloomExpiredFilters(dtCycleOne);

        assertThat(multiBloomFilter.hasExpired(dtCycleOne, 0), is(false));
        assertThat(multiBloomFilter.hasExpired(dtCycleOne, 1), is(false));


        final DateTime dtCycleOneAndHalf = dtCycleOne.plusMillis(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);

        assertThat(multiBloomFilter.hasExpired(dtCycleOneAndHalf, 0), is(false));
        assertThat(multiBloomFilter.hasExpired(dtCycleOneAndHalf, 1), is(true));

        multiBloomFilter.resetAllBloomExpiredFilters(dtCycleOneAndHalf);

        assertThat(multiBloomFilter.hasExpired(dtCycleOneAndHalf, 0), is(false));
        assertThat(multiBloomFilter.hasExpired(dtCycleOneAndHalf, 1), is(false));
    }

    @Test
    public void testMemorizeSenseIds(){
        final DateTime dt = DateTime.now(DateTimeZone.UTC);
        multiBloomFilter.initializeAllBloomFilters(dt);

        // Zer --> has code = 120487, One --> hash code = 110182,
        multiBloomFilter.addElement("Zer");
        multiBloomFilter.addElement("One");

        // Expectation is Zer is added to bloom filter #1 and One is added to bloom filter #0
        assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(true));
        assertThat(multiBloomFilter.mightHaveSeen("One"), is(true));


        // At 1 cycle, bloom filter #0 got reset first
        final DateTime dtCycleOne = dt.plusMillis(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000 + 1);
        multiBloomFilter.resetAllBloomExpiredFilters(dtCycleOne);

        // Thus we expect to forget One but not Zer
        assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(true));
        assertThat(multiBloomFilter.mightHaveSeen("One"), is(false));

        // We now need to memorize One again
        multiBloomFilter.addElement("One");

        // At 1 cycle and half, bloom filter #1 got reset in turn
        final DateTime dtCycleOneAndHalf = dtCycleOne.plusMillis(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);
        multiBloomFilter.resetAllBloomExpiredFilters(dtCycleOneAndHalf);

        // Thus we expect to forget Zer but not One
        assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(false));
        assertThat(multiBloomFilter.mightHaveSeen("One"), is(true));

        // Let say we don't memorize anymore to see what happen at 2 cycles
        final DateTime dtCycleTwo = dtCycleOneAndHalf.plusMillis(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);
        multiBloomFilter.resetAllBloomExpiredFilters(dtCycleTwo);

        // Both Zer and One are forgotten then
        assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(false));
        assertThat(multiBloomFilter.mightHaveSeen("One"), is(false));
    }
}
