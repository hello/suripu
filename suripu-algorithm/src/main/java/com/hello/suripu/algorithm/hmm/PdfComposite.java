package com.hello.suripu.algorithm.hmm;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by benjo on 2/21/15.
 */


public class PdfComposite implements HmmPdfInterface {

    final ArrayList<HmmPdfInterface> pdfs;
    int numFreeParams;

    public PdfComposite() {
        numFreeParams = 0;
        pdfs = new ArrayList<HmmPdfInterface>();
    }


    public void addPdf(HmmPdfInterface pdf) {
        pdfs.add(pdf);
        numFreeParams += pdf.getNumFreeParams();
    }

    public double [] getLogLikelihood(final double [][] meas) {
        double [] liks = new double[meas[0].length];
        Arrays.fill(liks, 0.0);

        //multiply out joint
        for (HmmPdfInterface pdf : pdfs) {
            double[] liks2 = pdf.getLogLikelihood(meas);

            for (int j = 0; j < liks.length; j++) {
                liks[j] += liks2[j];
            }

        }

        return liks;
    }

    @Override
    public int getNumFreeParams() {
        return numFreeParams;
    }


}
