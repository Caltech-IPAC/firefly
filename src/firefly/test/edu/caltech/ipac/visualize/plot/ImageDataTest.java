package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
/**
 * Created by zhang on 2/3/17.
 */
public class ImageDataTest {
    private static  Fits inFits;
    private static String fileName = "f3.fits";
    private static FitsRead[] frArray;

    private static ImageData.ImageType IMAGE_TYPE = ImageData.ImageType.TYPE_8_BIT;
    private static int COLROID = 0;
    private static RangeValues rangeValues;
    //save to png file since it is lossless format and preserves the pixels
    private static String imageFileName = "imageDataTest.png";
    private static BufferedImage expectedImage;
    private static String imageWithMaskFileName = "imageDataWithMaskTest.png";
    private static BufferedImage expectedImageWithMask;
    private static ImageMask[] imageMasks = {new ImageMask(0, Color.blue), new ImageMask(1, Color.green) };
    private static ImageData imageData;
    @Before

    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {
        //this FITS file has three extensions.  Using it as expected value to get if the FitsRead can get all extensions

        inFits = FileLoader.loadFits(ImageDataTest.class , fileName);
        frArray = FitsRead.createFitsReadArray(inFits);
        rangeValues = FitsRead.getDefaultRangeValues();
        expectedImage = ImageIO.read(new File(FileLoader.getDataPath(ImageDataTest.class)+imageFileName));
        expectedImageWithMask = ImageIO.read(new File(FileLoader.getDataPath(ImageDataTest.class)+imageWithMaskFileName));

        imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        frArray=null;
        rangeValues=null;
        expectedImage=null;
        expectedImageWithMask =null;
        imageData =null;

    }

    /**
     * To test to see if it gets the correct x value
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testGetX() throws FitsException, IOException{
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        Assert.assertEquals(0, imageData.getX());
    }

    /**
     * To get to see if it gets the correct y value
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testGetY() throws FitsException, IOException{
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        Assert.assertEquals(0, imageData.getY());
    }

    /**
     * To test to see if it gets teh correct height
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testGetHeight() throws FitsException, IOException{
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        Assert.assertEquals(100, imageData.getHeight());
    }

    /**
     * To test to see if it gets the correct width
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testGetWidth() throws FitsException, IOException{
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        Assert.assertEquals(100, imageData.getWidth());
    }

    /**
     * Test to see if it gets the initial color model ID
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testGetColorID() throws FitsException, IOException{
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        Assert.assertEquals(0, imageData.getColorTableId());
    }

    /**
     * To test to see if it sets the color model
     * @throws FitsException
     * @throws IOException
     */
    @Test
    public void testSetColorModel() throws FitsException, IOException{
        IndexColorModel colorModel = ColorTable.getColorModel(2);
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        imageData.setColorModel(colorModel);
        Assert.assertEquals(-1, imageData.getColorTableId());

    }

    /**
     * To test to see if it get the correct ColorModel
     */
    @Test
    public void testGetColorModel(){
        IndexColorModel indexColorModel = imageData.getColorModel();
        Assert.assertNotNull(indexColorModel );
        Assert.assertEquals(ColorTable.getColorModel(0), indexColorModel);
    }

    /**
     * To test to see if the imgage is changed after recomputing stretches
     */
    @Test
    public void testRecomputeStretch(){
        //Recalculate stretch
        imageData.recomputeStretch(frArray, 0, rangeValues, true);
        BufferedImage  stretchImage  =imageData.getImage(frArray);
        Assert.assertNotEquals(expectedImage, stretchImage);
    }

    /**
     * This is an end to end test.  It uses a pre-stored images as testing base to compare to see if the
     * new images are the same as the corresponding stored ones
     *
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void endToEndTest() throws FitsException, IOException, ClassNotFoundException {

        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        //Test the ImageData without mask
        BufferedImage calculatedImage = imageData.getImage(frArray);

        compareImage(expectedImage,calculatedImage);


        //Test the ImageData withmask
        ImageData  imageDataWithMask = new ImageData(frArray, IMAGE_TYPE,imageMasks, rangeValues, 0,0, 100, 100, true );
        BufferedImage calculatedImageWithMask  = imageDataWithMask.getImage(frArray);
        compareImage(expectedImageWithMask,calculatedImageWithMask);

    }


    /**
     * This method does the image comparasion to see if the data type, data size and the pixel values are the same
     * @param expectedImage
     * @param calculatedImage
     */
    public static void compareImage( BufferedImage expectedImage, BufferedImage calculatedImage) {

        // take buffer data from botm image files //
        DataBuffer dbA = expectedImage.getData().getDataBuffer();
        int dataTypeSizeA= dbA.getDataTypeSize(DataBuffer.TYPE_BYTE);

        DataBuffer dbB =calculatedImage.getData().getDataBuffer();
        int dataTypeSizeB = dbB.getDataTypeSize(DataBuffer.TYPE_BYTE);


        //validate the image size
        Assert.assertEquals(dataTypeSizeA, dataTypeSizeB);
        Assert.assertEquals(dbA.getSize(), dbB.getSize());


        if (expectedImage.getWidth() == calculatedImage.getWidth() && expectedImage.getHeight() == calculatedImage.getHeight()) {
            for (int x = 0; x < calculatedImage.getWidth(); x++) {
                for (int y = 0; y < calculatedImage.getHeight(); y++) {
                    Assert.assertEquals(expectedImage.getRGB(x, y), calculatedImage.getRGB(x, y));

                }
            }
        }


    }

    /**
     * [Feb-8-2017]
     * This main method is used to craete an image file as the time the test is written.  It is used
     * an expected image.  When the ImageData is modified in the future, if the new image is not the
     * same as the expected one created at this time, the modification introduces bugs.
     *
     * @param args
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void  main (String[] args) throws FitsException, IOException, ClassNotFoundException {
        inFits = FileLoader.loadFits(ImageDataTest.class , fileName);
        frArray = FitsRead.createFitsReadArray(inFits);
        rangeValues = FitsRead.getDefaultRangeValues();
        ImageData imageData = new ImageData(frArray, IMAGE_TYPE,COLROID, rangeValues, 0,0, 100, 100, true );
        BufferedImage bufferedImage = imageData.getImage(frArray);
        File outputfile = new File(FileLoader.getDataPath(ImageDataTest.class)+"imageDataTest.png");
        ImageIO.write(bufferedImage, "png", outputfile);



        //test ImageData with mask
        imageData = new ImageData(frArray, IMAGE_TYPE,imageMasks,rangeValues, 0,0, 100, 100, true );
        bufferedImage = imageData.getImage(frArray);
        outputfile = new File(FileLoader.getDataPath(ImageDataTest.class)+"imageDataWithMaskTest.png");
        ImageIO.write(bufferedImage, "png", outputfile);

    }

}
