package com.hello.suripu.core.processors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.core.db.FileInfoDAO;
import com.hello.suripu.core.db.FileManifestDAO;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.FileInfo;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.hello.suripu.core.models.sleep_sounds.SoundMap;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jakepiccolo on 3/30/16.
 *
 * Helper class for determining what sleep sounds are available on a Sense, and why.
 */
public class SleepSoundsProcessor implements SoundMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepSoundsProcessor.class);
    
    private final FileInfoDAO fileInfoDAO;
    private final FileManifestDAO fileManifestDAO;

    private SleepSoundsProcessor(final FileInfoDAO fileInfoDAO, final FileManifestDAO fileManifestDAO) {
        this.fileInfoDAO = fileInfoDAO;
        this.fileManifestDAO = fileManifestDAO;
    }

    public static SleepSoundsProcessor create(final FileInfoDAO fileInfoDAO, final FileManifestDAO fileManifestDAO) {
        return new SleepSoundsProcessor(fileInfoDAO, fileManifestDAO);
    }


    /**
     * Class to communicate the sleep sounds for a Sense, along with the state of sleep sound functionality on Sense.
     */
    public static class SoundResult implements SoundMap {
        @JsonProperty("sounds")
        public final List<Sound> sounds;

        @JsonProperty("state")
        public final State state;

        public SoundResult(final List<Sound> sounds, final State state) {
            this.sounds = sounds;
            this.state = state;
        }

        @Override
        public Optional<Sound> getSoundByFilePath(String filePath) {
            for (final Sound sound : sounds) {
                if (sound.filePath.equals(filePath)) {
                    return Optional.of(sound);
                }
            }
            return Optional.absent();
        }

        public enum State {
            OK,
            SOUNDS_NOT_DOWNLOADED,      // Sounds have not *yet* been downloaded to Sense, but should be.
            SENSE_UPDATE_REQUIRED,      // Sense cannot play sounds because it has old firmware
            FEATURE_DISABLED            // User doesn't have this feature flipped.
        }
    }

    /**
     * @param senseId Sense ID paired to this account
     * @return SoundResult containing the list of Sounds for the Sense.
     */
    public SoundResult getSounds(final String senseId, final HardwareVersion hardwareVersion) {
        final List<Sound> sounds = Lists.newArrayList();

        final Optional<FileSync.FileManifest> manifestOptional = fileManifestDAO.getManifest(senseId);
        if (!manifestOptional.isPresent()) {
            LOGGER.warn("dao=fileManifestDAO method=getManifest sense-id={} error=not-found", senseId);
            // If no File manifest, Sense cannot play sounds so return an empty list.
            return new SoundResult(sounds, SoundResult.State.SENSE_UPDATE_REQUIRED);

        }

        final int firmwareVersion = manifestOptional.get().hasFirmwareVersion()
                ? manifestOptional.get().getFirmwareVersion()
                : 0;

        final List<FileInfo> fileInfoList = fileInfoDAO.getAll(firmwareVersion, senseId);

        final List<FileInfo> sleepSoundFileInfoList = Lists.newArrayList();
        for (final FileInfo fileInfo: fileInfoList) {
            if (fileInfo.fileType == FileInfo.FileType.SLEEP_SOUND) {
                sleepSoundFileInfoList.add(fileInfo);
            }
        }

        LOGGER.debug("sense-id={} sleep-sound-file-info-list-size={} file-manifest-file-count={}",
                senseId, sleepSoundFileInfoList.size(), manifestOptional.get().getFileInfoCount());

        // O(n*m) but n and m are so small this is probably faster than doing something fancier.
        for (final FileInfo fileInfo : sleepSoundFileInfoList) {
            if (canPlayFile(manifestOptional.get(), fileInfo, hardwareVersion)) {
                sounds.add(Sound.fromFileInfo(fileInfo));
            }
        }

        if (sounds.size() < sleepSoundFileInfoList.size()) {
            LOGGER.warn("endpoint=sounds error=not-enough-sounds sense-id={} num-sounds={}",
                    senseId, sounds.size());
            return new SoundResult(Lists.<Sound>newArrayList(), SoundResult.State.SOUNDS_NOT_DOWNLOADED);
        }

        return new SoundResult(sounds, SoundResult.State.OK);
    }

    /**
     * @param filePath Full path of the file found on Sense.
     * @return Sound if the filePath maps to one, else absent.
     */
    public Optional<Sound> getSoundByFilePath(final String filePath) {
        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getByFilePath(filePath);
        if (!fileInfoOptional.isPresent() || !fileInfoOptional.get().fileType.equals(FileInfo.FileType.SLEEP_SOUND)) {
            LOGGER.warn("dao=fileInfoDAO error=path-not-found file_path={}", filePath);
            return Optional.absent();
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());
        return Optional.of(sound);
    }

    public Optional<Sound> getSoundByFileName(final String fileName) {
        final Optional<FileInfo> fileInfoOptional = fileInfoDAO.getByFileName(fileName);
        if (!fileInfoOptional.isPresent() || !fileInfoOptional.get().fileType.equals(FileInfo.FileType.SLEEP_SOUND)) {
            LOGGER.warn("dao=fileInfoDAO error=path-not-found file_name={}", fileName);
            return Optional.absent();
        }

        final Sound sound = Sound.fromFileInfo(fileInfoOptional.get());
        return Optional.of(sound);
    }

    /**
     * @return a sleep sound for this Sense to play, but only if this Sense can play the sound and if the sound ID is valid.
     */
    public Optional<Sound> getSound(final String senseId, final Long soundId, final HardwareVersion hardwareVersion) {
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

        if (!canPlayFile(fileManifestOptional.get(), fileInfoOptional.get(), hardwareVersion)) {
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

    private static Boolean canPlayFile(final FileSync.FileManifest senseManifest, final FileInfo fileInfo, final HardwareVersion hardwareVersion) {
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

                LOGGER.trace("method=canPlayFile sense-id={} sd-card-path={} sd-card-filename={} file-info-path={}",
                        senseManifest.getSenseId(), sdCardPath, sdCardFilename, fileInfo.path);

                if (getFullPath(sdCardPath, sdCardFilename).equals(fileInfo.path)) {
                    if (Arrays.equals(fileInfoSha, file.getDownloadInfo().getSha1().toByteArray())) {
                        return true;
                    } else if(HardwareVersion.SENSE_ONE_FIVE.equals(hardwareVersion)) {
                        // TODO: use FileInfoSenseOneFive to get correct file-info for Sense 1p5, override for now
                        LOGGER.info("action=play-sleep-sound-override sense-id={}", senseManifest.getSenseId());
                        return true;
                    }
                    LOGGER.warn("sense-id={} file-info-path={} file-info-sha={} error=sha-does-not-match",
                            senseManifest.getSenseId(), fileInfo.path, fileInfo.sha);
                }
            } else {
                LOGGER.debug("sense-id={} error=incomplete-download-info", senseManifest.getSenseId());
            }
        }
        LOGGER.debug("sense-id={} error=cannot-play-file file-info-path={}",
                senseManifest.getSenseId(), fileInfo.path);
        return false;
    }
}
