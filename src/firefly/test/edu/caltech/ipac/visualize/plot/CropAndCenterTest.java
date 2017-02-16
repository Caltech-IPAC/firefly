package edu.caltech.ipac.visualize.plot;


 /**
 * Created by zhang on 11/09/16.
 * This test class is to test the Crop class.  The sample file used for testing is f3.fits.  It is used to create a output file,
 * out_f3.fits.  Both f3.fits and out_f3.fits are stored in the firefly_test_data/edu/caltech/ipac/visualize/plot/.  The end to end test is
 * load the f3.fits and creates a cropped file in the fly.  The newly created outFits file is compared with out_f3.fits.  If the
 * assertion is failed, it means the bugs are introduced after this class is written.
 *
 * It is difficult to do a unit test for this kind cases.  We the end to end test is used instead.
 */


import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;


public class CropAndCenterTest extends CropTestBase  {


    private static String fileName = "f3.fits";
    private Fits inFits=null;
    private  String resultUsingMinMax ="cropUsingMinMax_f3.fits";
    private  String resultUsingRaDec ="cropUsingRaDec_f3.fits";
    private  Fits expectedFitsUsingMinMax =null;
    private  Fits expectedFitsUsingRaDec =null;
    private CropAndCenter crop;

    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        inFits = FileLoader.loadFits(CropAndCenterTest.class, fileName);

        //This file stored the results run when this class is written.  All future results are compared with it.
        expectedFitsUsingMinMax = FileLoader.loadFits(CropAndCenterTest.class, resultUsingMinMax);

        expectedFitsUsingRaDec = FileLoader.loadFits(CropAndCenterTest.class, resultUsingRaDec);
        crop = new CropAndCenter();

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        expectedFitsUsingMinMax =null;
        expectedFitsUsingRaDec =null;
    }

    @Test
    /**
     * This is to test the do_crop using min_x, min_y, max_x, max_y
     * We use the result wen this class written as a reference.  The output is saved to a file, out_f3.fits.  The newly calculated
     * data is compared with the data in cropUsingMinMax_f3.fits.
     */
    public void endToEndTestUsingMinMax() throws FitsException, ClassNotFoundException {


       int min_x = 47, min_y = 50, max_x = 349, max_y = 435;

        Fits  outFits = crop.do_crop(inFits, min_x, min_y, max_x, max_y);

        validHeader(expectedFitsUsingMinMax,outFits);

        validateData(expectedFitsUsingMinMax,outFits);

    }
    @Test
    /**
     * This is to test the do_crop using ra, dec, and radius
     * We use the result wen this class written as a reference.  The output is saved to a file, out_f3.fits.  The newly calculated
     * data is compared with the data in cropUsingRaDec_f3.fits.
     */
    public void endToEndTestUsingRaDec() throws FitsException, IOException {

        double ra = 329.162375;
        double dec = 62.2954;
        double radius = 3.18;
        FitsRead fits_read_0 = FitsRead.createFitsReadArray(inFits)[0];

        FitsRead newFitsRead =crop.do_crop(fits_read_0, ra, dec, radius); //crop.do_crop(fits_read_0, ra, dec, radius); //
        Fits outFits = newFitsRead.createNewFits();

        validHeader(expectedFitsUsingRaDec,outFits);

        validateData(expectedFitsUsingRaDec,outFits);

    }
    /**
     * This main program does not need to re-run.  It ran only once to create the testing files.
     * Use main to generate the CropAndCenter's output and store in the testing directory.  This is end to end test.
     * The testing file only needs to be generated once.  It will be used for all future testing.  It ensures
     * that the future changes will not affect the results.  If the assertion fails, it means the changes introduce
     * the bugs.
     * output file names:ls
     *  cropUsingMinMax_f3.fits
     *  cropUsingRaDec_f3.fits
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String dataPath = FileLoader.getDataPath(CropAndCenterTest.class);
        Fits    inFits = FileLoader.loadFits(CropAndCenterTest.class, fileName);
        String outFitsName1 =dataPath+ "cropUsingMinMax_"+fileName.substring(0, fileName.length()-5 )+".fits";

        //create first FITS file
        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;
        CropAndCenter crop = new CropAndCenter();
        Fits  outFits = crop.do_crop(inFits, min_x, min_y, max_x, max_y);
        FileOutputStream fout1 = new java.io.FileOutputStream(outFitsName1);
        BufferedDataOutputStream out1 = new BufferedDataOutputStream(fout1);
        outFits.write(out1);
        fout1.close();
        out1.close();

        //create the second FITS file
        FitsRead fits_read_0 = FitsRead.createFitsReadArray(inFits)[0];
        String outFitsName2 =dataPath+ "cropUsingRaDec_"+fileName.substring(0, fileName.length()-5 )+".fits";
        double ra = 329.162375;
        double dec = 62.2954;
        double radius = 3.18;
        FileOutputStream fout2 = new java.io.FileOutputStream(outFitsName2);
        FitsRead newFitsRead =crop.do_crop(fits_read_0, ra, dec, radius);
        Fits newFits = newFitsRead.createNewFits();
        BufferedDataOutputStream out2 = new BufferedDataOutputStream(fout2);
        newFits.write(out2);
        fout2.close();
        out2.close();


    }

}
