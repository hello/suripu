package com.hello.suripu.core.models;

import com.google.common.io.LittleEndianDataInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 10/9/14.
 *
 * If Jackson change the AES payload format these tests should fail.
 */
public class TrackerMotionUtilTest {

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
        try {
            final byte[] actual = TrackerMotion.Utils.counterModeDecrypt(new byte[16], nonce, encrypted);
            assertThat(actual, is(expected));

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
            expectedLong = littleEndianDataInputStream.readInt();
            if(expectedLong < 0){
                expectedLong += 0xFFFFFFFF;
            }
            littleEndianDataInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        long actual = -1;
        try {
            actual = TrackerMotion.Utils.encryptedToRaw(new byte[16], encrypted);


        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        assertThat(actual, is(expectedLong));
    }
}
