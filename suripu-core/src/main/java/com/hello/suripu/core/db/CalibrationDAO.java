package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Calibration;

import java.util.Map;
import java.util.Set;


public interface CalibrationDAO {
    /**
     * Retrieves calibration per sense
     * @param senseId String
     * @return Calibration
     */
    Optional<Calibration> get(String senseId);
    Optional<Calibration> getStrict(String senseId);

    /**
     *
     * @param senseId String
     */
    void put(String senseId, Integer dustOffset, String metadata);

    /**
     * Retrieves a map of sense Id to calibration object
     * @param senseIds Set<String>
     * @return Map<String, Calibration>
     */
    Map<String, Calibration> getBatch(Set<String> senseIds);
    Map<String, Calibration> getBatchStrict(Set<String> senseIds);
}
