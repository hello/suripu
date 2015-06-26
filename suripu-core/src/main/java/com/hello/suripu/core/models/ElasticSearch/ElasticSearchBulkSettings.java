package com.hello.suripu.core.models.ElasticSearch;

public class ElasticSearchBulkSettings {
    public final Integer bulkActions;
    public final Integer bulkSizeInMegabyes;
    public final Integer flushIntervalInSeconds;
    public final Integer concurrentRequests;


    public ElasticSearchBulkSettings(final Integer bulkActions, final Integer bulkSizeInMegabyes, final Integer flushIntervalInSeconds, final Integer concurrentRequests) {
        this.bulkActions = bulkActions;
        this.bulkSizeInMegabyes = bulkSizeInMegabyes;
        this.flushIntervalInSeconds = flushIntervalInSeconds;
        this.concurrentRequests = concurrentRequests;
    }
}
