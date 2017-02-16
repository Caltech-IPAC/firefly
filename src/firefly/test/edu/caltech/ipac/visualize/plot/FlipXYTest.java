package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.util.FitsGenerator;
import edu.caltech.ipac.firefly.util.FitsValidation;
import nom.tam.fits.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import java.io.FileOutputStream;
import java.io.IOException;



/**
 * Created by zhang on 1/31/17.
 * DM-7780
 * This file contains three kinds of tests:
 *
 * 1. Unit tests using small simulated data
 * 2. Unit tests based on the the common knowledge that an image flipped back forth will return to the original
 * 2. An end to end test using a real Fits file.
 *
 * NOTE: the small simulated data causes some Histogram error messages.  Please ignore it since it does not hurt
 * the unit tests.
 *
 *
 */
public class FlipXYTest extends FitsValidation {
    private Fits inFits;
    private FitsRead fitsRead;
    private double delta = 0.1e-10;

    //end to end test info
    private static String inFileName = "f3.fits";
    private static String expectedFlipXYFitsFileName  = "f3FlipedXY.fits";
   

    @Before
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        //create a simulated FITS object
        inFits = FitsGenerator.getSimulateFits();
        fitsRead = FitsRead.createFitsReadArray(inFits)[0];
    }

    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits = null;
        fitsRead=null;

    }

    /**
     * This test uses very small data array which can be easily reversed by eye-sight.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testDoFlipXWithSmallData() throws FitsException, IOException, ClassNotFoundException {

        float[][] fData = {
                {1.0f, 2.0f, 3.0f, 4.0f, 5.0f},
                {6.0f, 7.0f, 8.0f, 9.0f, 10.0f},
                {11.0f, 12.0f, 13.0f, 14.0f, 15.0f}
        };

        float[][] expectedFlipXData = {

                {11.0f, 12.0f, 13.0f, 14.0f, 15.0f},
                {6.0f, 7.0f, 8.0f, 9.0f, 10.0f},
                {1.0f, 2.0f, 3.0f, 4.0f, 5.0f},
        };

        Fits fits = FitsGenerator.getSimulateFits(fData);
        FitsRead fitsRead = FitsRead.createFitsReadArray(fits)[0];


        FlipXY flipX = new FlipXY(fitsRead, "xAxis");
        FitsRead flipedFR = flipX.doFlip();
        float[][] flippedData = (float[][]) flipedFR.getHDU().getData().getData();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                Assert.assertEquals(expectedFlipXData[i][j], flippedData[i][j], delta);
            }
        }

    }

    /**
     * This one uses a small array to test the flip so that the flipped results can be compared with
     * known results.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testDoFlipYWithSmallData() throws FitsException, IOException, ClassNotFoundException {

        float[][] fData = {
                {1.0f, 2.0f, 3.0f, 4.0f, 5.0f},
                {6.0f, 7.0f, 8.0f, 9.0f, 10.0f},
                {11.0f, 12.0f, 13.0f, 14.0f, 15.0f}
        };

        float[][] expectedFlipYData = {

                {5.0f, 4.0f, 3.0f, 2.0f, 1.0f},
                {10.0f, 9.0f, 8.0f, 7.0f, 6.0f},
                {15.0f, 14.0f, 13.0f, 12.0f, 11.0f}
        };

        Fits fits = FitsGenerator.getSimulateFits(fData);
        FitsRead fitsRead = FitsRead.createFitsReadArray(fits)[0];


        FlipXY flipY = new FlipXY(fitsRead, "yAxis");
        FitsRead flipedFR = flipY.doFlip();
        float[][] flippedData = (float[][]) flipedFR.getHDU().getData().getData();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                Assert.assertEquals(expectedFlipYData[i][j], flippedData[i][j], delta);
            }
        }

    }

    /**
     * This is to test Flip for 3 dimensional data.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testDoFlipWith3DimData() throws FitsException, IOException, ClassNotFoundException {

        float[][][] fData = {
                {
                        {1.0f, 2.0f, 3.0f, 4.0f, 5.0f},
                        {6.0f, 7.0f, 8.0f, 9.0f, 10.0f},
                        {11.0f, 12.0f, 13.0f, 14.0f, 15.0f}
                }
        };
        Fits fits = FitsGenerator.getSimulateFits(fData);
        FitsRead fitsRead = FitsRead.createFitsReadArray(fits)[0];

        validateFlipX(fitsRead);
        validateFlipY(fitsRead);
    }


    /**
     * This is test the Flip for 4 dimensional data.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testDoFlipWith4DimData() throws FitsException, IOException, ClassNotFoundException {

        float[][][][] fData = {{
                {
                        {1.0f, 2.0f, 3.0f, 4.0f, 5.0f},
                        {6.0f, 7.0f, 8.0f, 9.0f, 10.0f},
                        {11.0f, 12.0f, 13.0f, 14.0f, 15.0f}
                }
        }};
        Fits fits = FitsGenerator.getSimulateFits(fData);
        FitsRead fitsRead = FitsRead.createFitsReadArray(fits)[0];

        validateFlipX(fitsRead);
        validateFlipY(fitsRead);
    }



    /**
     * This it to test the flip for two dimensional data, based on the principle that double flip returns itself.
     *
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testDoFlip() throws FitsException, IOException {

        validateFlipX(fitsRead);
        validateFlipY(fitsRead);
    }
    
    /**
     * This is to validate the flipped results against x axis.  When the data flipped twice along
     * the same axis, the result will be the same as the original input.
     * @param fitsRead
     * @throws FitsException
     * @throws IOException
     */
    private void validateFlipX (FitsRead fitsRead) throws FitsException, IOException{


        //make a first flip along x direction
        FlipXY flipX_1 = new FlipXY(fitsRead, "xAxis");
        //do the first flip and get a flipped FitsRead
        FitsRead flipedOnceFR = flipX_1.doFlip();
        Assert.assertNotEquals(fitsRead.getHDU().getData().getData(),flipedOnceFR.getHDU().getData().getData() );

        //create a second flipXY object and use the previous flipped FitsRead as an input
        FlipXY flipX_2 = new FlipXY(flipedOnceFR, "xAxis");
        FitsRead flipedTwiceFR = flipX_2.doFlip();

        validateSingleHDU(fitsRead.getHDU(), flipedTwiceFR.getHDU());

        Assert.assertArrayEquals(fitsRead.getDataFloat(), flipedTwiceFR.getDataFloat(), (float) delta);

    }

    /**
     * This is to validate the flipped results against y axis.  When the data flipped twice along
     * the same axis, the result will be the same as the original input.
     * @param fitsRead
     * @throws FitsException
     * @throws IOException
     */
    public void validateFlipY(FitsRead fitsRead) throws FitsException, IOException{

        //make the first flip along yAxis
        FlipXY flipY_1 = new FlipXY(fitsRead, "yAxis");
        FitsRead flipedOnceFR = flipY_1.doFlip();

        Assert.assertNotEquals(fitsRead.getHDU().getData().getData(),flipedOnceFR.getHDU().getData().getData());
        
        //create a second flipXY object and use the previous flipped FitsRead as an input
        FlipXY flipY_2 = new FlipXY(flipedOnceFR, "yAxis");
        FitsRead flipedTwiceFR = flipY_2.doFlip();

        validateSingleHDU(fitsRead.getHDU(), flipedTwiceFR.getHDU());
        Assert.assertArrayEquals(fitsRead.getDataFloat(), flipedTwiceFR.getDataFloat(), (float) delta);
    }

    @Test
    /**
     * This uses a real Fits Image to run the unit test
     */
    public void endToEndTest() throws FitsException {
        Fits inFits = FileLoader.loadFits(FlipXYTest.class, inFileName);
        FitsRead fitsRead = FitsRead.createFitsReadArray(inFits)[0];
        FlipXY flipX = new FlipXY(fitsRead, "xAxis");
        FitsRead flipedX = flipX.doFlip();

        FlipXY flipY = new FlipXY(flipedX, "yAxis");
        FitsRead flipedXY = flipY.doFlip();


        inFits = FileLoader.loadFits(FlipXYTest.class, expectedFlipXYFitsFileName);
        FitsRead  expectedFlipXY = FitsRead.createFitsReadArray(inFits)[0];

        validateSingleHDU(expectedFlipXY.getHDU(),flipedXY.getHDU());
        Assert.assertArrayEquals(expectedFlipXY.getDataFloat(),flipedXY.getDataFloat(), (float) delta);

    }


    /**
     * This main method is used to generate a Fits file that save as a reference file for end to end test
     * @param args
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main (String[] args) throws FitsException, IOException, ClassNotFoundException {
        Fits inFits = FileLoader.loadFits(FlipXYTest.class, "f3.fits");
        FitsRead fitsRead = FitsRead.createFitsReadArray(inFits)[0];

        FlipXY flipX = new FlipXY(fitsRead, "xAxis");
        FitsRead flipedX = flipX .doFlip();


        FlipXY flipY = new FlipXY(flipedX, "yAxis");
        FitsRead flipedXY= flipY .doFlip();

        String outFitsName ="f3FlipedXY.fits";
        FileOutputStream fo = new java.io.FileOutputStream(FileLoader.getDataPath(FlipXYTest.class)+outFitsName);
        flipedXY.writeSimpleFitsFile(fo);
        fo.close();


    }


}