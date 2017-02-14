package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import java.io.*;

/**
 * Created by zhang on 11/11/16.
 * This test class is to test the CropFile class.  The sample file used for testing is f3.fits.  It is used to create a output file,
 * out_f3.fits.  Both f3.fits and out_f3.fits are stored in the firefly_test_data/edu/caltech/ipac/visualize/plot/.  The end to end test is
 * load the f3.fits and creates a cropped file in the fly.  The newly created outFits file is compared with out_f3.fits.  If the
 * assertion is failed, it means the bugs are introduced after this class is written.
 *
 * It is difficult to do a unit test for this kind cases.  We the end to end test is used instead.
 */
public class CropFileTest extends CropTestBase {

    //multi-extension file
    private static String fileName = "f3.fits";
    private  Fits  inFits=null;
    private  String resultUsingMinMax ="cropFileUsingMinMax_"+fileName;
    private  String resultUsingMinMaxExt ="cropFileUsingMinMaxExtension_"+fileName;
    private  String resultUsingRaDec ="cropFileUsingWorldPtRadius_"+fileName;
    private  Fits expectedFitsUsingMinMax =null;
    private  Fits expectedFitsUsingMinMaxExt =null;
    private  Fits expectedFitsUsingWorldPtRadius =null;


    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        inFits = FileLoader.loadFits(CropFileTest.class, fileName);

        //This file stored the results run when this class is written.  All future results are compared with it.
        expectedFitsUsingMinMax = FileLoader.loadFits(CropFileTest.class, resultUsingMinMax);
        expectedFitsUsingMinMaxExt = FileLoader.loadFits(CropFileTest.class, resultUsingMinMaxExt);
        expectedFitsUsingWorldPtRadius = FileLoader.loadFits(CropFileTest.class, resultUsingRaDec);

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        expectedFitsUsingMinMax =null;
        expectedFitsUsingMinMaxExt=null;
        expectedFitsUsingWorldPtRadius =null;

    }

    @Test
    /**
     * We use the result wen this class written as a reference.  The output is saved to a file, out_f3.fits.  The newly calculated
     * data is compared with the data in out_f3.fits.
     */
    public void endToEndTestUsingMinMax() throws FitsException, IOException {

        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;

        Fits  outFits = CropFile.do_crop(inFits, min_x, min_y, max_x, max_y);

        validHeader(expectedFitsUsingMinMax,outFits);

        validateData(expectedFitsUsingMinMax,outFits);

    }

    @Test
    public void endToEndTestUsingMinMaxExtension() throws FitsException, IOException {

        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;

        Fits  outFits = CropFile.do_crop(inFits,0, min_x, min_y, max_x, max_y);

        validHeader(expectedFitsUsingMinMaxExt,outFits);

        validateData(expectedFitsUsingMinMaxExt,outFits);

    }
    @Test
    /**
     * This is to test the do_crop using WordPt, and radius
     * We use the result wen this class written as a reference.  The output is saved to a file, out_f3.fits.  The newly calculated
     * data is compared with the data in cropFileUsingWorldPtRadius_f3.fits.
     */
    public void endToEndTestUsingWorldPtRadius() throws FitsException, IOException, ProjectionException {

        double ra = 329.162375;
        double dec = 62.2954;
        WorldPt worldPt = new WorldPt(ra,dec);
        double radius = 3.18;


        Fits outFits =CropFile.do_crop(inFits, worldPt, radius);

        validHeader(expectedFitsUsingWorldPtRadius,outFits);

        validateData(expectedFitsUsingWorldPtRadius,outFits);

    }
    /**
     * This main program does not need to re-run.  It ran only once to create the json file.
     * Use main to generate the Histogram output and store in json format.  This is end to end test.  The json file only needs
     * to generated once.  It will be used for all future testing.  It ensures that the future changes will not affect the
     * results.  If the assertion fails, it means the changes introduce the bugs.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String dataPath = FileLoader.getDataPath(CropFileTest.class);
        Fits inFits = FileLoader.loadFits(CropFileTest.class, fileName);

        //create first FITS file
        String outFitsName1 =dataPath+ "cropFileUsingMinMax_"+fileName;
        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;
        Fits  outFits1 = CropFile.do_crop(inFits, min_x, min_y, max_x, max_y);
        FileOutputStream fout1 = new java.io.FileOutputStream(outFitsName1);
        BufferedDataOutputStream out1 = new BufferedDataOutputStream(fout1);
        outFits1.write(out1);
        fout1.close();
        out1.close();


        inFits = FileLoader.loadFits(CropFileTest.class, fileName);
        String outFitsName2 =dataPath+ "cropFileUsingMinMaxExtension_"+fileName;
        //create first FITS file
        Fits  outFits2 = CropFile.do_crop(inFits, 0, min_x, min_y, max_x, max_y);
        FileOutputStream fout2 = new java.io.FileOutputStream(outFitsName2);
        BufferedDataOutputStream out2 = new BufferedDataOutputStream(fout2);
        outFits2.write(out2);
        fout2.close();
        out2.close();




        //create the third FITS file
        //reload FITS file since the continues reading caused the file pointer issue
        inFits = FileLoader.loadFits(CropFileTest.class, fileName);
        String outFitsName3 =dataPath+ "cropFileUsingWorldPtRadius_"+fileName;
        double ra = 329.162375;
        double dec = 62.2954;
        double radius = 3.18;
        FileOutputStream fout3 = new java.io.FileOutputStream(outFitsName3);
        Fits outFits3 =CropFile.do_crop(inFits, new WorldPt(ra, dec), radius);
        BufferedDataOutputStream out3 = new BufferedDataOutputStream(fout3);
        outFits3.write(out3);
        fout3.close();
        out3.close();



    }

}
