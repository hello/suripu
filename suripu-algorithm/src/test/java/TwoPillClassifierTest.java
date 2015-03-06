import com.google.common.base.Optional;
import com.hello.suripu.algorithm.signals.TwoPillsClassifier;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomVectorGenerator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by benjo on 3/3/15.
 */
public class TwoPillClassifierTest {

    //some random correlated numbers
    final double [][] x = {
            {1.476665,-0.102361,0.146054,0.129179,-0.695252,0.497396,0.684225,0.117496,-0.148519,-0.287647,-0.486230,-0.846915,-0.298684,-0.219516,-1.693183,0.504974,0.098066,0.705929,-0.590070,0.691597,-1.491503,-0.069486,1.388254,1.053367,-1.396142,1.365787,0.465111,0.615408,-0.651038,0.912687,-2.076724,-1.870253,0.270329,-2.068485,0.188871,0.307140,0.435932,-0.229680,-0.266443,-0.123488,-0.553441,-0.945292,1.032532,0.229519,0.081956,-2.290913,1.383566,-0.750682,0.439298,-0.999733},
            {0.239612,0.160769,-1.409712,1.180099,0.581584,-1.607147,-0.185949,-1.443574,1.618634,1.222391,-1.203318,0.144861,0.535891,1.191978,-0.669106,0.186143,-0.228688,0.573367,1.582896,-1.365560,0.811704,-1.408338,-0.267712,-2.305639,1.058342,1.074984,-1.009914,3.424526,-0.307045,-0.015417,-0.275743,0.047857,-2.508895,0.146609,1.339699,-0.659990,1.495633,1.213951,-0.213534,-1.329909,1.531782,-1.957305,0.998537,1.313747,-1.353890,-0.650287,-0.091963,0.456000,-0.254150,0.408671},
            {0.611736,0.159085,-2.434588,-0.457071,-0.355790,-3.421950,-1.958930,-0.989955,0.538373,-1.426998,-1.212711,-0.331670,1.201765,-0.315622,0.677598,0.337830,-1.187564,-1.061839,0.156056,-3.301036,1.518416,-2.649881,2.106745,-2.994329,0.555919,1.288495,-0.521266,3.763873,-2.938705,-2.044020,-2.054167,2.799503,-2.626165,1.259404,-0.301716,-1.266770,4.038595,-1.508508,-1.424566,0.248783,1.879308,-2.793689,0.896550,0.679637,-2.511613,-0.075007,0.064203,-0.239881,-0.251054,0.262463 }};



    @Test
    public void TestDecorrelation() {


        final Optional<RealMatrix> y = TwoPillsClassifier.getUncorrelatedDataPoints(x);


        assertTrue(y.isPresent());

        double [][] result = y.get().getData();

        double sum1 = 0.0;
        double sum2 = 0.0;
        for (int i = 0; i < 50; i++) {
            sum1 += result[1][i] *  result[1][i];
            sum2 += result[0][i] * result[2][i];
        }

        assertEquals(sum1,50.0,1e-3);
        assertEquals(sum2,0.0,1e-3);

    }

    @Test
    public void JustRunThroughTheCode() {

        final int classes [] = TwoPillsClassifier.classifyPillOwnershipByMovingSimilarity(x);

    }

}
