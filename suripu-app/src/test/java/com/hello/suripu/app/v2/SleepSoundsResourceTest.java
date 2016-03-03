package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.hello.suripu.api.input.State;
import com.hello.suripu.app.messeji.MessejiClient;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.SenseStateDynamoDB;
import com.hello.suripu.core.db.sleep_sounds.DurationDAO;
import com.hello.suripu.core.db.sleep_sounds.SoundDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.SenseStateAtTime;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundStatus;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
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
    private SoundDAO soundDAO;
    private MessejiClient messejiClient;
    private SleepSoundsResource sleepSoundsResource;

    @Before
    public void setUp() {
        deviceDAO = Mockito.mock(DeviceDAO.class);
        senseStateDynamoDB = Mockito.mock(SenseStateDynamoDB.class);
        durationDAO = Mockito.mock(DurationDAO.class);
        soundDAO = Mockito.mock(SoundDAO.class);
        messejiClient = Mockito.mock(MessejiClient.class);
        sleepSoundsResource = SleepSoundsResource.create(soundDAO, durationDAO, senseStateDynamoDB, deviceDAO, messejiClient);
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
        Mockito.when(soundDAO.getByFilePath(Mockito.anyString())).thenReturn(Optional.<Sound>absent());
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
    public void testGetStatusAllCorrect() throws Exception {
        Mockito.when(deviceDAO.getMostRecentSensePairByAccountId(Mockito.anyLong())).thenReturn(pair);
        final Duration duration = Duration.create(1L, "path", 30);
        Mockito.when(durationDAO.getByDurationSeconds(Mockito.anyInt())).thenReturn(Optional.of(duration));
        final Sound sound = Sound.create(1L, "preview", "name", "path", "url");
        Mockito.when(soundDAO.getByFilePath(Mockito.anyString())).thenReturn(Optional.of(sound));
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
        assertThat(status.sound.get(), is(sound));
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


        final Sound sound = Sound.create(soundId, "preview", "name", "path", "url");
        final Duration duration = Duration.create(durationId, "name", 30);

        // Only work for our specific sound
        Mockito.when(soundDAO.getById(Mockito.anyLong())).thenReturn(Optional.<Sound>absent());
        Mockito.when(soundDAO.getById(soundId)).thenReturn(Optional.of(sound));

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
}