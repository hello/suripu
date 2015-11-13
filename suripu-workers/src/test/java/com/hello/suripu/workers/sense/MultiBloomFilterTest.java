package com.hello.suripu.workers.sense;

import com.hello.suripu.workers.sense.lastSeen.MultiBloomFilter;
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
        multiBloomFilter.initializeAllBloomFilters();
        try {
            Thread.sleep(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000 + 1);
            assertThat(multiBloomFilter.hasExpired(0), is(true));
            assertThat(multiBloomFilter.hasExpired(1), is(false));

            multiBloomFilter.resetAllBloomExpiredFilters();

            assertThat(multiBloomFilter.hasExpired(0), is(false));
            assertThat(multiBloomFilter.hasExpired(1), is(false));

            Thread.sleep(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);

            assertThat(multiBloomFilter.hasExpired(0), is(false));
            assertThat(multiBloomFilter.hasExpired(1), is(true));

            multiBloomFilter.resetAllBloomExpiredFilters();

            assertThat(multiBloomFilter.hasExpired(0), is(false));
            assertThat(multiBloomFilter.hasExpired(1), is(false));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMemorizeSenseIds(){
        multiBloomFilter.initializeAllBloomFilters();

        // Zer --> has code = 120487, One --> hash code = 110182,
        multiBloomFilter.addElement("Zer");
        multiBloomFilter.addElement("One");

        // Expectation is Zer is added to bloom filter #1 and One is added to bloom filter #0
        assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(true));
        assertThat(multiBloomFilter.mightHaveSeen("One"), is(true));

        try {

            // At 1 cycle, bloom filter #0 got reset first
            Thread.sleep(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000 + 1);
            multiBloomFilter.resetAllBloomExpiredFilters();

            // Thus we expect to forget One but not Zer
            assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(true));
            assertThat(multiBloomFilter.mightHaveSeen("One"), is(false));

            // We now need to memorize One again
            multiBloomFilter.addElement("One");

            // At 1 cycle and half, bloom filter #1 got reset in turn
            Thread.sleep(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);
            multiBloomFilter.resetAllBloomExpiredFilters();

            // Thus we expect to forget Zer but not One
            assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(false));
            assertThat(multiBloomFilter.mightHaveSeen("One"), is(true));

            // Let say we don't memorize anymore to see what happen at 2 cycles
            Thread.sleep(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000 + 1);
            multiBloomFilter.resetAllBloomExpiredFilters();

            // Both Zer and One are forgotten then
            assertThat(multiBloomFilter.mightHaveSeen("Zer"), is(false));
            assertThat(multiBloomFilter.mightHaveSeen("One"), is(false));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
