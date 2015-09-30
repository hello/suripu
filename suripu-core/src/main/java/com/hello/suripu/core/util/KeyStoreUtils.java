package com.hello.suripu.core.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Optional;
import com.hello.suripu.core.provision.PillBlobProvision;
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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
//        return null;
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

    private static byte[] blockCipher(final PrivateKey privateKey, byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // copied from http://coding.westreicher.org/?p=23
        // TODO: rewrite or check license

        byte[] scrambled;
        byte[] toReturn = new byte[0];
        int length = 128;
        byte[] buffer = new byte[128]; // RSA

        Cipher cipher = Cipher.getInstance("RSA/NONE/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        for (int i=0; i< bytes.length; i++){

            if ((i > 0) && (i % length == 0)){

                scrambled = cipher.doFinal(buffer);
                toReturn = append(toReturn,scrambled);
                int newlength = length;

                if (i + length > bytes.length) {
                    newlength = bytes.length - i;
                }

                buffer = new byte[newlength];
            }
            buffer[i%length] = bytes[i];
        }

        scrambled = cipher.doFinal(buffer);
        toReturn = append(toReturn,scrambled);

        return toReturn;
    }

    public static Optional<PrivateKey> getPrivateKeyFromS3(final AmazonS3 s3, final String bucket, final String path) {
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

    public Optional<SenseProvision> decrypt(final byte[] encryptedData) throws Exception {

        final String stuff = new String(encryptedData, "UTF-8");
        byte[] rawData = Hex.decodeHex(stuff.toCharArray());
        byte[] data = blockCipher(privateKey, rawData, Cipher.DECRYPT_MODE);

        return parse(data);
    }


    public static Optional<PillBlobProvision> decryptPill(final byte[] encryptedData, final String serialNumber) throws Exception {
        return parsePill(encryptedData, serialNumber);
    }


    private static Optional<PillBlobProvision> parsePill(final byte[] data, final String serialNumber) {
        // TODO: move this outside code. s3?
        final byte[] factoryKey = new byte[]{0x0b, 0x53, 0x55, (byte)0xfb, (byte)0xe8, 0x69, 0x7d, 0x74, (byte)0xf4, (byte)0xe0, 0x45, 0x3c, 0x4a, (byte)0xe7, 0x40, (byte)0xc4};

        // For offsets please refer to:
        // https://github.com/hello/kodobannin/blob/master/common/device_info.h
        // https://github.com/hello/masamune/blob/master/README.md

        // Offsets structure
        // typedef struct{
        //      uint32_t device_id[2];
        //      uint32_t device_address[2];
        //      uint8_t device_aes[16];
        //      uint8_t ficr[256];
        //      uint8_t sha[SHA1_DIGEST_LENGTH];
        // }__attribute__((packed)) device_encrypted_info_t;

        final byte[] nonce = new byte[16];
        final byte[] prefix = Arrays.copyOfRange(data, 8, 16);
        for(int i = 0; i < 8; i ++) {
            nonce[i] = prefix[i];
        }

        final byte[] dataStart = Arrays.copyOfRange(data, 16, 308 + 16);

        final byte[] buffer = new byte[512];
        int counter = 0;
        for(int i = 0; i < dataStart.length; i+=16) {
            final byte[] result = counterModeDecryptBasic(factoryKey, nonce, Arrays.copyOfRange(dataStart, i, i + 16), counter);
            System.arraycopy(result, 0, buffer, i, 16);
            counter++;
        }

        final byte[] pillId = Arrays.copyOfRange(buffer, 0, 8);
        final String pillIdString = Hex.encodeHexString(pillId);

        final byte[] ble = Arrays.copyOfRange(buffer, 8, 16);
        final String bleString = Hex.encodeHexString(ble);

        final byte[] aesKey = Arrays.copyOfRange(buffer, 16, 32);
        final String aesKeyString = Hex.encodeHexString(aesKey);
        //
        final byte[] ficr = Arrays.copyOfRange(buffer, 32, 288);
        final String ficrString = Hex.encodeHexString(ficr);

        final byte[] hdwrKey = Arrays.copyOfRange(buffer, 32+128, 32+128+16);
        final String hdwrKeyString = Hex.encodeHexString(hdwrKey);

        final byte[] sha1Stored = Arrays.copyOfRange(buffer,288,308);
        final String sha1StoredHex = DigestUtils.sha1Hex(sha1Stored);

        final byte[] sha1Computed = DigestUtils.sha1(Arrays.copyOfRange(buffer, 0, 288));
        final String sha1ComputedHex = DigestUtils.sha1Hex(sha1Computed);

        for(int i =0; i < sha1Stored.length; i++) {
            if(sha1Stored[i] != sha1Computed[i]) {
                return Optional.absent();
            }
        }
        return Optional.of(new PillBlobProvision(pillIdString, hdwrKeyString, serialNumber, Hex.encodeHexString(data)));
    }


    private static Optional<SenseProvision> parse(final byte[] data) {

        final byte[] pad = Arrays.copyOfRange(data, 0, 80);
        final byte[] key = Arrays.copyOfRange(data, 81, 97); // key length is 16
        final byte[] deviceId = Arrays.copyOfRange(data,98, 106);
        final byte[] sha1 = Arrays.copyOfRange(data,107, 127);

        final String deviceIdString = Hex.encodeHexString(deviceId).toUpperCase();
        final String aesKeyHex = Hex.encodeHexString(key).toUpperCase();

        final byte[] checksum = DigestUtils.sha1(Arrays.copyOfRange(data, 81, data.length - 21));
        final String sha1Hex = DigestUtils.sha1Hex(checksum);

        for(int i =0; i < sha1.length; i++) {
            if(sha1[i] != checksum[i]) {
                return Optional.absent();
            }
        }
        final SenseProvision sense = SenseProvision.create(deviceIdString, aesKeyHex, sha1Hex);
        return Optional.of(sense);
    }


    /**
     * Decrypts AES CTR mode 16 bytes by 16
     * @param key
     * @param nonce
     * @param encrypted
     * @param counter
     * @return
     * @throws IllegalArgumentException
     */
    public static byte[] counterModeDecryptBasic(final byte[] key, final byte[] nonce, final byte[] encrypted, final int counter)  // make it explicit that decryption can fail.
            throws IllegalArgumentException {
        final SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        final ByteBuffer buf = ByteBuffer.wrap(nonce);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(8);  // Counter is the last 8 bytes
        buf.putLong(counter);

        final IvParameterSpec ivParameterSpec = new IvParameterSpec(buf.array());

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
}
