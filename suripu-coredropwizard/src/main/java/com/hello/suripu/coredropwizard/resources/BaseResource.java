package com.hello.suripu.coredropwizard.resources;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.analytics.AnalyticsTracker;
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


    @Inject
    public AnalyticsTracker analyticsTracker;

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

    protected Boolean hasColorCompensationEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.COMPENSATE_LIGHT_WITH_SENSE_COLOR, accountId, Collections.EMPTY_LIST);
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


    protected Boolean isSenseLastSeenDynamoDBReadEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SENSE_LAST_SEEN_VIEW_DYNAMODB_READ, accountId, Collections.EMPTY_LIST);
    }


    protected Boolean isSensorsViewUnavailable(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.VIEW_SENSORS_UNAVAILABLE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean isTimelineViewUnavailable(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.VIEW_TIMELINE_UNAVAILABLE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean isTrendsViewUnavailable(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.VIEW_TRENDS_UNAVAILABLE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasDelayCurrentRoomStateThreshold(final Long accountId) {
        return  featureFlipper.userFeatureActive(FeatureFlipper.DELAY_CURRENT_ROOM_STATE_THRESHOLD,accountId,Collections.EMPTY_LIST);
    }

    // Calibration is enabled on a per device basis
    protected Boolean hasCalibrationEnabled(final String senseId) {
        return featureFlipper.deviceFeatureActive(FeatureFlipper.CALIBRATION, senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasDustSmoothEnabled(final String senseId) {
        return featureFlipper.deviceFeatureActive(FeatureFlipper.DUST_SMOOTH, senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasInvalidSleepScoreFromFeedbackChecking(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_EVENT_SLEEP_SCORE_ENFORCEMENT, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasDeviceDataDynamoDBEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.DYNAMODB_DEVICE_DATA, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useAudioPeakEnergy(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.AUDIO_PEAK_ENERGY_DB, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepSoundsEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SOUNDS_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepSoundsDisplayFirmwareUpdate(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SOUNDS_DISPLAY_FW_UPDATE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasQuestionSurveyProcessorEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.QUESTION_SURVEY_PROCESSOR_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasQuestionCoreProcessorEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.QUESTION_CORE_PROCESSOR_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasTimelineProcessorV3(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_PROCESSOR_V3_ENABLED, accountId, Collections.EMPTY_LIST);
    }
}
