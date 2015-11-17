package com.hello.suripu.core.models;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.primitives.UnsignedInts;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 10/9/14.
 *
 * If Jackson change the AES payload format these tests should fail.
 */
public class TrackerMotionUtilTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerMotionUtilTest.class);

    @Test
    public void testRawToMilliG(){
        final long countPerG = 65536 / 4;
        long actual = TrackerMotion.Utils.rawToMilliMS2(countPerG * countPerG);
        long expected = 0;
        assertThat(actual, is(expected));


        final long oneGUp = countPerG - countPerG;
        actual = TrackerMotion.Utils.rawToMilliMS2(oneGUp * oneGUp);

        // The positive direction of Z axis is point down
        expected = -(long)(1000 * TrackerMotion.Utils.GRAVITY_IN_MS2);
        assertThat(actual, is(expected));
    }

    @Test
    public void testAESDecrypt(){
        // Nonce: 3D F9 53 77 85 CF BD C0
        // Raw Payload: 67 5B B0 69
        // AES: 04 00  bytes
        // Encrypted Payload: 3D F9 53 77 85 CF BD C0 FE D2 70 18

        final byte[] nonce = new byte[]{0x3D, (byte)0xF9, 0x53, 0x77,
                (byte)0x85, (byte)0xCF, (byte)0xBD, (byte)0xC0};

        final byte[] encrypted = new byte[]{(byte)0xFE, (byte)0xD2, 0x70, 0x18};
        final byte[] expected = new byte[]{0x67, 0x5B, (byte)0xB0, 0x69};
        final byte[] actual = TrackerMotion.Utils.counterModeDecrypt(new byte[16], nonce, encrypted);
        assertThat(actual, is(expected));
    }

    @Test
    public void testBadAESDecrypt(){
        // Nonce: 3D F9 53 77 85 CF BD C0
        // Raw Payload: 67 5B B0 69
        // AES: 04 00  bytes
        // Encrypted Payload: 3D F9 53 77 85 CF BD C0 FE D2 70 18

        final byte badByte = 0x00;
        final byte[] nonce = new byte[]{badByte, (byte)0xF9, 0x53, 0x77,
                (byte)0x85, (byte)0xCF, (byte)0xBD, (byte)0xC0};

        final byte[] encrypted = new byte[]{(byte)0xFE, (byte)0xD2, 0x70, 0x18};
        final byte[] expected = new byte[]{0x67, 0x5B, (byte)0xB0, 0x69};
        final byte[] actual = TrackerMotion.Utils.counterModeDecrypt(new byte[16], nonce, encrypted);
        assertThat(Arrays.equals(actual, expected), is(false));
    }

    @Test
    public void testDecryptEncryptedPillData(){
        // Nonce: 3D F9 53 77 85 CF BD C0
        // Raw Payload: 67 5B B0 69
        // AES: 04 00  bytes
        // Encrypted Payload: 3D F9 53 77 85 CF BD C0 FE D2 70 18

        final byte[] encrypted = new byte[]{0x3D, (byte)0xF9, 0x53, 0x77,
                (byte)0x85, (byte)0xCF, (byte)0xBD, (byte)0xC0, (byte)0xFE, (byte)0xD2, 0x70, 0x18};
        final byte[] expectedBytes = new byte[]{0x67, 0x5B, (byte)0xB0, 0x69};
        final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(expectedBytes));
        long expectedLong = 0;
        try {
            expectedLong = UnsignedInts.toLong(littleEndianDataInputStream.readInt());
            littleEndianDataInputStream.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        try {
            final byte[] decrypted = TrackerMotion.Utils.decryptRawMotion(new byte[16], encrypted);
            final TrackerMotion.PillPayloadV2 payloadV2 = TrackerMotion.Utils.decryptedToRaw(decrypted);
            assertThat(payloadV2.maxAmplitude, is(expectedLong));
        } catch (TrackerMotion.InvalidEncryptedPayloadException exception) {
            LOGGER.error("Fail to decrypt tracker motion payload");
        }

    }


    private byte[] bytes(int... ints) {
        final byte[] output = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            output[i] = (byte) ints[i];
        }
        return output;
    }

    @Test
    public void testDecryptedToRaw() throws Exception {
        // Little endian
        final byte[] decrypted = bytes(0x07, 0x01, 0x00, 0x00);
        final TrackerMotion.PillPayloadV2 payloadV2 = TrackerMotion.Utils.decryptedToRaw(decrypted);
        assertThat(payloadV2.motionMask.isPresent(), is(false));
        assertThat(payloadV2.cosTheta.isPresent(), is(false));
        assertThat(payloadV2.maxAmplitude, is(263L));
    }

    @Test
    public void testDecryptedToRawVersion2() throws Exception {
        // Little endian
        final byte[] decrypted = bytes(
                0x07, 0x01, 0x00, 0x00, // max amplitude
                0x00, 0x01, // max acceleration range
                0x0F, // kickoff per minute
                0x0A // Motion duration
        );
        final TrackerMotion.PillPayloadV2 payloadV2 = TrackerMotion.Utils.decryptedToRawVersion2(decrypted);
        assertThat(payloadV2.motionMask.isPresent(), is(false));
        assertThat(payloadV2.cosTheta.isPresent(), is(false));
        assertThat(payloadV2.maxAmplitude, is(263L));
        assertThat(payloadV2.motionRange, is(256L));
        assertThat(payloadV2.kickOffCounts, is(15L));
        assertThat(payloadV2.onDurationInSeconds, is(10L));
    }

    @Test
    public void testDecryptedToRawVersion3() throws Exception {
        // Little endian, remember
        final byte[] decrypted = bytes(
                0x00, 0x02,  // max amplitude
                0x11, // cosTheta
                0x11, 0x00, 0x03, 0x00, 0x11, 0x00, 0x03, 0x00 // motionMask
        );
        final TrackerMotion.PillPayloadV2 payloadV2 = TrackerMotion.Utils.decryptedToRawVersion3(decrypted);
        assertThat(payloadV2.onDurationInSeconds, is(8L));
        assertThat(payloadV2.maxAmplitude, is(512L));
        assertThat(payloadV2.cosTheta.get(), is(17L));
        assertThat(payloadV2.motionMask.get(), is(0x0003001100030011L));
    }



    // F4 A1 F4 34 0D BF 59 9A 91 C6 1F 72 40 64 E6 50

    @Test
    public void testDecryptEncryptedWithMagicBytesPillData() throws DecoderException {
        // Nonce: C1 B7 32 6C 51 87 66 30
        // Raw Payload:
        // AES: F4 A1 F4 34 0D BF 59 9A 91 C6 1F 72 40 64 E6 50
        // Encrypted Payload: CD BA FA B3 5B 3E


        final byte[] encrypted = new byte[]{(byte) 0xC1, (byte) 0xB7, (byte) 0x32, (byte) 0x6C, (byte) 0x51, (byte) 0x87, (byte)0x66, (byte) 0x30,
                (byte) 0xCD, (byte)0xBA, (byte) 0xFA, (byte) 0xB3,(byte)0x5B, (byte)0x3E};
        final byte[] expectedBytes = new byte[]{0x67, 0x5B, (byte)0xB0, 0x69};
        final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(new ByteArrayInputStream(expectedBytes));
        long expectedLong = 0;
        try {
            expectedLong = UnsignedInts.toLong(littleEndianDataInputStream.readInt());
            littleEndianDataInputStream.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        byte[] key = Hex.decodeHex("F4A1F4340DBF599A91C61F724064E650".toCharArray());
        try {
            final byte[] decrypted = TrackerMotion.Utils.decryptRawMotion(key, encrypted);
            TrackerMotion.PillPayloadV2 payloadV2 = TrackerMotion.Utils.decryptedToRaw(decrypted);
            assertThat(payloadV2.maxAmplitude == 0, is(false));
        } catch (TrackerMotion.InvalidEncryptedPayloadException exception) {
            LOGGER.debug("Fail to decrypt tracker motion");
        }

    }
}
