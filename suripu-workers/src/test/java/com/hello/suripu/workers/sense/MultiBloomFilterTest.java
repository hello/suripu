package com.hello.suripu.workers.sense;

import com.hello.suripu.workers.sense.lastSeen.MultiBloomFilter;
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
        multiBloomFilter = new MultiBloomFilter(2, 2, 1, 4000, 0.05);
    }

    @Test
    public void testAlternateBloomFilterExpiration(){
        multiBloomFilter.initializeAllBloomFilters();
        try {
            Thread.sleep(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000);
            assertThat(multiBloomFilter.hasExpired(0), is(true));
            assertThat(multiBloomFilter.hasExpired(1), is(false));

            multiBloomFilter.resetAllBloomExpiredFilters();

            assertThat(multiBloomFilter.hasExpired(0), is(false));
            assertThat(multiBloomFilter.hasExpired(1), is(false));

            Thread.sleep(multiBloomFilter.getBloomFilterOffsetSeconds() * 1000);

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
    public void testSeenSenses(){
        multiBloomFilter.initializeAllBloomFilters();


        final int lastSeenMinuteOfHour = DateTime.now(DateTimeZone.UTC).getMinuteOfHour();


        multiBloomFilter.addElement("Zer", lastSeenMinuteOfHour);
        multiBloomFilter.addElement("One", lastSeenMinuteOfHour);


        assertThat(multiBloomFilter.mightHaveSeen("Zer", lastSeenMinuteOfHour), is(true));
        assertThat(multiBloomFilter.mightHaveSeen("One", lastSeenMinuteOfHour), is(true));

        try {
            Thread.sleep(multiBloomFilter.getBloomFilterLifeSpanSeconds() * 1000);

            multiBloomFilter.resetAllBloomExpiredFilters();

            // Zer --> has code = 120487, One --> hash code = 110182,
            // so these 2 senses will be put into different bloom filters
            // Hence after the one of the bloom filter is removed, we expect to see only exactly one of them as "might have been seen"

            assertThat(multiBloomFilter.mightHaveSeen("Zer", lastSeenMinuteOfHour)
                    && multiBloomFilter.mightHaveSeen("One", lastSeenMinuteOfHour), is(false));

            assertThat(multiBloomFilter.mightHaveSeen("Zer", lastSeenMinuteOfHour)
                    || multiBloomFilter.mightHaveSeen("One", lastSeenMinuteOfHour), is(true));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
