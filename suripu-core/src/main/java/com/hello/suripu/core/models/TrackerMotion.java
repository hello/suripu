package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.primitives.UnsignedInts;
import com.hello.suripu.api.ble.SenseCommandProtos;
import org.joda.time.DateTimeZone;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


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

    @JsonProperty("motion_range")
    public final Long motionRange;

    @JsonProperty("kickoff_counts")
    public final Long kickOffCounts;

    @JsonProperty("on_duration_seconds")
    public final Long onDurationInSeconds;

    @JsonCreator
    // TODO: make constructor private and force Builder use to reduce risks on not
    // TODO: converting data properly
    public TrackerMotion(@JsonProperty("id") final long id,
                         @JsonProperty("account_id") final long accountId,
                         @JsonProperty("tracker_id") final Long trackerId,
                         @JsonProperty("timestamp") final long timestamp,
                         @JsonProperty("value") final int value,
                         @JsonProperty("timezone_offset") final int timeZoneOffset,
                         final Long motionRange,
                         final Long kickOffCounts,
                         final Long onDurationInSeconds){

        this.id = id;
        this.accountId = accountId;
        this.trackerId = trackerId;
        this.timestamp = timestamp;
        this.value = value;
        this.offsetMillis = timeZoneOffset;


        this.motionRange = motionRange;
        this.kickOffCounts = kickOffCounts;
        this.onDurationInSeconds = onDurationInSeconds;
    }


    public static TrackerMotion create(final SenseCommandProtos.pill_data pill_data, final DeviceAccountPair accountPair, final DateTimeZone timeZone, final byte[] encryptionKey) throws InvalidEncryptedPayloadException{
        final PillPayloadV2 payloadV2 = TrackerMotion.data(pill_data, encryptionKey, accountPair.externalDeviceId);
        final Long timestampInMillis = Utils.convertTimestampInSecondsToTimestampInMillis(pill_data.getTimestamp());
        final Integer timeZoneOffset = timeZone.getOffset(timestampInMillis);

        return new TrackerMotion(
                0L,
                accountPair.accountId,
                accountPair.internalDeviceId,
                timestampInMillis,
                Utils.rawToMilliMS2(payloadV2.maxAmplitude),
                timeZoneOffset,
                payloadV2.motionRange,
                payloadV2.kickOffCounts,
                payloadV2.onDurationInSeconds
        );
    }


    public static PillPayloadV2 data(SenseCommandProtos.pill_data data, final byte[] encryptionKey, final String pillId) throws InvalidEncryptedPayloadException{
        if(data.hasMotionDataEntrypted()) {
            switch(data.getFirmwareVersion()) {
                case 0:
                case 1:
                    return Utils.encryptedToRaw(encryptionKey, data.getMotionDataEntrypted().toByteArray());
                case 2:
                case 3: // cleans up error code, new interrupt
                    return Utils.encryptedToRawVersion2(encryptionKey, data.getMotionDataEntrypted().toByteArray());
            }
        }

        final String message = String.format("%s No motion data present or bad firmware version. Is this a hearbeat?", pillId);
        throw new IllegalArgumentException(message);
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

    public static class InvalidEncryptedPayloadException extends Exception {
        public InvalidEncryptedPayloadException(final String message) {
            super(message);
        }
    }

    public static class PillPayloadV2 {
        public final Long maxAmplitude;
        public final Long motionRange;
        public final Long kickOffCounts;
        public final Long onDurationInSeconds;

        public PillPayloadV2(final Long maxAmplitude, final Long motionRange, final Long kickOffCounts, final Long onDurationInSeconds) {
            this.maxAmplitude = maxAmplitude;
            this.motionRange = motionRange;
            this.kickOffCounts = kickOffCounts;
            this.onDurationInSeconds = onDurationInSeconds;
        }
    }

    public static class Builder{

        private long id = 0L;
        private long accountId;
        private Long trackerId;
        private long timestamp;
        private int valueInMilliMS2 = -1;
        private int offsetMillis;
        private Long motionRange = 0L;
        private Long kickOffCounts = 0L;
        private Long onDurationInSeconds = 0L;

        public Builder(){

        }

        public Builder withId(final Long id) {
            this.id = id;
            return this;
        }

        public Builder withAccountId(final long accountId){
            this.accountId = accountId;
            return this;
        }

        public Builder withTrackerId(final Long internalPillId){
            this.trackerId = internalPillId;
            return this;
        }

        public Builder withTimestampMillis(final long timestamp){
            this.timestamp = timestamp;
            return this;
        }

        public Builder withValue(final int valueInMilliMS2){
            this.valueInMilliMS2 = valueInMilliMS2;
            return this;
        }

        public Builder withOffsetMillis(final int offsetMillis){
            this.offsetMillis = offsetMillis;
            return this;
        }

        public Builder withMotionRange(final Long motionRange) {
            this.motionRange = motionRange;
            return this;
        }

        public Builder withKickOffCounts(final Long kickOffCounts) {
            this.kickOffCounts = kickOffCounts;
            return this;
        }


        public Builder withOnDurationInSeconds(final Long onDurationInSeconds) {
            this.onDurationInSeconds = onDurationInSeconds;
            return this;
        }

        public TrackerMotion build(){
            return new TrackerMotion(
                    this.id,
                    this.accountId,
                    this.trackerId,
                    this.timestamp,
                    this.valueInMilliMS2,
                    this.offsetMillis,
                    this.motionRange,
                    this.kickOffCounts,
                    this.onDurationInSeconds);
        }



    }


    public static class Utils {
        public static final double ACC_RANGE_IN_G = 4.0;
        public static final double GRAVITY_IN_MS2 = 9.81;
        public static final double ACC_RESOLUTION_32BIT = 65536.0;
        public static final double COUNTS_IN_G_SQUARE = Math.pow((ACC_RANGE_IN_G  * GRAVITY_IN_MS2)/ ACC_RESOLUTION_32BIT, 2);



        public static Long convertTimestampInSecondsToTimestampInMillis(final Long timestampInSeconds) {
            return timestampInSeconds * 1000L;
        }

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

        public static int rawToMilliMS2(final Long rawMotionAmplitude){
            final double trackerValueInMS2 = Math.sqrt(rawMotionAmplitude) * Math.sqrt(COUNTS_IN_G_SQUARE) - GRAVITY_IN_MS2;
            return (int)(trackerValueInMS2 * 1000);
        }

        public static PillPayloadV2 encryptedToRaw(final byte[] key, final byte[] encryptedMotionData) throws InvalidEncryptedPayloadException {

            final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

            //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
            final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

            final byte[] decryptedRawMotion = counterModeDecrypt(key, nonce, encryptedRawMotion);

            // check for magic bytes 5A5A added by the pill
            // fail if they don't match
            // Only pill DVT has magic bytes, so check length to ensure only pill DVT fails if we don't find magic bytes
            if(decryptedRawMotion.length > 4 && decryptedRawMotion[decryptedRawMotion.length -1] != 90 && decryptedRawMotion[decryptedRawMotion.length -2] != 90) {
                throw new InvalidEncryptedPayloadException("Magic bytes don't match");
            }
            final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(decryptedRawMotion));
            Exception exception = null;
            long motionAmplitude = -1;

            try {
                motionAmplitude = UnsignedInts.toLong(littleEndianDataInputStream.readInt());

            }catch (IOException ioe){
                exception = ioe;
            }finally {
                try {
                    littleEndianDataInputStream.close();
                }catch (IOException ioe){
                    exception = ioe;
                }
            }



            if(exception != null){
                throw new InvalidEncryptedPayloadException(exception.getMessage());
            }

            return new PillPayloadV2(motionAmplitude,0L,0L,0L);
        }


        public static PillPayloadV2 encryptedToRawVersion2(final byte[] key, final byte[] encryptedMotionData) throws InvalidEncryptedPayloadException {

            final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

            //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
            final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

            final byte[] decryptedRawMotion = counterModeDecrypt(key, nonce, encryptedRawMotion);

            // check for magic bytes 5A5A added by the pill
            // fail if they don't match
            // Only pill DVT has magic bytes, so check length to ensure only pill DVT fails if we don't find magic bytes
            if(decryptedRawMotion.length > 4 && decryptedRawMotion[decryptedRawMotion.length -1] != 0x5A &&
                    decryptedRawMotion[decryptedRawMotion.length -2] != 0x5A) {
                throw new InvalidEncryptedPayloadException("Magic bytes don't match");
            }
            final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(decryptedRawMotion));

            Exception exception = null;
            long motionAmplitude = -1;
            long maxAccelerationRange = 0;
            long kickOffTimePerMinute = 0;
            long motionDurationInSecond = 0;

            try {
                motionAmplitude = UnsignedInts.toLong(littleEndianDataInputStream.readInt());
                maxAccelerationRange = littleEndianDataInputStream.readShort() & 0xFFFFL;
                kickOffTimePerMinute = littleEndianDataInputStream.readByte() & 0xFFL;
                motionDurationInSecond = littleEndianDataInputStream.readByte() & 0xFFL;

            }catch (IOException ioe){
                exception = ioe;
            }finally {
                try {
                    littleEndianDataInputStream.close();
                }catch (IOException ioe){
                    exception = ioe;
                }
            }

            if(exception != null){
                throw new InvalidEncryptedPayloadException(exception.getMessage());
            }

            return new PillPayloadV2(motionAmplitude, maxAccelerationRange, kickOffTimePerMinute, motionDurationInSecond);

        }


        public static List<TrackerMotion> removeDuplicates(final List<TrackerMotion> original){
            final LinkedList<TrackerMotion> noDuplicateList = new LinkedList<>();
            for(final TrackerMotion datum:original){
                if(noDuplicateList.size() == 0){
                    noDuplicateList.add(datum);
                    continue;
                }

                if(noDuplicateList.getLast().timestamp == datum.timestamp){
                    continue;
                }
                noDuplicateList.add(datum);
            }

            return noDuplicateList;
        }
    }
}
