package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by benjo on 2/21/15.
 */


public class PdfComposite implements HmmPdfInterface {

    ArrayList<HmmPdfInterface> _pdfs;

    public PdfComposite() {
        _pdfs = new ArrayList<HmmPdfInterface>();
    }


    public void addPdf(HmmPdfInterface pdf) {
        _pdfs.add(pdf);
    }

    public void clear() {
        _pdfs.clear();
    }

    public double [] getLogLikelihood(final double [][] meas) {
        double [] liks = new double[meas[0].length];
        Arrays.fill(liks, 0.0);

        //multiply out joint
        for (HmmPdfInterface pdf : _pdfs) {
            double[] liks2 = pdf.getLogLikelihood(meas);

            for (int j = 0; j < liks.length; j++) {
                liks[j] += liks2[j];
            }

        }

        return liks;
    }





}
