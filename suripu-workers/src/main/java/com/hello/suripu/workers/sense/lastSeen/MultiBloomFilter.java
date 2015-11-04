package com.hello.suripu.workers.sense.lastSeen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class MultiBloomFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLastSeenProcessor.class);

    private final Map<Integer, BloomFilter<CharSequence>> bloomFilterMap = Maps.newHashMap();
    private final Map<Integer, DateTime> bloomFilterCreatedMap = Maps.newHashMap();
    private final Integer numberOfBloomFilters;
    private final Integer bloomFilterLifeSpanSeconds;
    private final Integer bloomFilterOffsetSeconds;
    private final Integer bloomFilterCapacity;
    private final double bloomFilterErrorRate;

    public MultiBloomFilter(final Integer numberOfBloomFilters, final Integer bloomFilterLifeSpanSeconds, final Integer bloomFilterOffsetSeconds, final Integer bloomFilterCapacity, final double bloomFilterErrorRate) {
        this.numberOfBloomFilters = numberOfBloomFilters;
        this.bloomFilterLifeSpanSeconds = bloomFilterLifeSpanSeconds;
        this.bloomFilterOffsetSeconds = bloomFilterOffsetSeconds;
        this.bloomFilterCapacity = bloomFilterCapacity;
        this.bloomFilterErrorRate = bloomFilterErrorRate;
    }

    private void createNewBloomFilter(final int bloomFilterId, final int headwaySeconds) {
        this.bloomFilterMap.put(bloomFilterId, BloomFilter.create(Funnels.stringFunnel(), this.bloomFilterCapacity, this.bloomFilterErrorRate));
        this.bloomFilterCreatedMap.put(bloomFilterId, DateTime.now(DateTimeZone.UTC).plusSeconds(headwaySeconds));
    }

    public boolean mightHaveSeen(final String senseExternalId, final int lastSeenMinuteOfHour) {
        final int bloomFilterId = (Math.abs(senseExternalId.hashCode()) + lastSeenMinuteOfHour) % this.numberOfBloomFilters;
        return this.bloomFilterMap.get(bloomFilterId).mightContain(senseExternalId);
    }

    public void initializeAllBloomFilters() {
        for (int j=0; j< this.numberOfBloomFilters; j++) {
            createNewBloomFilter(j, j * this.bloomFilterOffsetSeconds);
            LOGGER.trace("Bloom filter {} created at {}", j, this.bloomFilterCreatedMap.get(j));
        }
    }

    public void resetAllBloomExpiredFilters() {
        for (int j=0; j< this.numberOfBloomFilters; j++) {
            if(hasExpired(j)) {
                createNewBloomFilter(j, 0);
                LOGGER.trace("Bloom filter {} reseted at {}", j, this.bloomFilterCreatedMap.get(j));
            }
        }
    }

    @VisibleForTesting
    public boolean hasExpired(final int bloomFilterId) {
        return DateTime.now(DateTimeZone.UTC).isAfter(this.bloomFilterCreatedMap.get(bloomFilterId).plusSeconds(this.bloomFilterLifeSpanSeconds));
    }

    public void addElement(final String senseExternalId, int lastSeenMinuteOfHour) {
        final int bloomFilterId = (Math.abs(senseExternalId.hashCode()) + lastSeenMinuteOfHour) % this.numberOfBloomFilters;
        this.bloomFilterMap.get(bloomFilterId).put(senseExternalId);
    }

    public Integer getBloomFilterLifeSpanSeconds() {
        return this.bloomFilterLifeSpanSeconds;
    }

    public Integer getBloomFilterOffsetSeconds() {
        return this.bloomFilterOffsetSeconds;
    }
    public DateTime getCreated(Integer bloomFilterId) {
        return this.bloomFilterCreatedMap.get(bloomFilterId);
    }

}
