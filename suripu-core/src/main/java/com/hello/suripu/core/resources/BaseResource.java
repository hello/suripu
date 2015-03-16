package com.hello.suripu.core.resources;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.flipper.FlipperParams;
import com.librato.rollout.RolloutClient;

import javax.inject.Inject;
import java.util.Collections;

public class BaseResource {

    @Inject
    RolloutClient featureFlipper;

    protected BaseResource()  {
        ObjectGraphRoot.getInstance().inject(this);
    }


    /**
     * Changes the default value for missing data when generating graphs
     * Will have to be removed once everyone has migrated to newer app versions
     *
     * @param accountId
     * @return
     */
    protected Integer missingDataDefaultValue(final Long accountId) {
        boolean active = featureFlipper.userFeatureActive(FeatureFlipper.MISSING_DATA_DEFAULT_VALUE, accountId, Collections.EMPTY_LIST);
        return (active) ? -1 : 0;
    }

    protected FlipperParams getFlipperParams(final Long accountId){
        return FlipperParams.create(accountId, this.featureFlipper);
    }

}
