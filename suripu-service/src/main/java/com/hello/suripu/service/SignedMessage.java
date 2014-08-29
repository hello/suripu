package com.hello.suripu.service;

import com.google.common.base.Optional;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SignedMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignedMessage.class);

    private static final Integer IV_LENGTH = 16;
    private static final Integer SIG_LENGTH = 32;

    public final byte[] body;
    public final byte[] IV;
    public final byte[] sig;

    public final String debugMessage;

    public static class Error {
        public final String message;

        public Error(final String message) {
            this.message = message;
        }
    }

    public SignedMessage(byte[] body, byte[] IV, byte[] sig, String debugMessage) {
        this.body = body;
        this.IV = IV;
        this.sig = sig;
        this.debugMessage = debugMessage;
    }

    public static SignedMessage parse(byte[] body) {


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
         *  3. Encrypt hash with key
         *  4. Compare signature
         *
         */

        final int sigStartIndex = body.length - SIG_LENGTH;
        final int ivStartIndex = sigStartIndex - IV_LENGTH;

        final byte[] pb = Arrays.copyOfRange(body, 0, ivStartIndex) ;
        final byte[] IV = Arrays.copyOfRange(body, ivStartIndex, sigStartIndex);
        final byte[] sig = Arrays.copyOfRange(body, sigStartIndex, body.length);

        return new SignedMessage(pb, IV, sig, "");
    }

    public Optional<Error> validateWithKey(byte[] key) {
        final StringBuilder sb = new StringBuilder();

        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(body);
            byte[] output = md.digest();

            LOGGER.debug("HexDigest: {}", new String(Hex.encodeHex(output)));
            LOGGER.debug("Output length: {}", output.length);

            byte[] padded = new byte[32];
            for(int i = 0; i < output.length; i++) {
                padded[i] = output[i];
            }

            final String paddedHex =  new String(Hex.encodeHex(padded));
            sb.append("padded hex: " + paddedHex);
            sb.append("\n");

            LOGGER.debug("padded: {}", paddedHex);

            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV));

            byte[] decryptedBytes = cipher.doFinal(sig);

            for(int i = 0; i < 20; i ++) {
                if(decryptedBytes[i] != output[i]) {
                    return Optional.of(new Error(sb.toString()));
                }
            }


            sb.append("Received sig: " + new String(Hex.encodeHex(sig)));
            sb.append("\n");
            sb.append("Decrypted sha: " + new String(Hex.encodeHex(decryptedBytes)));
            sb.append("\n");

            LOGGER.debug("Sig: {}", sig);
            return Optional.absent();

        } catch (NoSuchAlgorithmException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        } catch (InvalidKeyException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        } catch (NoSuchPaddingException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        } catch (BadPaddingException exception) {
            LOGGER.error(exception.getMessage());
        } catch (IllegalBlockSizeException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        } catch (InvalidAlgorithmParameterException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        }

        LOGGER.error("Signatures don't match");
        return Optional.of(new Error(sb.toString()));
    }
}
