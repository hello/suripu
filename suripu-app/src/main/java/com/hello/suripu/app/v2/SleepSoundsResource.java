package com.hello.suripu.app.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.api.input.State;
import com.hello.suripu.app.messeji.MessejiClient;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FileInfoDAO;
import com.hello.suripu.core.db.FileManifestDAO;
import com.hello.suripu.core.db.SenseStateDynamoDB;
import com.hello.suripu.core.db.sleep_sounds.DurationDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.FileInfo;
import com.hello.suripu.core.models.SenseStateAtTime;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundStatus;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.JsonError;
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

    private final DurationDAO durationDAO;
    private final SenseStateDynamoDB senseStateDynamoDB;
    private final DeviceDAO deviceDAO;
    private final MessejiClient messejiClient;
    private final FileInfoDAO fileInfoDAO;
    private final FileManifestDAO fileManifestDAO;

    private SleepSoundsResource(final DurationDAO durationDAO,
                                final SenseStateDynamoDB senseStateDynamoDB,
                                final DeviceDAO deviceDAO,
                                final MessejiClient messejiClient,
                                final FileInfoDAO fileInfoDAO,
                                final FileManifestDAO fileManifestDAO)
    {
        this.durationDAO = durationDAO;
        this.senseStateDynamoDB = senseStateDynamoDB;
        this.deviceDAO = deviceDAO;
        this.messejiClient = messejiClient;
        this.fileInfoDAO = fileInfoDAO;
        this.fileManifestDAO = fileManifestDAO;
    }

    public static SleepSoundsResource create(final DurationDAO durationDAO,
                                             final SenseStateDynamoDB senseStateDynamoDB,
                                             final DeviceDAO deviceDAO,
                                             final MessejiClient messejiClient,
                                             final FileInfoDAO fileInfoDAO,
                                             final FileManifestDAO fileManifestDAO)
    {
        return new SleepSoundsResource(durationDAO, senseStateDynamoDB, deviceDAO, messejiClient, fileInfoDAO, fileManifestDAO);
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

    @POST
    @Path("/play")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response play(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                         @Valid final PlayRequest playRequest)
    {
        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getById(playRequest.soundId);
        if (!fileInfoOptional.isPresent()) {
            LOGGER.warn("dao=fileInfoDAO method=getById id={} error=not-found", playRequest.soundId);
            return invalid_request("invalid sound id");
        }

        if (fileInfoOptional.get().fileType != FileInfo.FileType.SLEEP_SOUND) {
            LOGGER.warn("dao=fileInfoDAO method=getById id={} error=not-sleep-sound", playRequest.soundId);
            return invalid_request("invalid sound id");
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());

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

        // Make sure that this Sense can play this sound
        final Optional<FileSync.FileManifest> fileManifestOptional = fileManifestDAO.getManifest(senseId);
        if (!fileManifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            return invalid_request("cannot play sound");
        }

        if (!canPlayFile(fileManifestOptional.get(), fileInfoOptional.get())) {
            LOGGER.warn("sense-id={} error=cannot-play-file file-info-id={} path={}",
                    senseId, fileInfoOptional.get().id, fileInfoOptional.get().path);
            return invalid_request("cannot play sound");
        }

        // Send to Messeji
        final Optional<Long> messageId = messejiClient.playAudio(
                senseId, MessejiClient.Sender.fromAccountId(accountId), playRequest.order,
                durationOptional.get(), sound, 0, 0, playRequest.volumePercent);

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
                senseId, MessejiClient.Sender.fromAccountId(accountId), stopRequest.order, 0);
        if (messageId.isPresent()) {
            return Response.status(Response.Status.ACCEPTED).entity("").build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("").build();
        }
    }
    //endregion stop


    //region sounds
    protected class SoundResult {
        @JsonProperty("sounds")
        @NotNull
        public final List<Sound> sounds;

        public SoundResult(final List<Sound> sounds) {
            this.sounds = sounds;
        }
    }

    @GET
    @Path("/sounds")
    @Produces(MediaType.APPLICATION_JSON)
    public SoundResult getSounds(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final Long accountId = accessToken.accountId;
        final List<Sound> sounds = Lists.newArrayList();

        if (!hasSleepSoundsEnabled(accountId)) {
            LOGGER.debug("endpoint=sleep-sounds sleep-sounds-enabled=false account-id={}", accountId);
            return new SoundResult(sounds);
        }
        LOGGER.info("endpoint=sleep-sounds sleep-sounds-enabled=true account-id={}", accountId);

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceIdPair.isPresent()) {
            LOGGER.warn("account-id={} device-id-pair=not-found", accountId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final String senseId = deviceIdPair.get().externalDeviceId;
        final Optional<FileSync.FileManifest> manifestOptional = fileManifestDAO.getManifest(senseId);
        if (!manifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            // If no File manifest, Sense cannot play sounds so return an empty list.
            return new SoundResult(sounds);
        }

        final List<FileInfo> sleepSoundFileInfoList = fileInfoDAO.getAllForType(FileInfo.FileType.SLEEP_SOUND);
        // O(n*m) but n and m are so small this is probably faster than doing something fancier.
        for (final FileInfo fileInfo : sleepSoundFileInfoList) {
            if (canPlayFile(manifestOptional.get(), fileInfo)) {
                sounds.add(Sound.fromFileInfo(fileInfo));
            }
        }

        return new SoundResult(sounds);
    }
    //endregion sounds


    //region durations
    private class DurationResult {
        @JsonProperty("durations")
        @NotNull
        public final List<Duration> durations;

        public DurationResult(final List<Duration> durations) {
            this.durations= durations;
        }
    }

    @GET
    @Path("/durations")
    @Produces(MediaType.APPLICATION_JSON)
    public DurationResult getDurations(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final List<Duration> durations = durationDAO.all();
        return new DurationResult(durations);
    }
    //endregion durations


    //region status
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
            LOGGER.warn("error=inconsistent-sense-state account-id={} device-id={} audio-state-playing={} audio-state-has-duration={} audio-state-has-file-path={}",
                    accountId, deviceId, audioState.getPlayingAudio(), audioState.getDurationSeconds(), audioState.getFilePath());
            return NOT_PLAYING;
        }

        final Optional<Duration> durationOptional = durationDAO.getByDurationSeconds(audioState.getDurationSeconds());
        if (!durationOptional.isPresent()) {
            LOGGER.warn("error=duration-not-found account-id={} device-id={} duration-seconds={}",
                    accountId, deviceId, audioState.getDurationSeconds());
            return NOT_PLAYING;
        }

        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getByFilePath(audioState.getFilePath());
        if (!fileInfoOptional.isPresent() || fileInfoOptional.get().fileType != FileInfo.FileType.SLEEP_SOUND) {
            LOGGER.warn("error=sound-file-not-found account-id={} device-id={} file-path={}",
                    accountId, deviceId, audioState.getFilePath());
            return NOT_PLAYING;
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());

        if (audioState.hasVolumePercent()) {
            return SleepSoundStatus.create(sound, durationOptional.get(), audioState.getVolumePercent());
        } else {
            return SleepSoundStatus.create(sound, durationOptional.get());
        }

    }
    //endregion status


    private static Response invalid_request(final String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new JsonError(400, message))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private static String getFullPath(final String sdCardPath, final String sdCardFilename) {
        return "/" + sdCardPath + "/" + sdCardFilename;
    }

    private static Boolean canPlayFile(final FileSync.FileManifest senseManifest, final FileInfo fileInfo) {
        for (final FileSync.FileManifest.File file : senseManifest.getFileInfoList()) {
            if (file.hasDownloadInfo() &&
                    file.getDownloadInfo().hasSdCardFilename() &&
                    file.getDownloadInfo().hasSdCardPath())
            {
                final String sdCardPath = file.getDownloadInfo().getSdCardPath();
                final String sdCardFilename = file.getDownloadInfo().getSdCardFilename();
                if (getFullPath(sdCardPath, sdCardFilename).equals(fileInfo.path)) {
                    return true;
                }
            }
        }
        return false;
    }

}
