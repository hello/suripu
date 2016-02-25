package com.hello.suripu.app.messeji;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.hello.messeji.api.AudioCommands;
import com.hello.messeji.api.Messeji;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.Sound;

/**
 * Created by jakepiccolo on 2/22/16.
 */
public abstract class MessejiClient {
    public abstract Optional<Long> sendMessage(final String senseId, final Messeji.Message message);

    public static class Sender {
        private String id;

        private Sender(final String id) {
            this.id = id;
        }

        public static Sender fromAccountId(final Long accountId) {
            return new Sender(String.format("account:%s", accountId));
        }
    }

    protected static String logFormatMessage(final Messeji.Message message) {
        final StringBuilder builder = new StringBuilder();

        final String general = String.format("type=%s sender_id=%s order=%s message_id=%s",
                message.getType(), message.getSenderId(), message.getOrder(), message.getMessageId());
        builder.append(general);

        if (message.hasPlayAudio()) {
            final AudioCommands.PlayAudio play = message.getPlayAudio();
            final String playAudio = String.format("file_path=%s volume_percent=%s duration_seconds=%s fade_in_duration_seconds=%s fade_out_duration_seconds=%s",
                    play.getFilePath(), play.getVolumePercent(), play.getDurationSeconds(), play.getFadeInDurationSeconds(), play.getFadeOutDurationSeconds());
            builder.append(" " + playAudio);
        }

        if (message.hasStopAudio()) {
            final String stopAudio = String.format("fade_out_duration_seconds=%s", message.getStopAudio().getFadeOutDurationSeconds());
            builder.append(" " + stopAudio);
        }

        return builder.toString();
    }

    public Optional<Long> playAudio(final String senseId, final Sender sender, final Long order, final Duration duration, final Sound sound,
                                    final Integer fadeInSeconds, final Integer fadeOutSeconds, final Integer volumePercent)
    {
        final AudioCommands.PlayAudio.Builder playBuilder = AudioCommands.PlayAudio.newBuilder()
                .setFadeInDurationSeconds(fadeInSeconds)
                .setFadeOutDurationSeconds(fadeOutSeconds)
                .setVolumePercent(volumePercent)
                .setFilePath(sound.filePath);
        if (duration.durationSeconds.isPresent()) {
            playBuilder.setDurationSeconds(duration.durationSeconds.get());
        }
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setOrder(order)
                .setSenderId(sender.id)
                .setType(Messeji.Message.Type.PLAY_AUDIO)
                .setPlayAudio(playBuilder.build())
                .build();

        return sendMessage(senseId, message);
    }

    public Optional<Long> stopAudio(final String senseId, final Sender sender, final Long order, final int fadeOutDurationSeconds) {
        final Messeji.Message message = Messeji.Message.newBuilder()
                .setOrder(order)
                .setSenderId(sender.id)
                .setType(Messeji.Message.Type.STOP_AUDIO)
                .setStopAudio(AudioCommands.StopAudio.newBuilder().setFadeOutDurationSeconds(fadeOutDurationSeconds).build())
                .build();
        return sendMessage(senseId, message);
    }
}
