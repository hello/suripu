package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.NeuralNetProtos;

import java.util.List;

/**
 * Created by benjo on 2/23/16.
 */
public interface NeuralNetDAO {
    Optional<NeuralNetProtos.NeuralNetMessage> getNetDataById(final String id);
    List<String> getAvailableIds();
}
