package com.hello.suripu.core.resources;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.librato.rollout.RolloutClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

public class BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseResource.class);

    @Inject
    RolloutClient featureFlipper;

    protected BaseResource()  {
        // Constructor DI will make unit testing hard: http://www.javaranch.com/journal/200709/dependency-injection-unit-testing.html
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

    protected Boolean hasVotingEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.VOTING_ALGORITHM, accountId, Collections.EMPTY_LIST);
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

    public void throwPlainTextError(final Response.Status status, final String message) throws WebApplicationException {
        plainTextError(status, message);
    }

    // TODO: add similar method for JSON Error

    /**
     * Returns the first IP address specified in headers or empty string
     * @param request
     * @return
     */
    public static String getIpAddress(final HttpServletRequest request) {
        final String ipAddress = (request.getHeader("X-Forwarded-For") == null) ? request.getRemoteAddr() : request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            return "";
        }

        final String[] ipAddresses = ipAddress.split(",");
        return ipAddresses[0]; // always return first one?
    }


    protected Boolean isSensorsDBUnavailable(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SENSORS_DB_UNAVAILABLE, accountId, Collections.EMPTY_LIST);
    }

}
