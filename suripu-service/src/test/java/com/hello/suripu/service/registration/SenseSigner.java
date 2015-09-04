package com.hello.suripu.service.registration;

import com.hello.suripu.service.SignedMessage;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

public class SenseSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenseSigner.class);

    private static final Integer IV_LENGTH = 16;
    private static final Integer SIG_LENGTH = 32;


    public static SignedMessage reverseParse(byte[] body) {


        /**
         * Format [PB bytes][IV (16 bytes)][Sig (32 bytes)]
         *
         *  On Morpheus
         *
         *  1. The protobuf bytes are sha-1'd by Sense
         *  2. This hash (20 bytes) is padded to be a multiple of 16 bytes (in our case 32 bytes)
         *  3. Morpheus encrypts the hash with our unique per user shared key – also stored in our DB
         *
         *
         *  On server
         *
         *  1. Extract pb body, IV and signature
         *  2. Sha-1 pb body
         *  3. Decrypt signature with key
         *  4. Compare sha
         *
         */

        if(body.length < (SIG_LENGTH + IV_LENGTH)) {
            LOGGER.error("Body length is less than sum of signature and IV");
            throw new RuntimeException("Invalid content");
        }

        /*
                    byteArrayOutputStream.write(IV);
            byteArrayOutputStream.write(sig);
            byteArrayOutputStream.write(body);
         */

        final int bodyStartIndex = IV_LENGTH + SIG_LENGTH;
        final int sigStartIndex = IV_LENGTH;
        final int ivStartIndex = 0;

        final byte[] pb = Arrays.copyOfRange(body, bodyStartIndex, body.length) ;
        final byte[] IV = Arrays.copyOfRange(body, ivStartIndex, sigStartIndex);
        final byte[] sig = Arrays.copyOfRange(body, sigStartIndex, bodyStartIndex);

        return new SignedMessage(pb, IV, sig, "");
    }


    /**
     * Sign message for Sense
     * Format = IV + sig + pb
     * @param body
     * @param key
     * @return
     */
    public static byte[] sign(final byte[] body, final byte[] key) {
        final Random r = new SecureRandom();
        final byte[] IV = new byte[IV_LENGTH];
        r.nextBytes(IV);
        LOGGER.trace("random IV = {}", Hex.encodeHex(IV));

        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            final MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(body);
            final byte[] output = md.digest();
            LOGGER.trace("Sha = {}", Hex.encodeHex(output));

            final byte[] paddedSha = new byte[32];
            for(int i= 0; i < paddedSha.length; i++) {
                if(i < output.length) {
                    paddedSha[i] = output[i];
                } else {
                    paddedSha[i] = 0;
                }
            }

            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV));

            final byte[] sig = cipher.doFinal(paddedSha);

            LOGGER.trace("Sig = {}", Hex.encodeHex(sig));

            byteArrayOutputStream.write(body);
            byteArrayOutputStream.write(IV);
            byteArrayOutputStream.write(sig);

            final byte[] data = byteArrayOutputStream.toByteArray();

            LOGGER.trace("Body = {}", Hex.encodeHex(data));
            return data;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
