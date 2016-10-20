package com.hello.suripu.core.sense.voice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.api.input.State;

public class VoiceMetadata {

    public enum UpdateType{
        IS_PRIMARY_USER("is_primary_user"),
        MUTED("muted"),
        VOLUME("volume");

        UpdateType(String value) {

        }
    }

    final private String senseId;
    final private Long currentAccount;
    final private Optional<Boolean> voiceEnabled;
    final private Optional<Integer> volume;

    final private Optional<Long> primaryAccountId;

    public static VoiceMetadata create(final String senseId, final Long currentAccount) {
        return new VoiceMetadata(senseId, currentAccount, null, null, null);
    }

    public static VoiceMetadata create(final String senseId, final Long currentAccount, final Long primaryAccountId) {
        return new VoiceMetadata(senseId, currentAccount, null, null, primaryAccountId);
    }

    private VoiceMetadata(final String senseId, final Long currentAccount, final Boolean voiceEnabled, final Integer volume, final Long primaryAccountId) {
        this.senseId = senseId;
        this.currentAccount = currentAccount;
        this.voiceEnabled = Optional.fromNullable(voiceEnabled);
        this.volume = Optional.fromNullable(volume);
        this.primaryAccountId = Optional.fromNullable(primaryAccountId);
    }

    public static VoiceMetadata create(final String senseId, final Long currentAccount, final Boolean muted, final Integer volume, final Long primaryAccountId) {
        return new VoiceMetadata(senseId, currentAccount, muted, volume, primaryAccountId);
    }

    public static VoiceMetadata fromSenseState(final String senseId, final Long currentAccount, final Long primaryAccountId, final State.SenseState senseState) {
        return new VoiceMetadata(senseId, currentAccount, senseState.getVoiceControlEnabled(), senseState.getVolume(), primaryAccountId);
    }

    @JsonProperty("muted")
    public Boolean muted() {
        return !voiceEnabled.or(false);
    }

    @JsonProperty("volume")
    public Integer volume(){
        return volume.or(60);
    }

    @JsonProperty("is_primary_user")
    public Boolean isPrimary() {
        return primaryAccountId.isPresent() && primaryAccountId.get().equals(currentAccount);
    }
}
