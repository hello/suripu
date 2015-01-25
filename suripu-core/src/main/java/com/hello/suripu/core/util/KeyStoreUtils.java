package com.hello.suripu.core.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Optional;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Arrays;

public class KeyStoreUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreUtils.class);
    private final PrivateKey privateKey;

    private KeyStoreUtils(final PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public static KeyStoreUtils build(final AmazonS3 s3, final String bucket, final String key) {
        final Optional<PrivateKey> privateKeyOptional = getPrivateKeyFromS3(s3, bucket, key);
        if(!privateKeyOptional.isPresent()) {
            throw new RuntimeException("Unable to get private key from s3");
        }

        return new KeyStoreUtils(privateKeyOptional.get());
    }

    private static byte[] append(byte[] prefix, byte[] suffix){
        byte[] toReturn = new byte[prefix.length + suffix.length];
        for (int i=0; i< prefix.length; i++){
            toReturn[i] = prefix[i];
        }
        for (int i=0; i< suffix.length; i++){
            toReturn[i+prefix.length] = suffix[i];
        }
        return toReturn;
    }

    private static byte[] blockCipher(PrivateKey privateKey, byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // string initialize 2 buffers.
        // scrambled will hold intermediate results
        byte[] scrambled;

        // toReturn will hold the total result
        byte[] toReturn = new byte[0];
        // if we encrypt we use 100 byte long blocks. Decryption requires 128 byte long blocks (because of RSA)
        int length = (mode == Cipher.ENCRYPT_MODE)? 100 : 128;

        // another buffer. this one will hold the bytes that have to be modified in this step
        byte[] buffer = new byte[length];


        Cipher cipher = Cipher.getInstance("RSA/NONE/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        for (int i=0; i< bytes.length; i++){

            // if we filled our buffer array we have our block ready for de- or encryption
            if ((i > 0) && (i % length == 0)){
                //execute the operation
                scrambled = cipher.doFinal(buffer);
                // add the result to our total result.
                toReturn = append(toReturn,scrambled);
                // here we calculate the length of the next buffer required
                int newlength = length;

                // if newlength would be longer than remaining bytes in the bytes array we shorten it.
                if (i + length > bytes.length) {
                    newlength = bytes.length - i;
                }
                // clean the buffer array
                buffer = new byte[newlength];
            }
            // copy byte into our buffer.
            buffer[i%length] = bytes[i];
        }

        // this step is needed if we had a trailing buffer. should only happen when encrypting.
        // example: we encrypt 110 bytes. 100 bytes per run means we "forgot" the last 10 bytes. they are in the buffer array
        scrambled = cipher.doFinal(buffer);

        // final step before we can return the modified data.
        toReturn = append(toReturn,scrambled);

        return toReturn;
    }

    public static Optional<PrivateKey> getPrivateKeyFromS3(AmazonS3 s3, String bucket, String path) {
        try {
            final S3Object s3Object = s3.getObject(bucket, path);
            final BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            final Reader reader = new InputStreamReader(s3Object.getObjectContent());
            final PEMReader pemReader = new PEMReader(reader);
            final KeyPair keyPair = (KeyPair) pemReader.readObject();
            final PrivateKey privateKey = keyPair.getPrivate();
            return Optional.fromNullable(privateKey);
        } catch (IOException e) {
            LOGGER.error("Failed getting private key: {}", e.getMessage());
        }
        return Optional.absent();
    }

    public Optional<SenseProvision> decrypt(byte[] encryptedData) throws Exception {

        final String stuff = new String(encryptedData, "UTF-8");
        byte[] rawData = Hex.decodeHex(stuff.toCharArray());
        byte[] data = blockCipher(privateKey, rawData, Cipher.DECRYPT_MODE);

        return parse(data);
    }


    private static Optional<SenseProvision> parse(byte[] data) {

        final byte[] pad = Arrays.copyOfRange(data, 0, 80);
        final byte[] key = Arrays.copyOfRange(data, 81, 97); // key length is 16
        final byte[] deviceId = Arrays.copyOfRange(data,98, 106);
        final byte[] sha1 = Arrays.copyOfRange(data,107, 127);

        final String deviceIdString = Hex.encodeHexString(deviceId).toUpperCase();
        final String aesKeyHex = Hex.encodeHexString(key).toUpperCase();

        byte[] checksum = DigestUtils.sha1(Arrays.copyOfRange(data, 81, data.length - 21));
        final String sha1Hex = DigestUtils.sha1Hex(checksum);

        for(int i =0; i < sha1.length; i++) {
            if(sha1[i] != checksum[i]) {
                return Optional.absent();
            }
        }
        final SenseProvision sense = SenseProvision.create(deviceIdString, aesKeyHex, sha1Hex);
        return Optional.of(sense);
    }
}
