package com.hello.suripu.core.processors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.core.db.FileInfoDAO;
import com.hello.suripu.core.db.FileManifestDAO;
import com.hello.suripu.core.models.FileInfo;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jakepiccolo on 3/30/16.
 */
public class SleepSoundsProcessor extends FeatureFlippedProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepSoundsProcessor.class);

    private static final Integer MIN_SOUNDS = 5; // Anything less than this and we return an empty list.

    private final FileInfoDAO fileInfoDAO;
    private final FileManifestDAO fileManifestDAO;

    private SleepSoundsProcessor(final FileInfoDAO fileInfoDAO, final FileManifestDAO fileManifestDAO) {
        this.fileInfoDAO = fileInfoDAO;
        this.fileManifestDAO = fileManifestDAO;
    }

    public static SleepSoundsProcessor create(final FileInfoDAO fileInfoDAO, final FileManifestDAO fileManifestDAO) {
        return new SleepSoundsProcessor(fileInfoDAO, fileManifestDAO);
    }

    public static class SoundResult {
        @JsonProperty("sounds")
        @NotNull
        public final List<Sound> sounds;

        @JsonProperty("state")
        @NotNull
        public final State state;

        public SoundResult(final List<Sound> sounds, final State state) {
            this.sounds = sounds;
            this.state = state;
        }

        public enum State {
            OK,
            SOUNDS_NOT_DOWNLOADED,      // Sounds have not *yet* been downloaded to Sense, but should be.
            SENSE_UPDATE_REQUIRED,      // Sense cannot play sounds because it has old firmware
            FEATURE_DISABLED            // User doesn't have this feature flipped.
        }
    }

    public SoundResult getSounds(final Long accountId, final String senseId) {
        final List<Sound> sounds = Lists.newArrayList();

        if (!hasSleepSoundsEnabled(accountId)) {
            LOGGER.debug("endpoint=sleep-sounds sleep-sounds-enabled=false account-id={}", accountId);
            return new SoundResult(sounds, SoundResult.State.FEATURE_DISABLED);
        }

        LOGGER.info("endpoint=sleep-sounds sleep-sounds-enabled=true account-id={}", accountId);

        final Optional<FileSync.FileManifest> manifestOptional = fileManifestDAO.getManifest(senseId);
        if (!manifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            // If no File manifest, Sense cannot play sounds so return an empty list.
            return new SoundResult(sounds, SoundResult.State.SENSE_UPDATE_REQUIRED);
        }

        final List<FileInfo> sleepSoundFileInfoList = fileInfoDAO.getAllForType(FileInfo.FileType.SLEEP_SOUND);
        LOGGER.debug("device-id={} sleep-sound-file-info-list-size={} file-manifest-file-count={}",
                senseId, sleepSoundFileInfoList.size(), manifestOptional.get().getFileInfoCount());

        // O(n*m) but n and m are so small this is probably faster than doing something fancier.
        for (final FileInfo fileInfo : sleepSoundFileInfoList) {
            if (canPlayFile(manifestOptional.get(), fileInfo)) {
                sounds.add(Sound.fromFileInfo(fileInfo));
            }
        }

        if (sounds.size() < MIN_SOUNDS) {
            LOGGER.warn("endpoint=sounds error=not-enough-sounds sense-id={} num-sounds={}",
                    senseId, sounds.size());
            return new SoundResult(Lists.<Sound>newArrayList(), SoundResult.State.SOUNDS_NOT_DOWNLOADED);
        }

        return new SoundResult(sounds, SoundResult.State.OK);
    }

    public Optional<Sound> getSound(final String filePath) {
        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getByFilePath(filePath);
        if (!fileInfoOptional.isPresent() || fileInfoOptional.get().fileType != FileInfo.FileType.SLEEP_SOUND) {
            LOGGER.warn("dao=fileInfoDAO error=path-not-found file-path={}", filePath);
            return Optional.absent();
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());
        return Optional.of(sound);
    }

    /**
     * @return a sleep sound for this Sense to play, but only if this Sense can play the sound and if the sound ID is valid.
     */
    public Optional<Sound> getSound(final String senseId, final Long soundId) {
        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getById(soundId);
        if (!fileInfoOptional.isPresent()) {
            LOGGER.warn("dao=fileInfoDAO method=getById id={} error=not-found", soundId);
            return Optional.absent();
        }

        if (fileInfoOptional.get().fileType != FileInfo.FileType.SLEEP_SOUND) {
            LOGGER.warn("dao=fileInfoDAO method=getById id={} error=not-sleep-sound", soundId);
            return Optional.absent();
        }

        // Make sure that this Sense can play this sound
        final Optional<FileSync.FileManifest> fileManifestOptional = fileManifestDAO.getManifest(senseId);
        if (!fileManifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            return Optional.absent();
        }

        if (!canPlayFile(fileManifestOptional.get(), fileInfoOptional.get())) {
            LOGGER.warn("sense-id={} error=cannot-play-file file-info-id={} path={}",
                    senseId, fileInfoOptional.get().id, fileInfoOptional.get().path);
            return Optional.absent();
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());
        return Optional.of(sound);
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

                final byte[] fileInfoSha;
                try {
                    fileInfoSha = fileInfo.getShaBytes();
                } catch (DecoderException e) {
                    LOGGER.error("method=canPlayFile exception=DecoderException file-info-path={} file-info-sha={} error={}",
                            fileInfo.path, fileInfo.sha, e);
                    continue;
                }

                LOGGER.trace("method=canPlayFile device-id={} sd-card-path={} sd-card-filename={} file-info-path={}",
                        senseManifest.getSenseId(), sdCardPath, sdCardFilename, fileInfo.path);

                if (getFullPath(sdCardPath, sdCardFilename).equals(fileInfo.path)) {
                    if (Arrays.equals(fileInfoSha, file.getDownloadInfo().getSha1().toByteArray())) {
                        return true;
                    }
                    LOGGER.warn("device-id={} file-info-path={} file-info-sha={} error=sha-does-not-match",
                            senseManifest.getSenseId(), fileInfo.path, fileInfo.sha);
                }
            } else {
                LOGGER.debug("device-id={} error=incomplete-download-info", senseManifest.getSenseId());
            }
        }
        LOGGER.debug("device-id={} error=cannot-play-file file-info-path={}",
                senseManifest.getSenseId(), fileInfo.path);
        return false;
    }
}
