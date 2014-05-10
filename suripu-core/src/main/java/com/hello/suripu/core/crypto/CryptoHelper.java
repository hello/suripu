package com.hello.suripu.core.crypto;

import com.google.common.base.Optional;
import com.sun.jersey.core.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CryptoHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoHelper.class);
    private static final String SIGNATURE_ALGORITHM = "SHA512WithRSA";
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    public static boolean validate(final byte[] content, final byte[] clientSignature, final byte[] publicKeyBase64Encoded) {

        final Optional<PublicKey> publicKey = getPublicKeyFromBase64EncodedBytes(publicKeyBase64Encoded);
        if(!publicKey.isPresent()) {
            return false;
        }

        try {

            final Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey.get());
            signature.update(content);

            if(!signature.verify(clientSignature)) {
                LOGGER.warn("Did not recognize the signature bailing");
                return false;
            }

            return true;

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("{}", e);
        } catch (SignatureException e) {
            LOGGER.error("{}", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("{}", e);
        }

        return false;
    }

    public static Optional<PublicKey> getPublicKeyFromBase64EncodedBytes(final byte[] publicKeyBase64Encoded) {
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64Encoded));

        try {
            final KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            final PublicKey publicKey = kf.generatePublic(spec);
            return Optional.of(publicKey);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("{}", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("{}", e);
        }

        return Optional.absent();
    }

    public static Optional<byte[]> encrypt(byte[] content, final byte[] publicKeyBase64Encoded) {
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64Encoded));

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(spec));
            byte[] encryptedBytes = Base64.encode(cipher.doFinal(content));
            return Optional.of(encryptedBytes);

            // LOL JAVA EXCEPTIONS
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }
}
