package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.api.input.State;
import com.hello.suripu.app.messeji.MessejiClient;
import com.hello.suripu.app.modules.RolloutAppModule;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FileInfoDAO;
import com.hello.suripu.core.db.FileManifestDAO;
import com.hello.suripu.core.db.SenseStateDynamoDB;
import com.hello.suripu.core.db.sleep_sounds.DurationDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Feature;
import com.hello.suripu.core.models.FileInfo;
import com.hello.suripu.core.models.SenseStateAtTime;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundStatus;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.SleepSoundsProcessor;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 2/22/16.
 */
public class SleepSoundsResourceTest {

    private final Long accountId = 1L;
    private final String senseId = "sense";

    private final Optional<DeviceAccountPair> pair = Optional.of(new DeviceAccountPair(accountId, 1L, senseId, new DateTime()));

    private final AccessToken token = makeToken(accountId);

    private DeviceDAO deviceDAO;
    private SenseStateDynamoDB senseStateDynamoDB;
    private DurationDAO durationDAO;
    private MessejiClient messejiClient;
    private FileInfoDAO fileInfoDAO;
    private FileManifestDAO fileManifestDAO;
    private SleepSoundsResource sleepSoundsResource;
    private FeatureStore featureStore;

    @Before
    public void setUp() {
        featureStore = Mockito.mock(FeatureStore.class);
        Mockito.when(featureStore.getData())
                .thenReturn(
                        ImmutableMap.of(
                                FeatureFlipper.SLEEP_SOUNDS_ENABLED,
                                new Feature("sleep_sounds_enabled", Collections.EMPTY_LIST, Collections.EMPTY_LIST, (float) 100.0)));
        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        deviceDAO = Mockito.mock(DeviceDAO.class);
        senseStateDynamoDB = Mockito.mock(SenseStateDynamoDB.class);
        durationDAO = Mockito.mock(DurationDAO.class);
        messejiClient = Mockito.mock(MessejiClient.class);
        fileInfoDAO = Mockito.mock(FileInfoDAO.class);
        fileManifestDAO = Mockito.mock(FileManifestDAO.class);
        sleepSoundsResource = SleepSoundsResource.create(
                durationDAO, senseStateDynamoDB, deviceDAO,
                messejiClient, fileInfoDAO, fileManifestDAO);
    }

    private void assertEmpty(final SleepSoundStatus status) {
        assertThat(status.isPlaying, is(false));
        assertThat(status.duration.isPresent(), is(false));
        assertThat(status.sound.isPresent(), is(false));
    }

    private static AccessToken makeToken(final Long accountId) {
        return new AccessToken.Builder()
                .withAccountId(accountId)
                .withCreatedAt(DateTime.now())
                .withExpiresIn(DateTime.now().plusHours(1).getMillis())
                .withRefreshToken(UUID.randomUUID())
                .withToken(UUID.randomUUID())
                .withScopes(new OAuthScope[]{ OAuthScope.USER_BASIC })
                .withAppId(1L)
                .build();
    }

    // region getStatus
    @Test
    public void testGetStatusNoDevicePaired() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(Optional.<DeviceAccountPair>absent());
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusNoState() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        Mockito.when(senseStateDynamoDB.getState(senseId)).thenReturn(Optional.<SenseStateAtTime>absent());
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusNoAudioState() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final SenseStateAtTime state = new SenseStateAtTime(State.SenseState.newBuilder().setSenseId(senseId).build(), new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusAudioNotPlaying() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder().setPlayingAudio(false).build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusInconsistentState() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder().setPlayingAudio(true).build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusNoDuration() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder()
                                .setPlayingAudio(true)
                                .setFilePath("path")
                                .build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusNoFilePath() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder()
                                .setPlayingAudio(true)
                                .setDurationSeconds(1)
                                .build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusInvalidDuration() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        Mockito.when(durationDAO.getByDurationSeconds(Mockito.anyInt())).thenReturn(Optional.<Duration>absent());
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder()
                                .setPlayingAudio(true)
                                .setFilePath("path")
                                .setDurationSeconds(1)
                                .build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    @Test
    public void testGetStatusInvalidPath() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        Mockito.when(durationDAO.getByDurationSeconds(Mockito.anyInt())).thenReturn(Optional.of(Duration.create(1L, "path", 30)));
        Mockito.when(fileInfoDAO.getByFilePath(Mockito.anyString())).thenReturn(Optional.<FileInfo>absent());
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder()
                                .setPlayingAudio(true)
                                .setFilePath("path")
                                .setDurationSeconds(1)
                                .build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);
        assertEmpty(status);
    }

    private FileInfo makeFileInfo(final Long id, final String preview, final String name, final String path, final String url) {
        return FileInfo.newBuilder()
                .withFileType(FileInfo.FileType.SLEEP_SOUND)
                .withId(id)
                .withPreviewUri(preview)
                .withName(name)
                .withPath(path)
                .withUri(url)
                .withSha(path)
                .withIsPublic(true)
                .withFirmwareVersion(1)
                .build();
    }

    @Test
    public void testGetStatusAllCorrect() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final Duration duration = Duration.create(1L, "path", 30);
        Mockito.when(durationDAO.getByDurationSeconds(Mockito.anyInt())).thenReturn(Optional.of(duration));
        final FileInfo fileInfo = makeFileInfo(1L, "preview", "name", "path", "url");
        Mockito.when(fileInfoDAO.getByFilePath(Mockito.anyString())).thenReturn(Optional.of(fileInfo));
        final SenseStateAtTime state = new SenseStateAtTime(
                State.SenseState.newBuilder()
                        .setSenseId(senseId)
                        .setAudioState(State.AudioState.newBuilder()
                                .setPlayingAudio(true)
                                .setFilePath("path")
                                .setDurationSeconds(1)
                                .build())
                        .build(),
                new DateTime());
        Mockito.when(senseStateDynamoDB.getState(senseId))
                .thenReturn(Optional.of(state));
        final SleepSoundStatus status = sleepSoundsResource.getStatus(token);

        assertThat(status.isPlaying, is(true));
        assertThat(status.sound.isPresent(), is(true));
        assertThat(status.duration.isPresent(), is(true));
        assertThat(status.duration.get(), is(duration));
        assertThat(status.sound.get(), is(Sound.fromFileInfo(fileInfo)));
        assertThat(status.volumePercent.isPresent(), is(false));
    }
    // endregion getStatus

    // region play
    @Test
    public void testPlayRequestValidation() throws Exception {
        final Long durationId = 1L;
        final Long soundId = 1L;
        final Long order = 1L;
        final Integer volumePercent = 50;

        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(pair);

        final FileInfo fileInfo = FileInfo.newBuilder()
                .withId(soundId)
                .withPreviewUri("preview")
                .withName("name")
                .withPath("/path/to/file")
                .withUri("url")
                .withFirmwareVersion(1)
                .withIsPublic(true)
                .withSha("11")
                .withFileType(FileInfo.FileType.SLEEP_SOUND)
                .build();
        final FileSync.FileManifest fileManifest = FileSync.FileManifest.newBuilder()
                .addFileInfo(FileSync.FileManifest.File.newBuilder()
                        .setDownloadInfo(FileSync.FileManifest.FileDownload.newBuilder()
                                .setSdCardFilename("file")
                                .setSdCardPath("path/to")
                                .build())
                        .build())
                .build();
        final Duration duration = Duration.create(durationId, "name", 30);

        // Only work for our specific sound
        Mockito.when(fileInfoDAO.getById(Mockito.anyLong())).thenReturn(Optional.<FileInfo>absent());
        Mockito.when(fileInfoDAO.getById(soundId)).thenReturn(Optional.of(fileInfo));

        Mockito.when(fileManifestDAO.getManifest(Mockito.anyString())).thenReturn(Optional.<FileSync.FileManifest>absent());
        Mockito.when(fileManifestDAO.getManifest(senseId)).thenReturn(Optional.of(fileManifest));

        // Only work for our specific duration
        Mockito.when(durationDAO.getById(Mockito.anyLong())).thenReturn(Optional.<Duration>absent());
        Mockito.when(durationDAO.getById(durationId)).thenReturn(Optional.of(duration));

        // TEST invalid sound
        assertThat(
                sleepSoundsResource.play(
                        token, SleepSoundsResource.PlayRequest.create(soundId + 1, durationId, order, volumePercent)
                ).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));

        // TEST invalid duration
        assertThat(
                sleepSoundsResource.play(
                        token, SleepSoundsResource.PlayRequest.create(soundId, durationId + 1, order, volumePercent)
                ).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));

        // TEST no sense for account
        final Long badAccountId = accountId + 100;
        final AccessToken badToken = makeToken(badAccountId);
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(badAccountId)).thenReturn(Optional.<DeviceAccountPair>absent());
        assertThat(
                sleepSoundsResource.play(
                        badToken, SleepSoundsResource.PlayRequest.create(soundId, durationId, order, volumePercent)
                ).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }
    // endregion play

    // region getSounds
    @Test
    public void testGetSoundsNoManifest() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(pair);

        Mockito.when(fileManifestDAO.getManifest(Mockito.anyString())).thenReturn(Optional.<FileSync.FileManifest>absent());

        final SleepSoundsProcessor.SoundResult soundResult = sleepSoundsResource.getSounds(token);
        assertThat(soundResult.sounds.size(), is(0));
        assertThat(soundResult.state, is(SleepSoundsProcessor.SoundResult.State.SENSE_UPDATE_REQUIRED));
    }

    @Test
    public void testGetSounds() throws Exception {
        final Long soundId = 1L;

        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(pair);

        final List<FileInfo> fileInfoList = Lists.newArrayList();
        for (int i = 0; i < 5; i++) {
            fileInfoList.add(FileInfo.newBuilder()
                    .withId(soundId + i)
                    .withPreviewUri("preview")
                    .withName("name")
                    .withPath("/path/to/file")
                    .withUri("url")
                    .withFirmwareVersion(1)
                    .withIsPublic(true)
                    .withSha("11")
                    .withFileType(FileInfo.FileType.SLEEP_SOUND)
                    .build());
        }
        fileInfoList.add(FileInfo.newBuilder()
                .withId(soundId + fileInfoList.size())
                .withPreviewUri("preview")
                .withName("name")
                .withPath("/wrong/path/to/file")
                .withUri("url")
                .withFirmwareVersion(1)
                .withIsPublic(true)
                .withSha("11")
                .withFileType(FileInfo.FileType.SLEEP_SOUND)
                .build());

        Mockito.when(fileInfoDAO.getAllForType(FileInfo.FileType.SLEEP_SOUND)).thenReturn(fileInfoList);

        final FileSync.FileManifest fileManifest = FileSync.FileManifest.newBuilder()
                .addFileInfo(FileSync.FileManifest.File.newBuilder()
                        .setDownloadInfo(FileSync.FileManifest.FileDownload.newBuilder()
                                .setSdCardFilename("file")
                                .setSdCardPath("path/to")
                                .setSha1(ByteString.copyFrom(Hex.decodeHex("11".toCharArray())))
                                .build())
                        .build())
                .build();
        Mockito.when(fileManifestDAO.getManifest(Mockito.anyString())).thenReturn(Optional.of(fileManifest));

        final SleepSoundsProcessor.SoundResult soundResult = sleepSoundsResource.getSounds(token);
        assertThat(soundResult.sounds.size(), is(5));
        assertThat(soundResult.state, is(SleepSoundsProcessor.SoundResult.State.OK));
        for (int i = 0; i < soundResult.sounds.size(); i++) {
            assertThat(soundResult.sounds.get(i).id, is(soundId + i));
        }
    }

    @Test
    public void testGetSoundsNotEnoughSounds() throws Exception {
        final Long soundId = 1L;

        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(pair);

        final List<FileInfo> fileInfoList = Lists.newArrayList();
        for (int i = 0; i < 3; i++) {
            fileInfoList.add(FileInfo.newBuilder()
                    .withId(soundId + i)
                    .withPreviewUri("preview")
                    .withName("name")
                    .withPath("/path/to/file")
                    .withUri("url")
                    .withFirmwareVersion(1)
                    .withIsPublic(true)
                    .withSha("11")
                    .withFileType(FileInfo.FileType.SLEEP_SOUND)
                    .build());
        }

        Mockito.when(fileInfoDAO.getAllForType(FileInfo.FileType.SLEEP_SOUND)).thenReturn(fileInfoList);

        final FileSync.FileManifest fileManifest = FileSync.FileManifest.newBuilder()
                .addFileInfo(FileSync.FileManifest.File.newBuilder()
                        .setDownloadInfo(FileSync.FileManifest.FileDownload.newBuilder()
                                .setSdCardFilename("file")
                                .setSdCardPath("path/to")
                                .setSha1(ByteString.copyFrom(Hex.decodeHex("11".toCharArray())))
                                .build())
                        .build())
                .build();
        Mockito.when(fileManifestDAO.getManifest(Mockito.anyString())).thenReturn(Optional.of(fileManifest));

        final SleepSoundsProcessor.SoundResult soundResult = sleepSoundsResource.getSounds(token);
        assertThat(soundResult.sounds.size(), is(0));
        assertThat(soundResult.state, is(SleepSoundsProcessor.SoundResult.State.SOUNDS_NOT_DOWNLOADED));
    }

    @Test(expected = WebApplicationException.class)
    public void testGetSoundsNotFeatureFlipped() {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(pair);
        Mockito.when(featureStore.getData())
                .thenReturn(
                        ImmutableMap.of(
                                FeatureFlipper.SLEEP_SOUNDS_ENABLED,
                                new Feature("sleep_sounds_enabled", Collections.EMPTY_LIST, Collections.EMPTY_LIST, (float) 0.0)));
        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);
        sleepSoundsResource = SleepSoundsResource.create(
                durationDAO, senseStateDynamoDB, deviceDAO,
                messejiClient, fileInfoDAO, fileManifestDAO);
        sleepSoundsResource.getSounds(token);
    }

    // endregion getSounds

    // region convertVolumePercent
    @Test
    public void testConvertVolumePercent() throws Exception {
        assertThat(SleepSoundsResource.convertVolumePercent(100), is(100));
        assertThat(SleepSoundsResource.convertVolumePercent(50), is(83));
        assertThat(SleepSoundsResource.convertVolumePercent(25), is(67));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertVolumePercentTooLarge() {
        SleepSoundsResource.convertVolumePercent(200);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertVolumePercentTooSmall() {
        SleepSoundsResource.convertVolumePercent(0);
    }
    // endregion convertVolumePercent
}