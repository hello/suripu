package com.hello.suripu.core.resources;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.librato.rollout.RolloutClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

public class BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseResource.class);

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

    protected Boolean hasHmmEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.HMM_ALGORITHM, accountId, Collections.EMPTY_LIST);
    }

    /**
     * Use this method to return plain text errors (to Sense)
     * It returns byte[] just to match the signature of most methods interacting with Sense
     * @param status
     * @param message
     * @return
     */
    protected byte[] plainTextError(final Response.Status status, final String message) {
        LOGGER.error("{} : {} ", status, (message.isEmpty()) ? "-" : message);
        throw new WebApplicationException(Response.status(status)
                .entity(message)
                .type(MediaType.TEXT_PLAIN_TYPE).build()
        );
    }

    protected void throwPlainTextError(final Response.Status status, final String message) throws WebApplicationException {
        plainTextError(status, message);
    }

    // TODO: add similar method for JSON Error
}
