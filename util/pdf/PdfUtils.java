package edu.caltech.ipac.util.pdf;

import com.lowagie.text.DocumentException;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jul 2, 2012
 * Time: 3:15:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class PdfUtils {
    /**
     *
     * Convert HTML with CSS to PDF
     *
     * @param html HTML file to convert
     * @return PDF file converted
     * @throws DocumentException
     * @throws IOException
     */
    public static File convertPDF(File html) throws DocumentException, IOException {
        String pdfOutput = html.getAbsolutePath().replaceAll("\\.htm[l]", "\\.pdf");
        File pdf = new File(pdfOutput);
        return convertPDF(html, pdf);
    }

    /**
     *
     * Convert HTML with CSS to PDF
     *
     * @param html HTML file to convert
     * @param pdf PDF file converted
     * @return PDF file converted
     * @throws DocumentException
     * @throws IOException
     */
    public static File convertPDF(File html, File pdf) throws DocumentException, IOException {
        OutputStream os = null;
        try {
            String url = html.toURI().toURL().toString();
            os = new FileOutputStream(pdf);
            ITextRenderer renderer = new ITextRenderer();

            renderer.setDocument(url);
            renderer.layout();
            renderer.createPDF(os);

        } finally {
            if (os!=null) os.close();
        }
        return pdf;
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */