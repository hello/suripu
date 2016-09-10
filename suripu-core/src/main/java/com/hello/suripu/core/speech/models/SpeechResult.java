package com.hello.suripu.core.speech.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Created by ksg on 7/20/16
 */
public class SpeechResult {

    public static final String EMPTY_STRING_PLACEHOLDER  = "NONE";

    @JsonIgnore
    public final Optional<Long> accountId;

    @JsonIgnore
    public final String senseId;

    @JsonIgnore
    public final String audioIdentifier;  // uuid string of audio file in S3

    @JsonProperty("datetime_utc")
    public final DateTime dateTimeUTC;

    @JsonIgnore
    public final DateTime updatedUTC;

    @JsonProperty("text")
    public final Optional<String> text;   // transcribed text

    @JsonProperty("response_text")
    public final Optional<String> responseText; // Sense response

    @JsonIgnore
    public final SpeechToTextService service;

    @JsonIgnore
    public final Optional<Float> confidence;


    @JsonIgnore
    public final Optional<String> s3ResponseKeyname;

    @JsonIgnore
    public final Optional<String> handlerType;

    @JsonProperty("command")
    public final Optional<String> command; // TODO: this should probably be a class or something

    @JsonIgnore
    public final WakeWord wakeWord;

    @JsonIgnore
    public final Map<String, Float> wakeWordsConfidence;

    @JsonProperty("result")
    public final Result result;


    public SpeechResult(final Optional<Long> accountId,
                        final String senseId,
                        final DateTime dateTimeUTC,
                        final DateTime updatedUTC,
                        final String audioIdentifier,
                        final Optional<String> text,
                        final Optional<String> responseText,
                        final SpeechToTextService service,
                        final Optional<Float> confidence,
                        final Optional<String> s3ResponseKeyname,
                        final Optional<String> handlerType,
                        final Optional<String> command,
                        final WakeWord wakeWord,
                        final Map<String, Float> wakeWordsConfidence,
                        final Result result) {
        this.accountId = accountId;
        this.senseId = senseId;
        this.dateTimeUTC = dateTimeUTC;
        this.updatedUTC = updatedUTC;
        this.audioIdentifier = audioIdentifier;
        this.text = text;
        this.responseText = responseText;
        this.service = service;
        this.confidence = confidence;
        this.s3ResponseKeyname = s3ResponseKeyname;
        this.handlerType = handlerType;
        this.command = command;
        this.wakeWord = wakeWord;
        this.wakeWordsConfidence = wakeWordsConfidence;
        this.result = result;

        // TODO: add checks for not null?
    }

    public static class Builder {
        private Optional<Long> accountId = Optional.absent();
        private String senseId = "";
        private DateTime dateTimeUTC;
        private DateTime updatedUTC;
        private String audioIdentifier = "";
        private Optional<String> text = Optional.absent();
        private Optional<String> responseText = Optional.absent();
        private SpeechToTextService service = SpeechToTextService.GOOGLE;
        private Optional<Float> confidence = Optional.absent();
        private Optional<String> s3ResponseKeyname = Optional.absent();
        private Optional<String> handlerType = Optional.absent();
        private Optional<String> command = Optional.absent();
        private WakeWord wakeWord = WakeWord.OKAY_SENSE;
        private Map<String, Float> wakeWordsConfidence = Maps.newHashMap();
        private Result result = Result.NONE;

        public Builder withAccountId(final Long accountId) {
            this.accountId = Optional.of(accountId);
            return this;
        }

        public Builder withSenseId(final String senseId) {
            this.senseId = senseId;
            return this;
        }

        public Builder withDateTimeUTC(final DateTime dateTimeUTC) {
            this.dateTimeUTC = dateTimeUTC;
            this.updatedUTC = dateTimeUTC;
            return this;
        }

        public Builder withUpdatedUTC(final DateTime updatedUTC) {
            this.updatedUTC = updatedUTC;
            return this;
        }

        public Builder withAudioIndentifier(final String audioIdentifier) {
            this.audioIdentifier = audioIdentifier;
            return this;
        }

        public Builder withText(final String text) {
            this.text = Optional.of(text);
            return this;
        }

        public Builder withResponseText(final String text) {
            this.responseText= Optional.of(text);
            return this;
        }

        public Builder withService(final SpeechToTextService service) {
            this.service = service;
            return this;
        }

        public Builder withConfidence(final Float confidence) {
            this.confidence = Optional.of(confidence);
            return this;
        }

        public Builder withS3Keyname(final String s3Keyname) {
            this.s3ResponseKeyname = Optional.of(s3Keyname);
            return this;
        }

        public Builder withHandlerType(final String handlerType) {
            this.handlerType = Optional.of(handlerType);
            return this;
        }

        public Builder withCommand(final String command) {
            this.command = Optional.of(command);
            return this;
        }

        public Builder withWakeWord(final WakeWord wakeWord) {
            this.wakeWord = wakeWord;
            return this;
        }

        public Builder withWakeWordString(final String wakeWordString) {
            this.wakeWord = WakeWord.fromString(wakeWordString);
            return this;
        }

        public Builder withWakeWordsConfidence(final Map<String, Float> wakeWordsConfidence) {
            for (final Map.Entry<String, Float> entry: wakeWordsConfidence.entrySet()) {
                final WakeWord wakeWord = WakeWord.fromWakeWordText(entry.getKey());
                if (!wakeWord.equals(WakeWord.NULL)) {
                    this.wakeWordsConfidence.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public Builder withResult(final Result result) {
            this.result = result;
            return this;
        }

        public SpeechResult build() {
            return new SpeechResult(accountId, senseId, dateTimeUTC, updatedUTC, audioIdentifier,
                    text, responseText, service, confidence,
                    s3ResponseKeyname, handlerType, command,
                    wakeWord, wakeWordsConfidence, result);
        }
    }
}
