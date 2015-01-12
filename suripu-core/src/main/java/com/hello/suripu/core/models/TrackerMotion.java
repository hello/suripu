package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.primitives.UnsignedInts;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.api.input.InputProtos;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    public static class Builder{

        private long id = 0L;
        private long accountId;
        private Long trackerId;
        private long timestamp;
        private int valueInMilliMS2 = -1;
        private int offsetMillis;

        public Builder(){

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

        /*
        * Take data from Morpheus and transform to core TrackerMotion data structure.
         */
        public Builder withPillKinesisData(final byte[] key, final InputProtos.PillDataKinesis data){

            final Long accountID = data.hasAccountIdLong() ? data.getAccountIdLong() : Long.parseLong(data.getAccountId());
            final Long pillID = data.hasPillIdLong() ? data.getPillIdLong() : Long.parseLong(data.getPillId());
            final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            long amplitudeMilliG = -1;
            if(data.hasValue()){
                amplitudeMilliG = data.getValue();
            }

            if(data.hasEncryptedData()){
                final byte[] encryptedData = data.getEncryptedData().toByteArray();

                final long raw = Utils.encryptedToRaw(key, encryptedData);
                amplitudeMilliG = Utils.rawToMilliMS2(raw);
            }

            this.withAccountId(accountID);
            this.withTrackerId(pillID);
            this.withTimestampMillis(sampleDT.getMillis());
            this.withValue((int)amplitudeMilliG);
            this.withOffsetMillis(data.getOffsetMillis());

            return this;
        }

        /*
        * Take data from Morpheus and transform to core TrackerMotion data structure.
         */
        public Builder withPillKinesisDataVersion2(final byte[] key, final InputProtos.PillDataKinesis data){

            final Long accountID = data.hasAccountIdLong() ? data.getAccountIdLong() : Long.parseLong(data.getAccountId());
            final Long pillID = data.hasPillIdLong() ? data.getPillIdLong() : Long.parseLong(data.getPillId());
            final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
            long amplitudeMilliG = -1;

            if(data.hasEncryptedData()){
                final byte[] encryptedData = data.getEncryptedData().toByteArray();

                final long[] featureVector = Utils.encryptedToRawVersion2(key, encryptedData);
                amplitudeMilliG = Utils.rawToMilliMS2(featureVector[0]);
            }

            this.withAccountId(accountID);
            this.withTrackerId(pillID);
            this.withTimestampMillis(sampleDT.getMillis());
            this.withValue((int)amplitudeMilliG);
            this.withOffsetMillis(data.getOffsetMillis());

            return this;
        }

        public Builder withEncryptedValue(final byte[] key, final SenseCommandProtos.pill_data pillData) {
            long amplitudeMilliG = -1;
            if(pillData.hasMotionDataEntrypted()){
                final byte[] encryptedData = pillData.getMotionDataEntrypted().toByteArray();

                final long raw = Utils.encryptedToRaw(key, encryptedData);
                amplitudeMilliG = Utils.rawToMilliMS2(raw);
            }
            this.withValue((int) amplitudeMilliG);
            return this;
        }

        public TrackerMotion build(){
            return new TrackerMotion(this.id, this.accountId, this.trackerId, this.timestamp, this.valueInMilliMS2, this.offsetMillis);
        }



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
            final double trackerValueInMS2 = Math.sqrt(rawMotionAmplitude) * Math.sqrt(COUNTS_IN_G_SQUARE) - GRAVITY_IN_MS2;
            return (long)(trackerValueInMS2 * 1000);
        }

        public static long encryptedToRaw(final byte[] key, final byte[] encryptedMotionData) throws IllegalArgumentException {

            final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

            //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
            final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

            final byte[] decryptedRawMotion = counterModeDecrypt(key, nonce, encryptedRawMotion);

            // check for magic bytes 5A5A added by the pill
            // fail if they don't match
            // Only pill DVT has magic bytes, so check length to ensure only pill DVT fails if we don't find magic bytes
            if(decryptedRawMotion.length > 4 && decryptedRawMotion[decryptedRawMotion.length -1] != 90 && decryptedRawMotion[decryptedRawMotion.length -2] != 90) {
                throw new IllegalArgumentException("Magic bytes don't match");
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
                throw new IllegalArgumentException(exception);
            }

            return motionAmplitude;

        }


        public static long[] encryptedToRawVersion2(final byte[] key, final byte[] encryptedMotionData) throws IllegalArgumentException {

            final byte[] nonce = Arrays.copyOfRange(encryptedMotionData, 0, 8);

            //final byte[] crc = Arrays.copyOfRange(encryptedMotionData, encryptedMotionData.length - 1 - 2, encryptedMotionData.length);  // Not used yet
            final byte[] encryptedRawMotion = Arrays.copyOfRange(encryptedMotionData, 8, encryptedMotionData.length);

            final byte[] decryptedRawMotion = counterModeDecrypt(key, nonce, encryptedRawMotion);

            // check for magic bytes 5A5A added by the pill
            // fail if they don't match
            // Only pill DVT has magic bytes, so check length to ensure only pill DVT fails if we don't find magic bytes
            if(decryptedRawMotion.length > 4 && decryptedRawMotion[decryptedRawMotion.length -1] != 0x5A &&
                    decryptedRawMotion[decryptedRawMotion.length -2] != 0x5A) {
                throw new IllegalArgumentException("Magic bytes don't match");
            }
            final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(decryptedRawMotion));
            Exception exception = null;
            long motionAmplitude = -1;
            long maxAccelerationRange = 0;
            long kickOffTimePerMinute = 0;
            long motionDurationInSecond = 0;

            try {
                motionAmplitude = UnsignedInts.toLong(littleEndianDataInputStream.readInt());
                maxAccelerationRange = UnsignedInts.toLong(littleEndianDataInputStream.readShort());
                kickOffTimePerMinute = UnsignedInts.toLong(littleEndianDataInputStream.readByte());
                motionDurationInSecond = UnsignedInts.toLong(littleEndianDataInputStream.readByte());

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
                throw new IllegalArgumentException(exception);
            }

            return new long[]{ motionAmplitude, maxAccelerationRange, kickOffTimePerMinute, motionDurationInSecond };

        }
    }
}
