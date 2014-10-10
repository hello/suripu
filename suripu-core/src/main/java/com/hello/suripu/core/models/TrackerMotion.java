package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.io.LittleEndianDataInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotion {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000;

    @JsonProperty("id")
    public final long id;

    @JsonProperty("account_id")
    public final long accountId;

    @JsonProperty("tracker_id")
    public final Long trackerId;

    @JsonProperty("timestamp")
    public final long timestamp;

    @JsonProperty("value")
    public final int value;

    @JsonProperty("timezone_offset")
    public final int offsetMillis;

    @JsonCreator
    public TrackerMotion(@JsonProperty("id") final long id,
                         @JsonProperty("account_id") final long accountId,
                         @JsonProperty("tracker_id") final Long trackerId,
                         @JsonProperty("timestamp") final long timestamp,
                         @JsonProperty("value") final int value,
                         @JsonProperty("timezone_offset") final int timeZoneOffset){

        this.id = id;
        this.accountId = accountId;
        this.trackerId = trackerId;
        this.timestamp = timestamp;
        this.value = value;
        this.offsetMillis = timeZoneOffset;


    }

    public static float intToFloatValue(final int value){
        return value / FLOAT_TO_INT_CONVERTER;
    }


    @Override
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final TrackerMotion convertedObject = (TrackerMotion) other;

        return   Objects.equal(this.timestamp, convertedObject.timestamp)
                && Objects.equal(this.value, convertedObject.value)
                && Objects.equal(this.offsetMillis, convertedObject.offsetMillis);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(TrackerMotion.class)
                .add("id", id)
                .add("account", accountId)
                .add("tracker_id", trackerId)
                .add("ts", timestamp)
                .add("value", value)
                .add("offset", offsetMillis)
                .toString();
    }


    public static class Utils {
        public static final double ACC_RANGE_IN_G = 4.0;
        public static final double GRAVITY_IN_MS2 = 9.81;
        public static final double ACC_RESOLUTION_32BIT = 65536.0;
        public static final double COUNTS_IN_G_SQUARE = Math.pow((ACC_RANGE_IN_G  * GRAVITY_IN_MS2)/ ACC_RESOLUTION_32BIT, 2);


        public static byte[] counterModeDecrypt(final byte[] key, final byte[] nonce, final byte[] encrypted)  // make it explicit that decryption can fail.
                throws IllegalArgumentException {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            final byte[] iv = new byte[16];
            for(int i = 0; i < nonce.length; i++){
                iv[i] = nonce[i];
            }
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            final Cipher cipher;
            try {
                cipher = Cipher.getInstance("AES/CTR/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
                final byte[] decValue = cipher.doFinal(encrypted);
                return decValue;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public static long rawToMilliMS2(final Long rawMotionAmplitude){
            final double trackerValueInMS2 = Math.sqrt(rawMotionAmplitude.doubleValue() * COUNTS_IN_G_SQUARE) - GRAVITY_IN_MS2;
            return (long)(trackerValueInMS2 * 1000);
        }

        public static long encryptedToRaw(final byte[] key, final byte[] encryptedMotionData) throws IllegalArgumentException {

            final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

            //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
            final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

            final byte[] decryptedRawMotion = counterModeDecrypt(key, nonce, encryptedRawMotion);
            final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(decryptedRawMotion));
            Exception exception = null;
            long motionAmplitude = -1;

            try {
                motionAmplitude = littleEndianDataInputStream.readInt();
                if (motionAmplitude < 0) {
                    motionAmplitude += 0xFFFFFFFF;  // Java everything is signed.
                }
            }catch (IOException ioe){
                exception = ioe;
            }

            try {
                littleEndianDataInputStream.close();
            }catch (IOException ioe){
                exception = ioe;
            }

            if(exception != null){
                throw new IllegalArgumentException(exception);
            }

            return motionAmplitude;

        }
    }
}
