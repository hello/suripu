package com.hello.suripu.core.speech;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Created by ksg on 7/20/16
 */
public class SpeechToTextResult {

    @JsonIgnore
    public final Long accountId;

    @JsonProperty("datetime_utc")
    public final DateTime dateTimeUTC;

    @JsonIgnore
    public final DateTime updatedUTC;

    @JsonIgnore
    public final String senseId;

    @JsonIgnore
    public final String audioIdentifier;  // uuid string of audio file in S3

    @JsonProperty("text")
    public final String text;   // transcribed text

    @JsonProperty("response_text")
    public final String responseText; // Sense response

    @JsonIgnore
    public final SpeechToTextService service;

    @JsonIgnore
    public final float confidence;

    // maybe for the next 3
    @JsonIgnore
    public final Intention.IntentType intent;

    @JsonIgnore
    public final Intention.ActionType action;

    @JsonIgnore
    public final Intention.IntentCategory intentCategory;

    @JsonProperty("command")
    public final String command; // TODO: this should probably be a class or something

    @JsonIgnore
    public final WakeWord wakeWord;

    @JsonIgnore
    public final Map<String, Float> wakeWordsConfidence;

    @JsonProperty("result")
    public final Result result;


    public SpeechToTextResult(final Long accountId,
                              final DateTime dateTimeUTC,
                              final DateTime updatedUTC,
                              final String senseId,
                              final String audioIdentifier,
                              final String text,
                              final String responseText,
                              final SpeechToTextService service,
                              final float confidence,
                              final Intention.IntentType intent,
                              final Intention.ActionType action,
                              final Intention.IntentCategory intentCategory,
                              final String command,
                              final WakeWord wakeWord,
                              final Map<String, Float> wakeWordsConfidence,
                              final Result result) {
        this.accountId = accountId;
        this.dateTimeUTC = dateTimeUTC;
        this.updatedUTC = updatedUTC;
        this.senseId = senseId;
        this.audioIdentifier = audioIdentifier;
        this.text = text;
        this.responseText = responseText;
        this.service = service;
        this.confidence = confidence;
        this.intent = intent;
        this.action = action;
        this.intentCategory = intentCategory;
        this.command = command;
        this.wakeWord = wakeWord;
        this.wakeWordsConfidence = wakeWordsConfidence;
        this.result = result;

        // TODO: add checks for not null?
    }

    public static class Builder {
        private Long accountId;
        private DateTime dateTimeUTC;
        private DateTime updatedUTC;
        private String senseId = "";
        private String audioIdentifier = "";
        private String text = "";
        private String responseText = "";
        private SpeechToTextService service = SpeechToTextService.GOOGLE;
        private float confidence = 0.0f;
        private Intention.IntentType intent = Intention.IntentType.NONE;
        private Intention.ActionType action = Intention.ActionType.NONE;
        private Intention.IntentCategory intentCategory = Intention.IntentCategory.NONE;
        private String command = "";
        private WakeWord wakeWord = WakeWord.OKAY_SENSE;
        private Map<String, Float> wakeWordsConfidence = Maps.newHashMap();
        private Result result = Result.NONE;


        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        Builder withDateTimeUTC(final DateTime dateTimeUTC) {
            this.dateTimeUTC = dateTimeUTC;
            this.updatedUTC = dateTimeUTC;
            return this;
        }

        Builder withUpdatedUTC(final DateTime updatedUTC) {
            this.updatedUTC = updatedUTC;
            return this;
        }

        Builder withSenseId (final String id) {
            this.senseId = id;
            return this;
        }

        Builder withAudioIndentifier(final String audioIdentifier) {
            this.audioIdentifier = audioIdentifier;
            return this;
        }

        Builder withText(final String text) {
            this.text = text;
            return this;
        }

        Builder withResponseText(final String text) {
            this.responseText= text;
            return this;
        }

        Builder withService(final SpeechToTextService service) {
            this.service = service;
            return this;
        }

        public Builder withConfidence(final Float confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder withIntent(final Intention.IntentType intent) {
            this.intent = intent;
            return this;
        }

        public Builder withAction(final Intention.ActionType action) {
            this.action = action;
            return this;
        }

        public Builder withIntentCategory(final Intention.IntentCategory intentCategory) {
            this.intentCategory = intentCategory;
            return this;
        }

        public Builder withCommand(final String command) {
            this.command = command;
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
                final WakeWord wakeWord = WakeWord.fromString(entry.getKey());
                if (!wakeWord.equals(WakeWord.ERROR)) {
                    this.wakeWordsConfidence.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public Builder withResult(final Result result) {
            this.result = result;
            return this;
        }

        public SpeechToTextResult build() {
            return new SpeechToTextResult(accountId,
                    dateTimeUTC, updatedUTC, senseId,
                    audioIdentifier, text, responseText, service, confidence,
                    intent, action, intentCategory, command,
                    wakeWord, wakeWordsConfidence, result);
        }
    }
}
