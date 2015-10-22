package com.hello.suripu.core.db;

import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;

/**
 * Created by benjo on 10/17/15.
 */
public interface DefaultModelEnsembleDAO {
    public OnlineHmmPriors getDefaultModelEnsemble();
    public OnlineHmmPriors getSeedModel();
}
