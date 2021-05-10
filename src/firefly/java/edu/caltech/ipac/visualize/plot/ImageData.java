/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.ImageStretch;
import edu.caltech.ipac.visualize.plot.plotdata.RGBIntensity;
import nom.tam.fits.Header;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
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
    private ColorModel cm;
    private int colorTableID = 0;   // this is not as flexible as color model and will be set to -1 when color model is set
    private BufferedImage bufferedImage;
    private boolean imageOutOfDate = true;
    private ImageMask[] imageMasks=null;
    private byte[] saveStandardStretch;
    private byte[][] save3CStretch;

    private final ImageType imageType;
    private final RangeValues[] rangeValuesAry;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int lastPixel;
    private final int lastLine;

    private WritableRaster raster;

    // used for hue-preserving RGB only
    private RGBIntensity rgbIntensity; // stats for intensity


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
        cm = imageType==ImageType.TYPE_24_BIT ?
                new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                                                             Transparency.OPAQUE, DataBuffer.TYPE_BYTE) :
                ColorTable.getColorModel(colorTableID);

        raster = imageType==ImageType.TYPE_24_BIT ?
                Raster.createBandedRaster( DataBuffer.TYPE_BYTE, this.width, this.height,3, null) :
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, this.width, this.height, 1, null);
    }

    //LZ 7/20/15 add this to make an ImageData with given IndexColorModel
    public ImageData(ImageMask[] iMasks,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height) {

        this(ImageType.TYPE_8_BIT,x,y,width,height,rangeValues);
        imageMasks=iMasks;
        cm =  getIndexColorModelForMask(iMasks);
        raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);
    }

    public void setRGBIntensity(RGBIntensity rgbIntensity) {
        this.rgbIntensity = rgbIntensity;
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
        rgbIntensity = null;
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


    ColorModel getColorModel() { return cm; }

    void markImageOutOfDate() { imageOutOfDate = true; }

    boolean isImageOutOfDate() { return imageOutOfDate; }

    void recomputeStretch(int idx, RangeValues updatedRangeValues) {
        imageOutOfDate = true;
        rangeValuesAry[idx] = updatedRangeValues;
    }

    public void setRangeValuesAry(RangeValues[] rvAry) {
        for(int i= 0; i<3; i++) {
            if (i<rvAry.length && rvAry[i]!=null) {
                this.rangeValuesAry[i]= rvAry[i];
            }
        }
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
            float[][] float1dAry= new float[3][];
            byte[][] pixelDataAry= new byte[3][];
            ImageHeader imHeadAry[]= new ImageHeader[3];
            Histogram[] histAry= new Histogram[3];
            for(int i=0;i<3; i++) {
                if (fitsReadAry[i] == null) {
                    float1dAry[i]=null;
                    imHeadAry[i]=null;
                    histAry[i]=null;
                } else {
                    float1dAry[i] = fitsReadAry[i].getRawFloatAry();
                    imHeadAry[i]= new ImageHeader(fitsReadAry[i].getHeader());
                    histAry[i]= fitsReadAry[i].getHistogram();
                }
                pixelDataAry[i]= db.getData(i);

            }
            ImageStretch.stretchPixels3Color(rangeValuesAry, float1dAry, pixelDataAry, imHeadAry, histAry,
                    rgbIntensity, x, lastPixel, y, lastLine );
            bufferedImage = new BufferedImage(cm, raster, false, null);
        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        imageOutOfDate =false;
    }


    public byte[][] stretch3Color(float [][] float1dAry, ImageHeader [] imHeadAry, Histogram[] histAry, Header header, RGBIntensity rgbIntensity) {
        byte[][] pixelDataAry= new byte[3][];
        for(int i=0;i<3; i++) {
            pixelDataAry[i]= new byte[this.width * this.height];
        }

        ImageStretch.stretchPixels3Color(rangeValuesAry, float1dAry, pixelDataAry, imHeadAry, histAry,
                rgbIntensity, x, lastPixel, y, lastLine );
        return pixelDataAry;
    }


    public byte[] stretch8bit(final float [] float1d, final Header header, final Histogram histogram) {
        final ImageHeader imHead= new ImageHeader(header) ;
        byte [] byteAry= new byte[this.width * this.height];
        ImageStretch.stretchPixels8Bit(rangeValuesAry[Band.NO_BAND.getIdx()],
                float1d, byteAry,
                imHead,  histogram,
                x, lastPixel, y, lastLine );
        return byteAry;
    }

    public void stretch8bitAndSave(final float [] float1d, final Header header, final Histogram histogram) {
        this.saveStandardStretch = stretch8bit(float1d,header,histogram);
    }

    public byte[] getSavedStandardStretch() { return this.saveStandardStretch; }

    public void stretch3ColorAndSave(float [][] float1dAry, ImageHeader [] imHeadAry, Histogram[] histAry,
                                  Header header, RGBIntensity rgbIntensity) {
        this.save3CStretch = stretch3Color(float1dAry,imHeadAry,histAry,header,rgbIntensity);
    }

    public byte[][] getSaved3CStretch() { return this.save3CStretch; }
}

