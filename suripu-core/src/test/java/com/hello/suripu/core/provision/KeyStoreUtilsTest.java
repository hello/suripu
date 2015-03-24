package com.hello.suripu.core.provision;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.KeyStoreUtils;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeyStoreUtilsTest {


    @Test
    public void testPillBlobProvision() throws Exception {

        final String PILL_ID = "21406BA108DD016F";
        final String BLE = "20e9abf930fddb8a";
        final String AES = "0C1FE4F88D00FF68473042EB7E10FF5F"; // not using this AES KEY
        final String FICR = "aa55aa55aa55aa55ffffffffffffffff0004000000010000ffffffffffffffffffffffff0202ffffffffffffffffffff" +
                "00ffffff020000000020000000200000ffffffffffffffff0fd6638900f3cb9ef8ffffffffffffff000000002e00ffff21406ba108dd" +
                "016fffffffffffffffff43424e3833330b2114ffffffffffffff017e6ead3f54be65002765408f09d15048f21e1af9de7566a6efec1c" +
                "e607c30affffffff20e9abf930fddb8af6ffffff005000784e00005403800c60246472003e420382ffffffffffffffffffffffffffff" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffff0056007d5000005c04880e68246472003e425382";
        final String HDWR_KEY = "017e6ead3f54be65002765408f09d150".toUpperCase();
        final String SHA_1 = "bbaf102db605e7d5f352a4673ebb1a26a99a2e4b".toUpperCase();

        final String hexString = "a5a5a5a50301bbea4085f6c1a97b8d5db0d60a994c957a1ce254e8ebe0d1ea3582dd8b82933f6794c6b40a557f2" +
                "b2ec3ad01792586e9c6374ef1742cabe20fb4b0f3c45419fb061a2e665737d9f622f4c7941158398e3e44904846bf9ff8fc3b0" +
                "3eefee055ad7c4fcd30418942862563fe849cbe460d68e26adc7c2b000bf4f59422a8d1d43f210113cbb2b95a241ef0f4f98c3" +
                "9b7bb7cc18cce5cce6e2b661ea5fa8492a5d81773d801149a97e307edf78435455b2e4e5b4403b9ef4a8fe2b1cbd936cdac95e" +
                "73a3f9f748ce3a6c1b699e03e7222b12000672d2c1ffac257e1607cee9fa79b1b6998546ccf206669c3fc40a13df55eba272f5" +
                "7adfa0551da0d9eb0db85639cdac871914fad75968c715c841a7a70882b46f01285c5b8505fc3473f475d6cbbcf5cd3e6bb6a87" +
                "af314c13390d42978de12d690549e7c1f19dd40d8c8e55ffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "ffffffffffffffff";

        final String fakeSerialNumber = "ABC";
        byte[] rawData = Hex.decodeHex(hexString.toCharArray());
        final Optional<PillBlobProvision> provisionOptional = KeyStoreUtils.decryptPill(rawData, fakeSerialNumber);
        assertThat(provisionOptional.isPresent(), is(true));

        final PillBlobProvision pillBlobProvision = provisionOptional.get();
        assertThat(pillBlobProvision.pillId, equalTo(PILL_ID));
        assertThat(pillBlobProvision.key, equalTo(HDWR_KEY));
        assertThat(pillBlobProvision.serialNumber, equalTo(fakeSerialNumber));
    }
}
