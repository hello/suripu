/**
 * Created by benjo on 2/17/15.
 */

import com.hello.suripu.algorithm.bayes.GaussianDistribution;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hello.suripu.algorithm.bayes.GaussianInference;

import java.util.logging.Logger;

public class BayesianInferenceTest {


    @After
    public void tearDown() throws Exception
    {

    }

    @Before
    public void setUp() throws  Exception {

    }

    @Test
    public void TestBayesRandomMeanUpdate() {
        final GaussianDistribution prior = new GaussianDistribution(1.0,1.0);

        final GaussianDistribution posterior = GaussianInference.GetInferredDistribution(prior,2.0,1.0,1e-6);

        //0.2 * 2 + 0.8 * 1 = 0.8
        //
        final GaussianDistribution posterior2 = GaussianInference.GetInferredDistribution(prior,2.0,2.0,1e-6);

        TestCase.assertEquals(posterior.mean,1.5,1e-6);
        TestCase.assertEquals(posterior.sigma, 1.0 / Math.sqrt(2), 1e-6);

        TestCase.assertEquals(posterior2.mean,1.2,1e-6);
        TestCase.assertEquals(posterior2.sigma,Math.sqrt(0.8),1e-6);

    }



}
