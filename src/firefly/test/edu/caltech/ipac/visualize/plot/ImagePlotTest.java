package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.visualize.plot.output.PlotOutput;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by zhang on 2/16/17.
 * The unit tests are done in the following three ways:
 * 1.  Create ImagePlot and save to a file as an expected image (end to end)
 * 2.  Calculate the expected values by different utility classes/methods and then use those values as the expected values
 * 3.  Generate some expected values using the ImagePlot object.  Then those values are saved as expected values.
 *
 * For #3, it only can guard the future changes that do not introduce bugs.
 * 
 */
public class ImagePlotTest extends ConfigTest{
    private double delta = 1.e-10;
    private  String  inFitsFileName = "f3.fits";
    private Fits inFits;
    private String expectedImageFile = "f3_0_0.png";
    private static BufferedImage expectedImage;
    private String expectedImageWithMaskFile = "f3WithMask_0_0.png";
    private static BufferedImage expectedImageWithMask;
    private FitsRead fitsRead;
    private  ActiveFitsReadGroup frGroup;
    private ImagePlot imagePlot;
    private String colorBandFitsFileName = "WISE-Multi-Color-Red.fits";
    //data stored in f3.fits (nasxis1 and naxis2)
    private int width = 492;
    private int height = 504;

    @Before
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        LOG.info("Load FITS file and then prepare data for testing");
        inFits = new Fits( FileLoader.getDataPath(ImagePlotTest.class)+inFitsFileName);
        fitsRead = FitsReadFactory.createFitsReadArray(inFits)[0];
        frGroup = new ActiveFitsReadGroup();
        frGroup.setFitsRead(Band.NO_BAND, fitsRead);
        LOG.info("Create an ImagePlot object using the input data");
        imagePlot = new ImagePlot(frGroup, 0);

        LOG.info("Load expected results from files");
        expectedImage = ImageIO.read(new File(FileLoader.getDataPath(ImageDataTest.class)+expectedImageFile));
        expectedImageWithMask = ImageIO.read(new File(FileLoader.getDataPath(ImageDataTest.class)+expectedImageWithMaskFile));
        LOG.info("Done set up");
    }

    @After
    /**
     * Release the memories
     */
    public void tearDown() {

        LOG.info("Free resources");
        inFits =null;
        expectedImageWithMask=null;
        expectedImage=null;
    }

    /**
     * This is to test if the ImagePlot created is the same as what is saved.
     * @throws FitsException
     * @throws ClassNotFoundException
     */
    @Test
    public void testImagePlotCreation() throws FitsException, ClassNotFoundException {

        //validate the size of the image
        Assert.assertEquals(expectedImage.getHeight(), imagePlot.getScreenHeight());
        Assert.assertEquals(expectedImage.getWidth(), imagePlot.getScreenWidth());

         //verify the calculated imagePlot has the same point as the expected image
         for (int x = 0; x < expectedImage.getWidth(); x++) {
             for (int y = 0; y < expectedImage.getHeight(); y++) {
                 ImageWorkSpacePt iwspt = new ImageWorkSpacePt (x,y);
                 Assert.assertTrue(imagePlot.pointInPlot(iwspt));

             }
         }
    }

    /**
     * This is to test if the ImagePlot created is the sane as what is saved.
     * @throws FitsException
     * @throws ClassNotFoundException
     */
    @Test
    public void testImagePlotWithMaskCreation() throws FitsException, ClassNotFoundException {


        //create an image file for an image plot with mask
        ImageMask lsstmaskRed= new ImageMask(0, Color.RED);
        ImageMask lsstmaskGreen = new ImageMask(5, Color.GREEN);
        ImageMask lsstmaskBlue = new ImageMask(8, Color.BLUE);


        ImageMask[] lsstMasks=  {lsstmaskRed,lsstmaskGreen, lsstmaskBlue };

        ImagePlot imagePlot = new ImagePlot(frGroup, lsstMasks);

        //validate the size of the image
        Assert.assertEquals(height, imagePlot.getScreenHeight());
        Assert.assertEquals(width, imagePlot.getScreenWidth());


        //verify the calculated imagePlot has the same point as the expected image
        for (int x = 0; x < expectedImageWithMask.getWidth(); x++) {
            for (int y = 0; y < expectedImageWithMask.getHeight(); y++) {
                ImageWorkSpacePt iswpt = new ImageWorkSpacePt (x,y);
                Assert.assertTrue(imagePlot.pointInPlot(iswpt));

            }
        }
    }


    @Test
    public void testisUseForMask(){
        Assert.assertTrue(!imagePlot.isUseForMask());
    }




    /**
     * In this method:
     *   1. Calculate the expected Coordinate using another utility class
     *   2. Use the value obtained from 1 as the expected value
     *   3. Compare the Coordinate calculated by ImagePlot with the expected value
     * one calculated by ImagePlot.
     */
    @Test
    public void testGetCoordinatesOfPlot() throws FitsException {

        ImageHeader   imageHeader =  new ImageHeader(fitsRead.getHeader());
        CoordinateSys  expectedCoordinateSys= CoordinateSys.makeCoordinateSys( imageHeader.getJsys(), imageHeader .getEquinox() );
        CoordinateSys coordinateSys = imagePlot.getCoordinatesOfPlot();
        Assert.assertEquals(expectedCoordinateSys,coordinateSys );

    }

    /**
     * This is to test if the ImageDataWith is the same as the known width
     */
    @Test
    public void testGetImageDataWidth(){
        double imageFactor = fitsRead.getImageScaleFactor();

        double cWidth  = fitsRead.getImageDataWidth();
        Assert.assertEquals(width*imageFactor, cWidth, delta);
    }

    /**
     * This is to test if the ImageDataHeight is the same as the known height.
     */
    @Test
    public void testGetImageDataHeight(){


        double imageFactor = fitsRead.getImageScaleFactor();

        double cHeight  = fitsRead.getImageDataHeight();
        Assert.assertEquals(height*imageFactor, cHeight, delta);
    }


    /**
     * The known value is calculated by ImagePlot, and then save it here.
     *
     * Known ImageWorkSpacePt is 370.3222689744406;391.9982294123436
     * WorldPt wpt = new WorldPt(326.7281667, 64.5584444);
     *
     *
     * @throws FitsException
     * @throws IOException
     * @throws ProjectionException
     */
    @Test
    public void testGetImageCoords() throws FitsException, IOException, ProjectionException, NoninvertibleTransformException {
        imagePlot = new ImagePlot(frGroup, 0);
        WorldPt wpt = new WorldPt(326.7281667, 64.5584444);
        LOG.info("Used a known point as a test reference");
        ImageWorkSpacePt  iwspt = imagePlot.getImageCoords (wpt);
        Assert.assertEquals(370.3222689744406, iwspt.getX(), delta);
        Assert.assertEquals(391.9982294123436, iwspt.getY(), delta);

        /*set offset to 0 thus, iImageWorkSpacePt is the same as ImagePt
          double xpass= (iwspt.getX()- ((double)getOffsetX()))/imageScaleFactor;
          double ypass= (iwspt.getY()- ((double)getOffsetY()))/imageScaleFactor;
        */
        //using the lower corner as a reference point
        Point2D p2d = new Point2D.Double(0, 0);
    }


    /**
     * @throws FitsException
     * @throws IOException
     * @throws ProjectionException
     */
    @Test
    public void testGetImageWorkSpaceCoords() throws FitsException, IOException, ProjectionException, NoninvertibleTransformException {
        Point2D p2d = new Point2D.Double(0, 0);
        LOG.info("Used a known point as a test reference");
        ImageWorkSpacePt  iwspt = imagePlot.getImageWorkSpaceCoords(p2d);
        Assert.assertEquals(0.0, iwspt.getX(), delta);
        Assert.assertEquals(504, iwspt.getY(), delta);


    }

    /**
     * expected value: -14760.001476000147;15120.00151200015 (calculated by ImagePlot)
     * @throws FitsException
     * @throws IOException
     * @throws ProjectionException
     * @throws NoninvertibleTransformException
     */

    @Test
    public void testGetDistanceCoord() throws FitsException, IOException, ProjectionException, NoninvertibleTransformException {
        double xdistance = VisUtil.computeDistance(new ImagePt(0, 0), new ImagePt(width,0));

        double ydistance = VisUtil.computeDistance(new ImagePt(0, 0), new ImagePt(0,height));
        ImagePt imagePt = imagePlot.getDistanceCoords(new ImagePt(0,0), xdistance, ydistance);

        Assert.assertEquals(-14760.001476000147, imagePt.getX(), delta);
        Assert.assertEquals(15120.00151200015, imagePt.getY(), delta);
    }

    @Test
    public void testPointInPlot() throws FitsException, IOException, ProjectionException, NoninvertibleTransformException {
        WorldPt wpt = new WorldPt(326.7281667, 64.5584444);
        Assert.assertTrue(imagePlot.pointInPlot(wpt));
    }



    /**
      * This main method is used to create an output file which is used as a reference to validate the
      * unit test.  The input file is : "f3.fits".
      *
      * @param args
      * @throws FitsException
      * @throws IOException
      * @throws ClassNotFoundException
      */
    public static void main(String [] args) throws FitsException, IOException, ClassNotFoundException {


        String inFitsName = "f3.fits";
        Fits fits = new Fits( FileLoader.getDataPath(ImagePlotTest.class)+inFitsName);


        FitsRead fitsRead = FitsReadFactory.createFitsReadArray(fits)[0];
        ActiveFitsReadGroup frGroup = new ActiveFitsReadGroup();
        frGroup.setFitsRead(Band.NO_BAND, fitsRead);


        ImagePlot imagePlot = new ImagePlot(frGroup, 0);

        PlotOutput po = new PlotOutput(imagePlot, frGroup);
        po.writeTilesFullScreen( new File(FileLoader.getDataPath(ImagePlotTest.class)), "f3", PlotOutput.PNG, true, true);



        //create an image file for an image plot with mask
        ImageMask lsstmaskRed= new ImageMask(0, Color.RED);
        ImageMask lsstmaskGreen = new ImageMask(5, Color.GREEN);
        ImageMask lsstmaskBlue = new ImageMask(8, Color.BLUE);


        ImageMask[] lsstMasks=  {lsstmaskRed,lsstmaskGreen, lsstmaskBlue };

        imagePlot = new ImagePlot(frGroup, lsstMasks);
        po = new PlotOutput(imagePlot, frGroup);
        po.writeTilesFullScreen( new File(FileLoader.getDataPath(ImagePlotTest.class)), "f3WithMask", PlotOutput.PNG, true, true);

    }

}
