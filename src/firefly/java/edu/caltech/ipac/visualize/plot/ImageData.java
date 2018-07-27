/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.ImageStretch;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.Arrays;

/**
 * 9/11/15
 *   Add a new constructor to handle mask plot
 *   Modified the constructImage to handle both mask and image plots
 *   Add a new method to create an IndexColorModel dynamically according to the input ImageMask array
 */
public class ImageData implements Serializable {


    public enum ImageType {TYPE_8_BIT, TYPE_24_BIT}
    private IndexColorModel cm;
    private int colorTableID = 0;   // this is not as flexible as color model and will be set to -1 when color model is set
    private BufferedImage bufferedImage;
    private boolean imageOutOfDate = true;
    private ImageMask[] imageMasks=null;

    private final ImageType imageType;
    private final RangeValues rangeValuesAry[];
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int lastPixel;
    private final int lastLine;

    private WritableRaster raster;


    private ImageData( ImageType imageType, int x, int y, int width, int height, RangeValues rangeValues) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lastPixel = this.x + this.width -1;
        this.lastLine = this.y + this.height -1;
        this.rangeValuesAry= new RangeValues[] {rangeValues, rangeValues, rangeValues};
        this.imageType = imageType;
    }


    public ImageData(ImageType imageType,
                     int colorTableID,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height) {
        this(imageType,x,y,width,height,rangeValues);
        this.colorTableID = colorTableID;
        cm = ColorTable.getColorModel(colorTableID);
        raster = imageType==ImageType.TYPE_24_BIT ?
                Raster.createBandedRaster( DataBuffer.TYPE_BYTE, this.width, this.height,3, null) :
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, this.width, this.height, 1, null);
    }

    //LZ 7/20/15 add this to make an ImageData with given IndexColorModel
    public ImageData(ImageType imageType,
                     ImageMask[] iMasks,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height) {

        this(imageType,x,y,width,height,rangeValues);
        imageMasks=iMasks;
        cm =  getIndexColorModelForMask(iMasks);
        raster = imageType==ImageType.TYPE_24_BIT ?
                Raster.createBandedRaster( DataBuffer.TYPE_BYTE, width,height,3, null) :
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);
    }

    public BufferedImage getImage(FitsRead fitsReadAry[])       {
        if (imageOutOfDate) constructImage(fitsReadAry);
        return bufferedImage;
    }


    public void freeResources() {
        cm = null;
        bufferedImage = null;
        raster = null;
        imageOutOfDate = true;
    }

    public int getX() { return x;}
    public int getY() { return y;}

    public int getWidth() { return width;}
    public int getHeight() { return height;}

    void setColorModel(IndexColorModel color_model) {
        colorTableID = -1;
        cm =color_model;
        imageOutOfDate =true;
    }

    public int getColorTableId() { return colorTableID; }


    /**
     * don't compute the color model.  Should only be call from ImageDataGroup
     * @param colorTableID the id
     */
    void setColorTableIdOnly(int colorTableID) { this.colorTableID = colorTableID; }


    IndexColorModel getColorModel() { return cm; }

    void markImageOutOfDate() { imageOutOfDate = true; }

    boolean isImageOutOfDate() { return imageOutOfDate; }

    void recomputeStretch(int idx, RangeValues updatedRangeValues) {
        imageOutOfDate = true;
        rangeValuesAry[idx] = updatedRangeValues;
    }



   // Testing Mask 07/16/16 LZ
    /**
     * Build a dynamic IndexColorModel which contains the colors defined in the imageMask array plus a white background color.
     * The background color should be transparent.  The lsstMasks is sorted according to the index.  mask0=lsstMasks[0].getIndex()=1
     * and mask1=lsstMasks[1].getIndex=5:
     *
     *       pixel index   color
     *       0             mask0.color
     *       1             mask1.color
     *       3             white color
     *
     *
     * @param lsstMasks the mask settings
     * @return the new color model
     */

    private static IndexColorModel getIndexColorModelForMask(ImageMask[] lsstMasks){

        byte[] cmap=new byte[4*(lsstMasks.length+1) ];

        Arrays.fill( cmap, (byte)0);
        for (int i=0; i<lsstMasks.length; i++){
            cmap[4*i]  =(byte) lsstMasks[i].getColor().getRed();
            cmap[4*i+1]=(byte) lsstMasks[i].getColor().getGreen();
            cmap[4*i+2]=(byte) lsstMasks[i].getColor().getBlue();
            cmap[4*i+3]=(byte) 255; //opaque
        }

         //store the transparent white color at pixel index = lsstMasks.length
        cmap[4*lsstMasks.length]=(byte) 255;
        cmap[4*lsstMasks.length+1]= (byte) 255;
        cmap[4*lsstMasks.length+2]= (byte) 255;
        cmap[4*lsstMasks.length+3]= (byte) 0; //alpha=0, transparent

        return new IndexColorModel(8, lsstMasks.length+1, cmap, 0, true);
    }

    private void constructImage(FitsRead fitsReadAry[])  {
        DataBufferByte db= (DataBufferByte) raster.getDataBuffer();
        if (imageType ==ImageType.TYPE_8_BIT) {
            if (imageMasks!=null && imageMasks.length!=0){
               bufferedImage = new BufferedImage(cm, raster, false, null);
               fitsReadAry[0].doStretchMask( db.getData(0), x, lastPixel, y, lastLine, imageMasks);

            }
            else {
                bufferedImage = new BufferedImage(cm, raster, false, null);
                ImageHeader imHead= new ImageHeader(fitsReadAry[0].getHeader());
                ImageStretch.stretchPixels8Bit(rangeValuesAry[Band.NO_BAND.getIdx()],
                                               fitsReadAry[0].getRawFloatAry(), db.getData(0),
                                               imHead,  fitsReadAry[0].getHistogram(),
                                               x, lastPixel, y, lastLine );

            }
        }
        else if (imageType ==ImageType.TYPE_24_BIT) {
            bufferedImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);


            float[][] float1dAry= new float[3][];
            byte[][] pixelDataAry= new byte[3][];
            ImageHeader imHeadAry[]= new ImageHeader[3];
            Histogram[] histAry= new Histogram[3];
            for(int i=0;i<3; i++) {
                float1dAry[i]= fitsReadAry[i].getRawFloatAry();
                pixelDataAry[i]= db.getData(i);
                imHeadAry[i]= new ImageHeader(fitsReadAry[i].getHeader());
                histAry[i]= fitsReadAry[i].getHistogram();
            }
            ImageStretch.stretchPixels3Color(rangeValuesAry, float1dAry, pixelDataAry, imHeadAry, histAry,
                                             x, lastPixel, y, lastLine );
            bufferedImage.setData(raster);
        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        imageOutOfDate =false;
    }
}
