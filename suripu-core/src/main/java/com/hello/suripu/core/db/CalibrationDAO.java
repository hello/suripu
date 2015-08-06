package com.hello.suripu.core.db;

import com.hello.suripu.core.models.Calibration;

import java.util.Map;
import java.util.Set;


public interface CalibrationDAO {
    /**
     * Retrieves an integer offset to help calibrate incoming dust data
     * @param senseId String
     * @return Integer
     */
    Calibration get(String senseId);

    /**
     *
     * @param senseId String
     */
    void put(String senseId, Integer offset);

    Map<String, Calibration> getBatch(Set<String> senseIds);
}
