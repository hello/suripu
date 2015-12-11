package com.hello.suripu.workers.sense.lastSeen;

import com.google.common.collect.Maps;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class MultiBloomFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLastSeenProcessor.class);

    private final Map<Integer, TimestampedBloomFilter> timestampedBloomFilterMap = Maps.newHashMap();

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

    private void createNewBloomFilter(final int bloomFilterId, final DateTime dt, final int headwaySeconds) {
        this.timestampedBloomFilterMap.put(bloomFilterId, new TimestampedBloomFilter(
                BloomFilter.create(Funnels.stringFunnel(), this.bloomFilterCapacity, this.bloomFilterErrorRate),
                dt.plusSeconds(headwaySeconds)
        ));
    }

    public boolean mightHaveSeen(final String element) {
        final int bloomFilterId = (Math.abs(element.hashCode())) % this.numberOfBloomFilters;
        return this.timestampedBloomFilterMap.get(bloomFilterId).bloomFilter.mightContain(element);
    }

    public void initializeAllBloomFilters(final DateTime dt) {
        for (int j=0; j< this.numberOfBloomFilters; j++) {
            createNewBloomFilter(j, dt, j * this.bloomFilterOffsetSeconds);
            LOGGER.trace("Bloom filter {} created at {}", j, this.timestampedBloomFilterMap.get(j).created);
        }
    }

    public void resetAllBloomExpiredFilters(final DateTime dt) {
        for (int j=0; j< this.numberOfBloomFilters; j++) {
            if(hasExpired(dt, j)) {
                createNewBloomFilter(j, dt, 0);
                LOGGER.trace("Bloom filter {} reseted at {}", j, this.timestampedBloomFilterMap.get(j).created);
            }
        }
    }


    boolean hasExpired(final DateTime dt, final int bloomFilterId) {
        return dt.isAfter(this.timestampedBloomFilterMap.get(bloomFilterId).created.plusSeconds(this.bloomFilterLifeSpanSeconds));
    }

    public void addElement(final String element) {
        final int bloomFilterId = (Math.abs(element.hashCode())) % this.numberOfBloomFilters;
        this.timestampedBloomFilterMap.get(bloomFilterId).bloomFilter.put(element);
    }

    public Integer getBloomFilterLifeSpanSeconds() {
        return this.bloomFilterLifeSpanSeconds;
    }

    public Integer getBloomFilterOffsetSeconds() {
        return this.bloomFilterOffsetSeconds;
    }
    public DateTime getCreated(Integer bloomFilterId) {return this.timestampedBloomFilterMap.get(bloomFilterId).created;}


    private class TimestampedBloomFilter {
        private final BloomFilter<CharSequence> bloomFilter;
        private final DateTime created;
        public TimestampedBloomFilter(final BloomFilter<CharSequence> bloomFilter, final DateTime created) {
            this.bloomFilter = bloomFilter;
            this.created = created;
        }

    }
}
