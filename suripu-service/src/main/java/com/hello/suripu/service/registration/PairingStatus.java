package com.hello.suripu.service.registration;

public class PairingStatus {

    public final PairingState pairingState;
    public final String message;

    private PairingStatus(final PairingState pairingState, final String message) {
        this.pairingState = pairingState;
        this.message = message;
    }

    public static PairingStatus failed(final PairingState pairingState, final String message) {
        return new PairingStatus(pairingState, message);
    }

    public static PairingStatus ok(final PairingState pairingState) {
        return new PairingStatus(pairingState, "");
    }

    public static PairingStatus ok(final PairingState pairingState, final String debugMessage) {
        return new PairingStatus(pairingState, debugMessage);
    }

    public boolean isOk() {
        return pairingState.equals(PairingState.NOT_PAIRED) || pairingState.equals(PairingState.PAIRED_WITH_CURRENT_ACCOUNT);
    }
}
