package com.hello.suripu.coredropwizard.clients;

import com.hello.messeji.api.AudioCommands;
import com.hello.messeji.api.Messeji;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 2/23/16.
 */
public class MessejiClientTest {

    @Test
    public void testLogFormatMessage() throws Exception {
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setOrder(1L)
                .setType(Messeji.Message.Type.STOP_AUDIO)
                .setSenderId("sender")
                .build();
        final String logMessage = MessejiClient.logFormatMessage(message);
        assertThat(logMessage, containsString("type=STOP_AUDIO"));
        assertThat(logMessage, containsString("sender_id=sender"));
        assertThat(logMessage, containsString("order=1"));
        assertThat(logMessage, containsString("message_id=0"));
    }

    @Test
    public void testLogFormatMessagePlayAudio() throws Exception {
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setOrder(1L)
                .setType(Messeji.Message.Type.STOP_AUDIO)
                .setSenderId("sender")
                .setPlayAudio(AudioCommands.PlayAudio.newBuilder()
                        .setFadeInDurationSeconds(0)
                        .setFadeOutDurationSeconds(0)
                        .setFilePath("path")
                        .setDurationSeconds(1)
                        .setVolumePercent(100)
                        .build())
                .build();
        final String logMessage = MessejiClient.logFormatMessage(message);
        assertThat(logMessage, containsString("type=STOP_AUDIO"));
        assertThat(logMessage, containsString("sender_id=sender"));
        assertThat(logMessage, containsString("order=1"));
        assertThat(logMessage, containsString("message_id=0"));
        assertThat(logMessage, containsString("file_path=path"));
        assertThat(logMessage, containsString("volume_percent=100"));
        assertThat(logMessage, containsString("duration_seconds=1"));
        assertThat(logMessage, containsString("fade_in_duration_seconds=0"));
        assertThat(logMessage, containsString("fade_out_duration_seconds=0"));
    }

    @Test
    public void testLogFormatMessageStopAudio() throws Exception {
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setOrder(1L)
                .setType(Messeji.Message.Type.STOP_AUDIO)
                .setSenderId("sender")
                .setStopAudio(AudioCommands.StopAudio.newBuilder().setFadeOutDurationSeconds(3))
                .build();
        final String logMessage = MessejiClient.logFormatMessage(message);
        assertThat(logMessage, containsString("type=STOP_AUDIO"));
        assertThat(logMessage, containsString("sender_id=sender"));
        assertThat(logMessage, containsString("order=1"));
        assertThat(logMessage, containsString("message_id=0"));
        assertThat(logMessage, containsString("fade_out_duration_seconds=3"));
    }
}