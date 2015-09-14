package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created by benjo on 7/7/15.
 */
public interface FeatureExtractionModelsDAO {
    public FeatureExtractionModelData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, Optional<UUID> uuidForLogger);
}
