package com.hello.suripu.algorithm.hmm;

/**
 * Created by benjo on 3/24/16.
 */
public class PdfCompositeBuilder {

    final private PdfComposite pdf;

    public static PdfCompositeBuilder newBuilder() {
        return new PdfCompositeBuilder();
    }

    private PdfCompositeBuilder() {
        pdf = new PdfComposite();
    }

    private PdfCompositeBuilder(final PdfComposite pdf) {
        this.pdf = pdf;
    }

    public PdfCompositeBuilder withPdf(final HmmPdfInterface pdf) {
        this.pdf.addPdf(pdf);

        return new PdfCompositeBuilder(this.pdf);
    }

    public HmmPdfInterface build() {
        return this.pdf;
    }


}
