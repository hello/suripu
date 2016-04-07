package com.hello.suripu.app.messeji;

import com.google.common.base.Optional;
import com.hello.messeji.api.AudioCommands;
import com.hello.messeji.api.Messeji;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import com.yammer.dropwizard.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 2/22/16.
 */
public class MessejiHttpClientIT {

    private MessejiHttpClient client;

    @Before
    public void setUp() {
        final HttpClient httpClient = new HttpClientBuilder().build();
        client = MessejiHttpClient.create(httpClient, "http", "messeji-dev.hello.is", 80);
    }

    @Test
    public void testSendMessage() throws Exception {
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setSenderId("MessejiHttpClientIT")
                .setOrder(System.nanoTime())
                .setType(Messeji.Message.Type.STOP_AUDIO)
                .setStopAudio(AudioCommands.StopAudio.newBuilder().setFadeOutDurationSeconds(2).build())
                .build();
        final Optional<Long> idOptional = client.sendMessage("sense1", message);
        assertThat(idOptional.isPresent(), is(true));
    }

    @Test
    public void testPlayAudio() throws Exception {
        final Optional<Long> idOptional = client.playAudio(
                "sense1", MessejiClient.Sender.fromAccountId(1L), System.nanoTime(), Duration.create(1L, "duration", 30),
                Sound.create(1L, "preview", "sound", "path", "url"), 2, 2, 50, 2);
        assertThat(idOptional.isPresent(), is(true));
    }

    @Test
    public void testStopAudio() throws Exception {
        final Optional<Long> idOptional = client.stopAudio(
                "sense1", MessejiClient.Sender.fromAccountId(1L), System.nanoTime(), 3);
        assertThat(idOptional.isPresent(), is(true));
    }
}