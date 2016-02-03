package com.hello.suripu.core.models;

import com.google.common.io.LittleEndianDataInputStream;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class PillProxData {

    public final Long accountId;
    public final String pillId;
    public final Integer proxValue;
    public final DateTime ts;
    public final Integer offsetMillis = 0;   // TODO: LOL

    private PillProxData(final Long accountId, final String pillId, final Integer proxValue, final DateTime ts) {
        this.accountId = accountId;
        this.pillId = pillId;
        this.proxValue = proxValue;
        this.ts = ts;
    }

    public static PillProxData create(final Long accountId, final String pillId, final Integer proxValue, final DateTime ts) {
        return new PillProxData(accountId, pillId, proxValue,ts);
    }

    public static PillProxData fromEncryptedData(final String pillId, final byte[] encryptedProxData, final byte[] key, final DateTime sampleTime) {
        final byte[] decryptedProxData = decryptProxData(key, encryptedProxData);
        try (final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(decryptedProxData))) {
            final int proxValue = littleEndianDataInputStream.readInt();
            return new PillProxData(0L, pillId, proxValue, sampleTime);
        } catch (IOException e) {
            throw new IllegalArgumentException("server can't parse prox data");
        }

    }

    public static byte[] decryptProxData(final byte[] key, final byte[] encryptedMotionData) {
        final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

        //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
        final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

        final byte[] decryptedRawMotion = TrackerMotion.Utils.counterModeDecrypt(key, nonce, encryptedRawMotion);
        return decryptedRawMotion;
    }
}
