package com.hello.suripu.service;

import com.google.common.base.Optional;
import com.hello.suripu.service.registration.FailedToSignException;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

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
         *  3. Decrypt signature with key
         *  4. Compare sha
         *
         */

        if(body.length < (SIG_LENGTH + IV_LENGTH)) {
            LOGGER.error("Body length is less than sum of signature and IV");
            throw new RuntimeException("Invalid content");
        }

        final int sigStartIndex = body.length - SIG_LENGTH;
        final int ivStartIndex = sigStartIndex - IV_LENGTH;

        final byte[] pb = Arrays.copyOfRange(body, 0, ivStartIndex) ;
        final byte[] IV = Arrays.copyOfRange(body, ivStartIndex, sigStartIndex);
        final byte[] sig = Arrays.copyOfRange(body, sigStartIndex, body.length);

        return new SignedMessage(pb, IV, sig, "");
    }

    public Optional<Error> validateWithKey(@NotNull byte[] key) {
        final StringBuilder sb = new StringBuilder();

        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(body);
            final byte[] output = md.digest();

            LOGGER.trace("HexDigest: {}", new String(Hex.encodeHex(output)));
            LOGGER.trace("Output length: {}", output.length);

            final byte[] padded = new byte[32];
            for(int i = 0; i < output.length; i++) {
                padded[i] = output[i];
            }

            final String paddedHex =  new String(Hex.encodeHex(padded));
            sb.append("padded hex: " + paddedHex);
            sb.append("\n");

            LOGGER.trace("padded: {}", paddedHex);

            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV));

            final byte[] decryptedBytes = cipher.doFinal(sig);

            for(int i = 0; i < 20; i ++) {
                if(decryptedBytes[i] != output[i]) {
                    return Optional.of(new Error(sb.toString()));
                }
            }


            sb.append("Received sig: " + new String(Hex.encodeHex(sig)));
            sb.append("\n");
            sb.append("Decrypted sha: " + new String(Hex.encodeHex(decryptedBytes)));
            sb.append("\n");

            LOGGER.trace("Sig: {}", sig);
            return Optional.absent();

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException exception) {
            LOGGER.error(exception.getMessage());
            sb.append(exception.getMessage());
            sb.append("\n");
        } catch (BadPaddingException exception) {
            LOGGER.error(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage());
        }

        LOGGER.error("Signatures don't match");
        return Optional.of(new Error(sb.toString()));
    }


    /**
     * Sign message for Sense
     * Format = IV + sig + pb
     * @param body
     * @param key
     * @return
     */
    public static Optional<byte[]> sign(final byte[] body, final byte[] key) {
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

            byteArrayOutputStream.write(IV);
            byteArrayOutputStream.write(sig);
            byteArrayOutputStream.write(body);

            final byte[] data = byteArrayOutputStream.toByteArray();

            LOGGER.trace("Body = {}", Hex.encodeHex(data));
            return Optional.of(data);

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage());
        } catch (NoSuchPaddingException e) {
            LOGGER.error(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.error(e.getMessage());
        } catch (InvalidKeyException e) {
            LOGGER.error(e.getMessage());
        } catch (BadPaddingException e) {
            LOGGER.error(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
    }

    public static byte[] signUnchecked(final byte[] body, final byte[] key) {
        final Optional<byte[]> bytes = sign(body, key);
        if(bytes.isPresent()) {
            return bytes.get();
        }
        throw new FailedToSignException("Failed to sign");

    }
}
