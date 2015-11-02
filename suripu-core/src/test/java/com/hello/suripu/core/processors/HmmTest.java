package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.SensorDataTimeSpanInfo;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.HmmDeserialization;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by benjo on 3/3/15.
 */
public class HmmTest {

    private static final int NUM_MINUTES_IN_WINDOW = 15;
    private static long t0 = 1425420828000L;
    private static long tf = t0 + 3600 * 8 * 1000;
    private static long tc1 = t0 + 3600 * 6 * 1000;
    private static long tc2 = t0 + 3600 * 7 * 1000;

    private static final String protoData = "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g2gIfZ+EPynbfR8tqJrDPykwStU+LmWcPynkslH8qwO1PyleDNhmcStNPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnn0SzD+7u0Pyk1coX6u27nPympwuCwrGgQPyl7+tolqJ+6PymKWfg2kN2oPynyhUj4M3ehPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAAClGqBpiD1+jPynODLAFPaIoPym6WX8YX3LtPymwWpeKpV+UPym4aSdRMFWWPynkTnUrrMcPPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnbFCTL+qaTPyk+1nNQwFWAPylzLy3dXgN8Pym+akvFTk3uPylA/rHzCQeKPyklUUDIvv95PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkJ3n0oAbG7PynMKc4C1FN7PylnL8vbare0PymAOm8WA3CGPynxjrR4A/HkPym4QFlvzfmsPylDBmvbUw+1PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnk3SMJwryQPykARhXTwiJ8Pym3q4cl2VO/PilCxXHYm3GRPylCPC1c7MuTPym5xV9wBADkPymLqCGbvy/UPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkHQJHdRDiGPymRDpbKVcTtPymyjJiUChOPPykms8k3V/+aPyndduV/96hzPykc3sy4E8yJPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACm6JBRf8UWxPykLv4ZzVU/APynJyzt3bMPpPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmxvr3d+1iQPykAAAAAAAAAACl7sQ+yMaGYPykAAAAAAAAAACnpUinL2OyFPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkeCcu4QOfqPym7/Ke708m7PylTVIU1Q+bkPSmmqseGnsjQPykfstGKRdCOPinyC4Jyg7m6PykaKNFsOM5VPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkZPlmzmDnkPykKSSeCY9k7Pil0sYJa3COoPykdV+rUssihPykOD13SkUYKPyldARZt+7G1PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAAClbOOVZht6vPykAAAAAAAAAACmWEIKNJvqQPylPxxTr1CToPzE7H7DoK1GKPTEomVDd9I8yOjG8lv/////vPzFHRaDI7jLCOzF1FMZ0L/73OjGzst32L/UwODFqyWbI18tbODHU2MJjWALUMzEPzy0bs613OTGdTH3aYhskOzF4iTtlXg6wODkAAAAAAABOQEEAAAAAAHDHQEkAAAAAAAAwQFEAAAAAAAAQQFhNYgdkZWZhdWx0";
    private static final String protoData2 = "CqkTCn4xLDEwMjUsMTAzOCwxMDQzLDEwNDksMTA1MCwxMDUyLDEwNTMsMTMxMCwxMDYwLDEwNjEsMTA2MiwxMDYzLDEwNjcsMTA3MCwxMDcxLDEwNzIsMTA4NiwxNjA5LDE2MjksMTAwMSwxMDAyLDEwMDUsMTY0OCwxMDEyLDEwMTMSDy4vZGVmYXVsdDQuanNvbhphIAEoATAAWhIJMgGWS7+QGUARvFzcCyNKA0BiCQnBXLv3DVHLP2oSCRZKz97oddU/CUtZmJALReU/chIJNU6akHQpC0ARBZarveHG+T96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoATAAWhIJaaa5sgqm2D8RfqIj+fkH4z9iCQkvzUVegT7MP2oSCfAJMAgnzN4/CfP753vsmeA/chIJYjAqvriWDEARs1IByXM7/T96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoATAAWhIJxRup2yn9GEARz9Kx3KoRA0BiCQkI8fvKHSqUP2oSCSzmYmdfo+8/CUONRycmKIc/chIJNa9JGpF+2T8RoO1iNljX4j96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoATAAWhIJz1ALf6xNyT8R8eVdNXlm0D9iCQlUCFHz6wCKP2oSCZMBPeWb1O8/CbMmfmENsnU/chIJWsYWSMgcyT8RnYujGmuA1z96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoADAAWhIJbKV1qxWQGEAR0uyJ58FtBUBiCQkZK6mPHtkQQGoSCaLIkDvLisc/CcjOGzFNHeo/chIJ0RnQVUH/DUAR1IiJV3Ff/D96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoADAAWhIJKqxpDFAt2D8RcFUxHCmS4T9iCQljnywhB8UVQGoSCQQU+3ASC9M/CUd1gsd2euY/chIJftaHy+nqBkARJEtMlcdn/T96EgkAAAAAAADwPwkAAAAAAADwPxphIAAoADAAWhIJT4WsYTbVzz8RpCOit2Wwzz9iCQmxn+rNXZL1P2oSCfae9B9nGu4/CTbRtQCOWa4/chIJ77nXatA52j8RnYQdKAY05D96EgkAAAAAAADwPwkAAAAAAADwPxphIAAoADAAWhIJspY7o4VIyz8RYhB2it6Dxz9iCQlKGWR8s/n6P2oSCZGcBVsXb+c/CUXG9EnRIdE/chIJo0gEjvonBUARoO79DGXT9j96EgkAAAAAAADwPwkAAAAAAADwPxphIAAoADAAWhIJxo2m6yzKEEARN8n+quJ7AUBiCQlqtv39a3z9P2oSCdJQcZqb1+w/Cd95dSwjQ7k/chIJIoIzdwAj3j8RjeuWy/wP5T96EgkAAAAAAADwPwmN7bWg98awPhphIAEoADAAWhIJ5GL4J/LbEkARdoXe4EUPA0BiCQmHLyllibQQQGoSCb4XOPWlkdA/Cb3zYwUtt+c/chIJfwaZv7gjBUARq1tE4X4z9j96EgkAAAAAAADwPwkAAAAAAADwPxphIAEoADAAWhIJhe94fvTw1T8ROPcENtR/2T9iCQkVbzIRb98PQGoSCQpIkSkkzts/CeRbN+vtGOI/chIJsfYfGVdzAUARsImkaWRL9D96EgkAAAAAAADwPwkAAAAAAADwPyALKayiyTD9yOc/KZYw9aLVaY0/KZjhDCwSg8E/KTUU+TDpgZo/Kegzhl0hy7M/KddiwG54I2M/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKcbpspaQIqs/KRIfVjUCkOk/KU5BEPi1yAU/KRdz0QUGZrM/KWPjaXwmDpw/KTIdlZy6BKc/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKZE1K8948Kk/KWP3E2kyVDs/KeiK7gtDWe0/KSyvuhboMo4/KZAFGekbb5E/KYGwK5y3hMk+KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKXzwOUBneY4/KeXmllIQWIk/KURDe6Sp6nw/KT+Hg7B8Ue4/KS2qWnow0oY/KYR6av+tD30/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQxyUCuHmME/Kf/kEE/CAX0/KU2F5+xfEaY/KcDW7Uavj4w/KQ+C2U8KO+U/KdL2iWZWaLA/KT50xr+GI7I/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKTOyWC7uxXk/KenF8Fo4vKs/Kb6/BzeaYk0/KVubHWLnj44/KXR2vHpgD58/KcUeqXi1l9w/KUkoZzOFldw/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKYfb/hAE84U/KS2vgROFEu0/KZbLk/FTXp0/KTdSruEb0ZI/KVHsVMNNq4M/KZhyXtLEsJg/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKctttZ/jyaY/Kaobt4n4VcA/KW3ilqPjfeo/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKUWR/xm91pU/KQAAAAAAAAAAKTasuo4l9qI/KQAAAAAAAAAAKQhW6zJ7X30/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKXcFKVw/Euk/KZozd4qkU8M/KVFb1qTrRYI+KapCXNxlzsw/KfgnTbO0rfE+KVs6/eRXvrU/KRF2WIMt6pM/KQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKQAAAAAAAAAAKczD9H/Q3L8/KZhpscgPeeE/KQhazUYFOw8/KcyVFcYhmZQ/KTQ/KbbyhYQ/KdI6hJ0Aq2U/KRBx00OJJrE/KQAAAAAAAAAAKQAAAAAAAAAAKVi8YAG6hr0/KdIyUBb1nLM/KQAAAAAAAAAAKbXpt90bF8M/KXeleiNm5OE/MUhJubNvBo8/MadielhyC248Mb8GMUHmg+8/MdIWPtNkGmQ9MRT2dZxc41s8Mb6enJRWqcE6MWfapKJ3N4c6MTpcZQ8dNtg1MecrKVPoyDQ7MXwbholjBYA8MW8KAFRzLjQ7OQAAAAAAQFBAQQAAAAAATM1ASQAAAAAAADBAUQAAAAAAABBAWE1iB2RlZmF1bHRwAAqvEgoEMTA4NRIRLi9hcG5lYW1vZGVsLmpzb24aYSABKAEwAFoSCc0H+QGMLBFAESS7qI0DJvY/YgkJFNbUh0v+vj9qEgnNshMT55XRPwmWJnZ2DDXnP3ISCQiH5wO18Q5AESGMCEEge/g/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAEwAFoSCR0+v4T5R94/Eb7uE5QAEd8/YgkJPjqRR7Bw1D9qEgnh6MWGwxnSPwmEC508HvPmP3ISCcVa8ZurGARAEapnONob+fI/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAEwAFoSCeRLTflf3BBAEWww15KqCfY/YgkJexSuR+F6hD9qEgmq8JzyfP7vPwm5s/gw1jAoP3ISCUIxoV64vNY/EbRT6kBl5uM/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAEwAFoSCeboY5PLFtE/Eedb4w13GdE/YgkJexSuR+F6hD9qEgm70KIMd//vPwlceeulax4RP3ISCVG9MFDjr8c/EfRVNJuXa9c/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAAwAFoSCdX/VYYZ1hFAEQCY/mm/CPI/YgkJpa57zZhkEUBqEglJJN/OdeCrPwm9DRKj+EHuP3ISCTk50BmGrRBAEeNcUMnCifU/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAAwAFoSCQqWtZPe2NM/EdvRK6+tFtI/YgkJzUK/U7UbIkBqEglXt3/LL4faPwlbJEAaaLziP3ISCZBTD9dniwlAEXlp2KUgb/M/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSAAKAAwAFoSCQgv5dHyFtQ/EbsS62DeD9I/YgkJhuX9Hk3z5D9qEgn7f/QTnBXvPwm8/2+BfUydP3ISCYAFXgks/u0/EQkyMJyhlew/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSAAKAAwAFoSCV+rNw8NU9Y/ESwODLls69Y/YgkJAp3XG9NdFUBqEglWk6fdmXPaPwlZNiwRM8biP3ISCX/Q22/Zew5AEXtPbjo7Bu0/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSAAKAAwAFoSCbyltOHTBA9AES6VwwGyZdk/YgkJzO7MjwSF/T9qEgnCexjpj/LvPwm4UgjPLeBaP3ISCafNDUhaxeQ/EVlwG7aSX+M/ehIJAAAAAAAA8D8Jje21oPfGsD4aYSABKAAwAFoSCbnA9uOJLgRAEYFDi16wzfA/YgkJdzdtzderBkBqEgmoK+YraVMnPwmcQW3Jiv7vP3ISCYVmJguteBNAEeWm0GLNUvA/ehIJAAAAAAAA8D8JAAAAAAAA8D8aYSABKAAwAFoSCan4xgLpTPA/EbvJ+M2FQ9c/YgkJDCTMeMm8HEBqEgknU0lLotjJPwk0qy1t14npP3ISCSkp/CuztBBAEfHoSrpHc+4/ehIJAAAAAAAA8D8JAAAAAAAA8D8gCymcEHHTFSnhPynglWzqBp/SPikMlTm2o+nWPymHIs4YlT6ePylx3DvJ0oCzPymiR0H/CGSYPSkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnGJScrmIBQPinsvhNmDHDkPynmb68qhBHPPilrjijDxDzGPymnv9Oqdrb5Pim8wk2+HALIPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmb4ptpdGC1PykzMTJgLt0SPikkypxu7S/sPymvu1N23tmgPynYlpMzHGZmPymn8an8IrEAPSkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAAClAZRXV5y6UPynzO6z4DFt0PymwybG/rYZfPykanHCaDYjuPylERHTHYxmQPyk79eTZlzZtPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACn2d4frlfSnPylULGPfO3SvPyl36u2guTa0PylEFg/yjSuePynf/FGMrG/gPymKBFr3TAazPylBowCoKqSvPynmX5enGRrBPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmJn9wIlXrjPimcyRmoGruWPim19E+Cnuc9PSnmivgUyr4hPilhiRQCBVwQPiko9VZbc3DkPykLezo3Q5aAPykI5X7LPprWPykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACmzsyuVeI7dPilgFjP0+5rBPyn3RCsat9rqPyl4ojnsV1VmPilTPa5qYM+XPylmeowPhINPPikAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACm7i8BAYg1TPym6eXY6ueK3Pylh4HNYTtTrPymoh7QpoVF4PykVsgwSaReSPyleYkgOUR2JPylruzb4mVCmPikAAAAAAAAAACn3ZDF1i1eMPikAAAAAAAAAACnf8kx+gs65PykAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACnfq9CB3DLlPymNmDdfRU3OPymohTRxLQH4PSkMJYyU4pDjPyktaNTXUuqqPimSY5XuOdjVPymroj7UJqgwPikAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACm4U7Ez7C+oPykEx8pcw/D5PSmxyd9ldannPyn7UwBCHgbLPykuiMHfZZ2UPinEdgg8wtUgPikAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkAAAAAAAAAACkr3El7FeOMPynJ66bbYBeiPzGPtCQFnBHQPTG5dbgJ8yCMOTEx9/f////vPzHYyH3xkaaNOzH1Zog4BTlAOjE3G7gqJgnzNjHp1SsFzypMNjFqF8dLYp+2MzHSJrFiYO7JNzEagH/z+OhxOjH8TtHsy+DaODkAAAAAAABOQEEAAAAAAHDHQEkAAAAAAAAwQFEAAAAAAAAQQFhNYgVhcG5lYXAA";
    final double [][] sensordata = {
            {7.0,0.3,0.3,0.3,5.6,8.8,8.8,8.8,8.8,8.8,8.8,8.8,8.8,8.8,8.8,8.8,7.7,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.3,0.4,1.4,3.4,4.5,5.7,6.6,7.0,5.9,5.9,6.1,6.1,6.0,6.3,6.9,6.8,7.0,7.1,7.5,8.7,9.2},
    {1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,2.0,1.0,1.0,0.0,2.0,3.0,1.0,2.0,2.0,4.0,2.0,1.0,2.0,1.0,0.0,3.0,3.0,3.0,2.0,2.0,3.0,3.0,1.0,1.0,1.0,1.0,1.0,0.0,1.0,1.0,2.0,2.0,3.0,2.0,1.0,2.0,1.0,4.0,0.0,0.0,2.0,0.0,3.0,8.0,5.0,5.0,3.0,0.0,0.0,0.0},
    {1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,1.0,1.0,1.0,1.0,0.0,0.0,0.0,1.0},
    {3.9,0.0,0.0,1.0,3.2,0.0,0.0,2.0,2.8,2.6,1.0,2.6,4.8,1.0,2.0,2.8,2.8,0.0,1.0,1.0,1.0,0.0,0.0,1.6,0.0,0.0,1.6,0.0,1.6,0.0,2.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0,0.0,0.0,1.0,0.0,1.6,1.0,2.0,2.3,2.0,0.0,0.0,2.0,0.0,2.0,0.0,2.0,1.0,1.0,4.1,5.8,6.3,3.9,0.0,3.8,4.3,4.2},
    {1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0}};

    final int [] pathref={0,0,3,3,3,0,2,2,0,0,0,0,0,0,0,0,0,4,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,8,8,8,8,8,8,8,8,8,8,8,9,9,9,9,8,0,0};

    static private class MyHmmDAO implements SleepHmmDAO {
        final private String protbufData;

        public MyHmmDAO(final String protobuf) {
            this.protbufData = protobuf;
        }

        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            Optional<SleepHmmWithInterpretation> ret = Optional.absent();

            try {
                final byte[] decodedBytes = Base64.decodeBase64(this.protbufData);

                final SleepHmmProtos.SleepHmmModelSet proto = SleepHmmProtos.SleepHmmModelSet.parseFrom(decodedBytes);

                ret = SleepHmmWithInterpretation.createModelFromProtobuf(proto);


            } catch (IOException e) {
                e.printStackTrace();
            }

            return ret;
        }

    };

    @Test
    public void TestHmm() {
        final int offset = -3600*1000;
        final SleepHmmDAO myDAO = new MyHmmDAO(protoData);

        final Optional<SleepHmmWithInterpretation>  hmm = myDAO.getLatestModelForDate(0,0);

        assertTrue(hmm.isPresent());

        final AllSensorSampleList sensorSampleList = new AllSensorSampleList();
        final List<TrackerMotion> motionList = new ArrayList<TrackerMotion>();

        for (int i = 0; i < 24; i++) {
            /*
            @JsonProperty("id") final long id,
            @JsonProperty("account_id") final long accountId,
            @JsonProperty("tracker_id") final Long trackerId,
            @JsonProperty("timestamp") final long timestamp,
            @JsonProperty("value") final int value,
            @JsonProperty("timezone_offset") final int timeZoneOffset,
            final Long motionRange,
            final Long kickOffCounts,
            final Long onDurationInSeconds
            */
            final long t = t0 + i*3600/3*1000;
            final TrackerMotion m = new TrackerMotion(0,0,0L,t,500,offset,0L,1L,1L);

            motionList.add(m);

        }

        List<Sample> light = new ArrayList<Sample>();

        for (int i = 1; i < 7*3600; i++) {
            final Long t = t0 + i*60*1000;
            final Sample s = new Sample(t,0.1f,offset);
            light.add(s);
        }

        sensorSampleList.add(Sensor.LIGHT,light);

        final OneDaysSensorData sensorData = new OneDaysSensorData(sensorSampleList,ImmutableList.copyOf(motionList),ImmutableList.copyOf(Collections.EMPTY_LIST),ImmutableList.copyOf(Collections.EMPTY_LIST));
        Optional<SleepHmmWithInterpretation.SleepHmmResult> res = hmm.get().getSleepEventsUsingHMM(sensorData,tc1);
        Optional<SleepHmmWithInterpretation.SleepHmmResult> res2 = hmm.get().getSleepEventsUsingHMM(sensorData,tc2);

        TestCase.assertTrue(res.isPresent());
        TestCase.assertTrue(res2.isPresent());

        SleepHmmWithInterpretation.SleepHmmResult r = res.get();

        TestCase.assertTrue(r.sleepEvents.size() == 4);
        boolean foundOne = false;
        for (Event e : r.sleepEvents) {

            if (e.getType() == Event.Type.SLEEP) {
                foundOne = true;
            }
        }

        TestCase.assertTrue(foundOne);

        final int expectedLength1 =  6*60 / NUM_MINUTES_IN_WINDOW;
        final int expectedLength2 =  7*60 / NUM_MINUTES_IN_WINDOW;

        TestCase.assertTrue(res.get().path.size() == expectedLength1);
        TestCase.assertTrue(res2.get().path.size() == expectedLength2);



    }

    @Test
    public void TestHmmInterpretation() {
        final SleepHmmDAO myDAO = new MyHmmDAO(protoData2);

        final Optional<SleepHmmWithInterpretation>  hmm = myDAO.getLatestModelForDate(0,0);

        ImmutableList<SleepHmmWithInterpretation.SegmentPair> sleeps = hmm.get().testDecodeWithData(sensordata);

        TestCase.assertTrue(sleeps.size() == 1);


    }

    @Test
    public void TestHmmEventProcessing() {
        final List<SleepHmmWithInterpretation.SegmentPair> sleeps = new ArrayList<>();
        final List<SleepHmmWithInterpretation.SegmentPair> beds = new ArrayList<>();

        sleeps.add (new SleepHmmWithInterpretation.SegmentPair(10,20));
        sleeps.add (new SleepHmmWithInterpretation.SegmentPair(36,42));

        beds .add(new SleepHmmWithInterpretation.SegmentPair(5, 8));
        beds .add(new SleepHmmWithInterpretation.SegmentPair(9,23));
        beds .add(new SleepHmmWithInterpretation.SegmentPair(35,45));
        beds .add(new SleepHmmWithInterpretation.SegmentPair(48,50));

        final SensorDataTimeSpanInfo timeSpanInfo = new SensorDataTimeSpanInfo(0,0);

        Optional<SleepHmmWithInterpretation.SleepHmmResult> resultOptional = SleepHmmWithInterpretation.processEventsIntoResult(ImmutableList.copyOf(sleeps), ImmutableList.copyOf(beds), ImmutableList.copyOf(Collections.EMPTY_LIST), timeSpanInfo,15);

        TestCase.assertEquals(resultOptional.isPresent(), true);


        SleepHmmWithInterpretation.SleepHmmResult result = resultOptional.get();
        int numInBeds = 0;
        int numSleeps = 0;
        int numWakes = 0;
        int numOutOfBeds = 0;
        for (final Event e : result.sleepEvents) {
            if (e.getType() == Event.Type.IN_BED) {
                numInBeds++;
            }

            if (e.getType() == Event.Type.OUT_OF_BED) {
                numOutOfBeds++;
            }

            if (e.getType() == Event.Type.SLEEP) {
                numSleeps++;
            }

            if (e.getType() == Event.Type.WAKE_UP) {
                numWakes++;
            }
        }

        TestCase.assertEquals(numInBeds, 2);
        TestCase.assertEquals(numOutOfBeds, 2);
        TestCase.assertEquals(numWakes, 2);
        TestCase.assertEquals(numSleeps, 2);



    }
}