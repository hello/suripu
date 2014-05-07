package com.hello.suripu.factory;

import com.sun.jersey.core.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Util {

    public static class Payload {
        public final String content;
        public final byte[] signature;

        public Payload(String content, byte[] signature) {
            this.content = content;
            this.signature = signature;
        }

        public void send() {
            System.out.println("Sending payload");
        }
    }

    public static void main(String[] args) throws Exception {

        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

//        final SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
//        keyGen.initialize(1024, random);

        final KeyPair pair = keyGen.generateKeyPair();

        final PrivateKey privateKey = pair.getPrivate();
        final PublicKey publicKey = pair.getPublic();


        byte[] privateBase64Bytes =  Base64.encode(privateKey.getEncoded());
        byte[] publicBase64Bytes =  Base64.encode(publicKey.getEncoded());

        /**
         *
         *
         *
         *
         *
         * MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALhC9OzB8mQknUZ4Dg1IjzxJhctmflk7NmraA8bxDlwg/OW7O8sk/sqiJIky48sf7DHfuU1gwAnqr4jiAeuc9JfgH9KtlE2h2Fg7zOrwvPFTFcJN5X0dyqwnnWr63W4w4CIlxe96PcXwhZzD/5rpqme1aY/XnLE4HL1c9L9tfCdNAgMBAAECgYAabE+fG/jcb3u2PUUWlo8jwA9Vqa74sMsO65CbGLpMb9aUQxNCAF4hRLskiY2s9TjztfdmpzcitOrTIEXSOcHBP0O61tSRhx8m3iUEUD700TlktCga9Jp/VHfj18PXV6xoe4jUCUyqeqnEASKQdO32OtP3w229vnL7jZADFWDpQQJBAOpoNCy7xcmuv2t9naG8WdtFl3VcujJtbRq1F/vcSUwWRjhyQHCFF+IMXAd3dLtruRwavrDlS4NcqZ6a6ZdfvBkCQQDJPDhIv3XR7uwv0NeZHVP2eQB5EFS9/2Eu0KvqlUx1jD0C62vcbf6KklmN/Z5SdmIWfy3IeuSVg37j5jY6PqtVAkBbBhXknRHkDGimrAPPL13RSQfUeiqPtHBeo53pnpQT6L5h4vPk/bnVfKkr+RYqRV1/bV9JP/D/1LbFMTCuKG3ZAkEAoivss/vP6HoiTzp/tT4kXxoOnoHhuSho2kCCe08MSiLVPFgbGY5vNp9QmpRMFeqfi7+8VrdrJW7OQL1S8Ed27QJBAOaojgagBwiNlt+EZbghCqQoB4dz6PBV5B7uqH/jQuTz0dMY3x0EvmLRX35Oi2igxPc+995zbVK3QCuipDK79SI=
         -----------
         MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4QvTswfJkJJ1GeA4NSI88SYXLZn5ZOzZq2gPG8Q5cIPzluzvLJP7KoiSJMuPLH+wx37lNYMAJ6q+I4gHrnPSX4B/SrZRNodhYO8zq8LzxUxXCTeV9HcqsJ51q+t1uMOAiJcXvej3F8IWcw/+a6apntWmP15yxOBy9XPS/bXwnTQIDAQAB


         */


        final String message = "hello!";

//        final String privateKeyBase64Encoded = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALhC9OzB8mQknUZ4Dg1IjzxJhctmflk7NmraA8bxDlwg/OW7O8sk/sqiJIky48sf7DHfuU1gwAnqr4jiAeuc9JfgH9KtlE2h2Fg7zOrwvPFTFcJN5X0dyqwnnWr63W4w4CIlxe96PcXwhZzD/5rpqme1aY/XnLE4HL1c9L9tfCdNAgMBAAECgYAabE+fG/jcb3u2PUUWlo8jwA9Vqa74sMsO65CbGLpMb9aUQxNCAF4hRLskiY2s9TjztfdmpzcitOrTIEXSOcHBP0O61tSRhx8m3iUEUD700TlktCga9Jp/VHfj18PXV6xoe4jUCUyqeqnEASKQdO32OtP3w229vnL7jZADFWDpQQJBAOpoNCy7xcmuv2t9naG8WdtFl3VcujJtbRq1F/vcSUwWRjhyQHCFF+IMXAd3dLtruRwavrDlS4NcqZ6a6ZdfvBkCQQDJPDhIv3XR7uwv0NeZHVP2eQB5EFS9/2Eu0KvqlUx1jD0C62vcbf6KklmN/Z5SdmIWfy3IeuSVg37j5jY6PqtVAkBbBhXknRHkDGimrAPPL13RSQfUeiqPtHBeo53pnpQT6L5h4vPk/bnVfKkr+RYqRV1/bV9JP/D/1LbFMTCuKG3ZAkEAoivss/vP6HoiTzp/tT4kXxoOnoHhuSho2kCCe08MSiLVPFgbGY5vNp9QmpRMFeqfi7+8VrdrJW7OQL1S8Ed27QJBAOaojgagBwiNlt+EZbghCqQoB4dz6PBV5B7uqH/jQuTz0dMY3x0EvmLRX35Oi2igxPc+995zbVK3QCuipDK79SI=";
//


        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicBase64Bytes));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKeyFromDataStore = kf.generatePublic(spec);


        PKCS8EncodedKeySpec specPriv = new PKCS8EncodedKeySpec(Base64.decode(privateBase64Bytes));
        PrivateKey privateKeyFromFirmware = kf.generatePrivate(specPriv);

//        final ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(Base64.decode(publicKeyBase64Encoded.getBytes()));
//        final ObjectInputStream objectInputStream2 = new ObjectInputStream(byteArrayInputStream);
//
//        final BigInteger modulus2 = (BigInteger) objectInputStream2.readObject();
//        final BigInteger exponent2 = (BigInteger) objectInputStream2.readObject();
//
//        final RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus2, exponent2);
//
//        final KeyFactory fact = KeyFactory.getInstance("RSA");
//        final PrivateKey privateKey = fact.generatePrivate(rsaPrivateKeySpec);
//        final PublicKey publicKey = fact.generatePublic(rsaPublicKeySpec);

        final Signature sig = Signature.getInstance("SHA512WithRSA");
        sig.initSign(privateKeyFromFirmware);
        sig.update(message.getBytes());

        final byte[] realSig = sig.sign();

        final Payload payload = new Payload(message, realSig);
        payload.send();

        final Signature sig2 = Signature.getInstance("SHA512WithRSA");
        sig2.initVerify(publicKeyFromDataStore);
        sig2.update(payload.content.getBytes());
        if(!sig2.verify(payload.signature)) {
            System.out.println("Did not recognize the signature bailing");
            return;
        }

        System.out.println("It's all good!");

    }
}
