package com.hello.suripu.app.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.hello.suripu.api.input.State;
import com.hello.suripu.app.messeji.MessejiClient;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.SenseStateDynamoDB;
import com.hello.suripu.core.db.sleep_sounds.DurationDAO;
import com.hello.suripu.core.models.sleep_sounds.DurationMap;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.SenseStateAtTime;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundStatus;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.hello.suripu.core.models.sleep_sounds.SoundMap;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.SleepSoundsProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v2/sleep_sounds")
public class SleepSoundsResource extends BaseResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepSoundsResource.class);

    // Fade in/out sounds over this many seconds on Sense
    private static final Integer FADE_IN = 1;
    private static final Integer FADE_OUT = 1;

    private final DurationDAO durationDAO;
    private final SenseStateDynamoDB senseStateDynamoDB;
    private final DeviceDAO deviceDAO;
    private final MessejiClient messejiClient;
    private final SleepSoundsProcessor sleepSoundsProcessor;

    private SleepSoundsResource(final DurationDAO durationDAO,
                                final SenseStateDynamoDB senseStateDynamoDB,
                                final DeviceDAO deviceDAO,
                                final MessejiClient messejiClient,
                                final SleepSoundsProcessor sleepSoundsProcessor)
    {
        this.durationDAO = durationDAO;
        this.senseStateDynamoDB = senseStateDynamoDB;
        this.deviceDAO = deviceDAO;
        this.messejiClient = messejiClient;
        this.sleepSoundsProcessor = sleepSoundsProcessor;
    }

    public static SleepSoundsResource create(final DurationDAO durationDAO,
                                             final SenseStateDynamoDB senseStateDynamoDB,
                                             final DeviceDAO deviceDAO,
                                             final MessejiClient messejiClient,
                                             final SleepSoundsProcessor sleepSoundsProcessor)
    {
        return new SleepSoundsResource(durationDAO, senseStateDynamoDB, deviceDAO, messejiClient, sleepSoundsProcessor);
    }


    //region play
    protected static class PlayRequest {

        @JsonProperty("sound")
        @NotNull
        public final Long soundId;

        @JsonProperty("duration")
        @NotNull
        public final Long durationId;

        @JsonProperty("order")
        @NotNull
        public final Long order;

        @JsonProperty("volume_percent")
        @NotNull
        @Min(0)
        @Max(100)
        public final Integer volumePercent;

        private PlayRequest(final Long soundId, final Long durationId, final Long order, final Integer volumePercent) {
            this.soundId = soundId;
            this.durationId = durationId;
            this.order = order;
            this.volumePercent = volumePercent;
        }

        @JsonCreator
        public static PlayRequest create(@JsonProperty("sound") final Long soundId,
                                         @JsonProperty("duration") final Long durationId,
                                         @JsonProperty("order") final Long order,
                                         @JsonProperty("volume_percent") final Integer volumePercent)
        {
            return new PlayRequest(soundId, durationId, order, volumePercent);
        }
    }

    @Timed
    @POST
    @Path("/play")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response play(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                         @Valid final PlayRequest playRequest)
    {
        final Optional<Duration> durationOptional = durationDAO.getById(playRequest.durationId);
        if (!durationOptional.isPresent()) {
            LOGGER.warn("dao=durationDAO method=getById id={} error=not-found", playRequest.durationId);
            return invalid_request("invalid duration id");
        }

        final Long accountId = accessToken.accountId;

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            return invalid_request("no device pair found");
        }

        final String senseId = deviceIdPair.get().externalDeviceId;

        final Optional<Sound> soundOptional = sleepSoundsProcessor.getSound(senseId, playRequest.soundId);
        if (!soundOptional.isPresent()) {
            return invalid_request("invalid sound id");
        }

        // Send to Messeji
        final Integer volumeScalingFactor = convertVolumePercent(playRequest.volumePercent);
        final Optional<Long> messageId = messejiClient.playAudio(
                senseId, MessejiClient.Sender.fromAccountId(accountId), playRequest.order,
                durationOptional.get(), soundOptional.get(), FADE_IN, FADE_OUT, volumeScalingFactor);

        if (messageId.isPresent()) {
            LOGGER.debug("messeji-status=success message-id={} sense-id={}", messageId.get(), senseId);
            return Response.status(Response.Status.ACCEPTED).entity("").build();
        } else {
            LOGGER.error("messeji-status=failure sense-id={}", senseId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("").build();
        }
    }
    //endregion play


    //region stop
    private static class StopRequest {
        @JsonProperty("order")
        @NotNull
        public final Long order;

        private StopRequest(final Long order) {
            this.order = order;
        }

        @JsonCreator
        public static StopRequest create(@JsonProperty("order") final Long order) {
            return new StopRequest(order);
        }
    }

    @Timed
    @POST
    @Path("/stop")
    public Response stop(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                         @Valid final StopRequest stopRequest) {
        final Long accountId = accessToken.accountId;

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            return Response.status(Response.Status.BAD_REQUEST).entity("").build();
        }

        final String senseId = deviceIdPair.get().externalDeviceId;

        final Optional<Long> messageId = messejiClient.stopAudio(
                senseId, MessejiClient.Sender.fromAccountId(accountId), stopRequest.order, FADE_OUT);
        if (messageId.isPresent()) {
            return Response.status(Response.Status.ACCEPTED).entity("").build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("").build();
        }
    }
    //endregion stop


    //region combinedState
    private class CombinedState {
        @JsonProperty("availableDurations")
        @NotNull
        public final DurationResult durationResult;

        @JsonProperty("availableSounds")
        @NotNull
        public final SleepSoundsProcessor.SoundResult soundResult;

        @JsonProperty("status")
        @NotNull
        public final SleepSoundStatus sleepSoundStatus;

        public CombinedState(final DurationResult durationResult, final SleepSoundsProcessor.SoundResult soundResult, final SleepSoundStatus sleepSoundStatus) {
            this.durationResult = durationResult;
            this.soundResult = soundResult;
            this.sleepSoundStatus = sleepSoundStatus;
        }
    }

    @Timed
    @GET
    @Path("combined_state")
    @Produces(MediaType.APPLICATION_JSON)
    public CombinedState getCombinedState(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final Long accountId = accessToken.accountId;
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final String senseId = deviceIdPair.get().externalDeviceId;

        final SleepSoundsProcessor.SoundResult soundResult = getSounds(accountId, senseId);
        final DurationResult durationResult = new DurationResult(durationDAO.all());
        final SleepSoundStatus sleepSoundStatus = getStatus(accountId, senseId, soundResult, durationResult);
        return new CombinedState(durationResult, soundResult, sleepSoundStatus);
    }

    //endregion combinedState


    //region sounds
    private SleepSoundsProcessor.SoundResult getSounds(final Long accountId, final String senseId) {
        final SleepSoundsProcessor.SoundResult result = sleepSoundsProcessor.getSounds(accountId, senseId);

        if (result.state == SleepSoundsProcessor.SoundResult.State.FEATURE_DISABLED) {
            throw new WebApplicationException(Response.Status.NO_CONTENT);
        }

        return result;
    }

    @Timed
    @GET
    @Path("/sounds")
    @Produces(MediaType.APPLICATION_JSON)
    public SleepSoundsProcessor.SoundResult getSounds(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final Long accountId = accessToken.accountId;
        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final String senseId = deviceIdPair.get().externalDeviceId;
        return getSounds(accountId, senseId);
    }
    //endregion sounds


    //region durations
    private class DurationResult implements DurationMap {
        @JsonProperty("durations")
        @NotNull
        public final List<Duration> durations;

        public DurationResult(final List<Duration> durations) {
            this.durations= durations;
        }

        @Override
        public Optional<Duration> getDurationBySeconds(Integer durationSeconds) {
            for (final Duration duration: durations) {
                if (duration.durationSeconds.isPresent() && duration.durationSeconds.get() == durationSeconds) {
                    return Optional.of(duration);
                }
            }
            return Optional.absent();
        }
    }

    @Timed
    @GET
    @Path("/durations")
    @Produces(MediaType.APPLICATION_JSON)
    public DurationResult getDurations(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final List<Duration> durations = durationDAO.all();
        return new DurationResult(durations);
    }
    //endregion durations


    //region status
    private SleepSoundStatus getStatus(final Long accountId, final String deviceId, final SoundMap soundMap, final DurationMap durationMap) {

        final SleepSoundStatus NOT_PLAYING = SleepSoundStatus.create();

        final Optional<SenseStateAtTime> senseStateAtTimeOptional = senseStateDynamoDB.getState(deviceId);
        if (!senseStateAtTimeOptional.isPresent()) {
            LOGGER.warn("account-id={} device-id={} sense-state=not-found", accountId, deviceId);
            return NOT_PLAYING;
        }

        final SenseStateAtTime senseStateAtTime = senseStateAtTimeOptional.get();
        if (!senseStateAtTime.state.hasAudioState() || !senseStateAtTime.state.getAudioState().getPlayingAudio()) {
            return NOT_PLAYING;
        }

        final State.AudioState audioState = senseStateAtTime.state.getAudioState();
        if (!(audioState.hasDurationSeconds() && audioState.hasFilePath())) {
            LOGGER.warn("error=inconsistent-sense-state account-id={} sense-id={} audio-state-playing={} audio-state-has-duration={} audio-state-has-file-path={}",
                    accountId, deviceId, audioState.getPlayingAudio(), audioState.getDurationSeconds(), audioState.getFilePath());
            return NOT_PLAYING;
        }

        final Optional<Duration> durationOptional = durationMap.getDurationBySeconds(audioState.getDurationSeconds());
        if (!durationOptional.isPresent()) {
            LOGGER.warn("error=duration-not-found account-id={} sense-id={} duration-seconds={}",
                    accountId, deviceId, audioState.getDurationSeconds());
            return NOT_PLAYING;
        }

        final Optional<Sound> soundOptional = soundMap.getSoundByFilePath(audioState.getFilePath());
        if (!soundOptional.isPresent()) {
            LOGGER.warn("error=sound-file-not-found account-id={} sense-id={} file-path={}",
                    accountId, deviceId, audioState.getFilePath());
            return NOT_PLAYING;
        }
        final Sound sound = soundOptional.get();

        if (audioState.hasVolumePercent()) {
            return SleepSoundStatus.create(sound, durationOptional.get(), audioState.getVolumePercent());
        } else {
            return SleepSoundStatus.create(sound, durationOptional.get());
        }
    }

    @Timed
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public SleepSoundStatus getStatus(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final SleepSoundStatus NOT_PLAYING = SleepSoundStatus.create();
        final Long accountId = accessToken.accountId;

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            return NOT_PLAYING;
        }

        final String deviceId = deviceIdPair.get().externalDeviceId;

        return getStatus(accountId, deviceId, sleepSoundsProcessor, durationDAO);
    }
    //endregion status


    private static Response invalid_request(final String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new JsonError(400, message))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    /**
     * Convert a perceived volume percentage to a linear decibel scaling coefficient.
     * Uses formula from http://www.sengpielaudio.com/calculator-loudness.htm
     *
     * Perception of Loudness (x):   (+20dB = 400%, +10db=200%, -10db=50%, -20db=25%, etc)
     * 10 Db gain would seem to be about twice as loud.
     *
     * Example: If maxDecibels is 60, then for different volumePercents this method returns:
     * convertVolumePercent(60, 100) => 100 // 1.0 * 60 = 60Db
     * convertVolumePercent(60,  50) =>  83 // .83 * 60 = 50Db
     * convertVolumePercent(60,  25) =>  67 // .67 * 60 = 40Db
     *
     * @param maxDecibels Maximum desired decibels for Sense to play (at 100%)
     * @param volumePercent "Perceived" volume percentage (100% is loudest, 50% is half of that, etc).
     *                      Must be in (0, 100].
     * @return Linear scaling factor for Sense to convert to decibels.
     */
    @VisibleForTesting
    protected static Integer convertVolumePercent(final Double maxDecibels,
                                                  final Integer volumePercent) {
        if (volumePercent > 100 || volumePercent <= 0) {
            throw new IllegalArgumentException(String.format("volumePercent must be in the range (0, 100], not %s", volumePercent));
        }
        // Formula/constants obtained from http://www.sengpielaudio.com/calculator-loudness.htm
        final double decibelOffsetFromMaximum = 33.22 * Math.log10(volumePercent / 100.0);
        final double decibels = maxDecibels + decibelOffsetFromMaximum;
        return (int) Math.round((decibels / maxDecibels) * 100);
    }

    /**
     * Uses 60 decibels for the max decibels, otherwise identical to {@link SleepSoundsResource#convertVolumePercent(Double, Integer)}
     * @param volumePercent "Perceived" volume percentage (100% is loudest, 50% is half of that, etc).
     * @return Linear scaling factor for sense to convert to decibels.
     */
    @VisibleForTesting
    protected static Integer convertVolumePercent(final Integer volumePercent) {
        return convertVolumePercent(60.0, volumePercent);
    }

}
