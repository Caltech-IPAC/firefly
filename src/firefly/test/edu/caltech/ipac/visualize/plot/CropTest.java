package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import java.io.*;

/**
 * Created by zhang on 10/10/16.
 * This test class is to test the Crop class.  The sample file used for testing is f3.fits.  It is used to create a output file,
 * out_f3.fits.  Both f3.fits and out_f3.fits are stored in the firefly_test_data/edu/caltech/ipac/visualize/plot/.  The end to end test is
 * load the f3.fits and creates a cropped file in the fly.  The newly created outFits file is compared with out_f3.fits.  If the
 * assertion is failed, it means the bugs are introduced after this class is written.
 *
 * It is difficult to do a unit test for this kind cases.  We the end to end test is used instead.
 */
@Deprecated
public class CropTest {


    private static String fileName = "f3.fits";
    private  Fits  inFits=null;
    private  String resultFitsName="out_f3.fits";
    private  Fits expectedFits =null;
    private  float delta =0.1f-10;

    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        inFits = FileLoader.loadFits(CropTest.class, fileName);

        //This file stored the results run when this class is written.  All future results are compared with it.
        expectedFits = FileLoader.loadFits(CropTest.class,resultFitsName);



    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        expectedFits=null;
    }

    /**
     * Validate the header card one by one
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
    private void validHeader (Fits expectedFits, Fits calculatedFits) throws FitsException {
        BasicHDU[] expectedHDUs =  expectedFits.read();

        BasicHDU[] outHDUs =calculatedFits.read();
        for (int i=0; i<expectedHDUs.length; i++){
            Header expectedHeader = expectedHDUs[i].getHeader();
            Header calculatedHeader = outHDUs[i].getHeader();
            Assert.assertEquals(expectedHeader.getNumberOfCards(), calculatedHeader.getNumberOfCards());
            Cursor  expectedIter = 	expectedHeader.iterator();
            Cursor  calculatedIter = 	calculatedHeader.iterator();
            while(expectedIter.hasNext() && calculatedIter.hasNext()){
                HeaderCard expectedCard = (HeaderCard) expectedIter.next();
                HeaderCard calculatedCard = (HeaderCard) calculatedIter.next();
                Assert.assertEquals( expectedCard.getKey(), calculatedCard.getKey());
                Assert.assertEquals( expectedCard.getValue(), calculatedCard.getValue());
            }
        }
    }

    /**
     * Validate the data stored in the FITS file
     * @param expectedFits
     * @param calculatedFits
     * @throws FitsException
     */
    private void validateData(Fits expectedFits, Fits calculatedFits) throws FitsException {
        FitsRead[] fitsReads = FitsRead.createFitsReadArray(calculatedFits);
        FitsRead[] expectedFitsRead = FitsRead.createFitsReadArray(expectedFits);
        for (int i=0;  i<fitsReads.length; i++){
            Assert.assertArrayEquals(fitsReads[i].getDataFloat(), expectedFitsRead[i].getDataFloat(), delta);
        }

    }
    @Test
    /**
     * We use the result wen this class written as a reference.  The output is saved to a file, out_f3.fits.  The newly calculated
     * data is compared with the data in out_f3.fits.
     */
    public void endToEndTest() throws FitsException {

        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;

        Fits  outFits = Crop.do_crop(inFits, min_x, min_y, max_x, max_y);

        validHeader(expectedFits,outFits);

        validateData(expectedFits,outFits);

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

        String dataPath =FileLoader.getDataPath(CropTest.class);
        Fits    inFits = FileLoader.loadFits(CropTest.class, fileName);
        String outFitsName =dataPath+ "out_"+fileName.substring(0, fileName.length()-5 )+".fits";

        int min_x = 47, min_y = 50, max_x = 349, max_y = 435;

        Fits  outFits = Crop.do_crop(inFits, min_x, min_y, max_x, max_y);

        FileOutputStream fo = new java.io.FileOutputStream(outFitsName);
        BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
        outFits.write(o);

    }

}
