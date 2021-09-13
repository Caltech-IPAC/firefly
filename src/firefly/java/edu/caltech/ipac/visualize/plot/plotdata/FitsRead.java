/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_EXT;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_OFF;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_PL;


/**
 *
 *
 */
public class FitsRead implements Serializable {
    private static final ArrayList<Integer> SUPPORTED_BIT_PIXS = new ArrayList<>(Arrays.asList(8, 16, 32, -32, -64));
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();
    private final int planeNumber;
    private final int hduNumber;
    private final Header zeroHeader;
    private BasicHDU hdu;
    private float[] float1d;
    private Header header;
    private Histogram hist;

//    private final long HDUOffset;
    private final CoordinateSys coordinateSys;

    private final int maptype;
    private final double cdelt1;
    private final String bunit;

    private final boolean tileCompress;


    /**
     * Cachable class made for holding FITS data.
     *
     *
     * @param imageHdu this hdu to build this FitsRead around
     * @throws FitsException if we can process the data
     */
    FitsRead( BasicHDU imageHdu, Header zeroHeader, boolean clearHdu) throws FitsException {

        tileCompress= (imageHdu instanceof CompressedImageHDU);

        if (imageHdu instanceof ImageHDU) {
            hdu = imageHdu;
        }
        else if (imageHdu instanceof CompressedImageHDU) {
            hdu = ((CompressedImageHDU) imageHdu).asImageHDU();
        }
        else {
            throw new FitsException("imageHdu much be a ImageHDU or a CompressedImageHDU");
        }
        this.header = hdu.getHeader();

        int bitpix = getBitPix();
        if (!SUPPORTED_BIT_PIXS.contains(bitpix)) Logger.warn("Unimplemented bitpix = " + bitpix);

        this.zeroHeader= zeroHeader;

        this.planeNumber = header.getIntValue(SPOT_PL, 0);
        this.hduNumber = header.getIntValue(SPOT_EXT, 0);
        long HDUOffset= header.getIntValue(SPOT_OFF, 0);
        this.bunit= hdu.getBUnit()!=null ? hdu.getBUnit() : "DN";

        ImageHeader imageHeader = new ImageHeader(header, HDUOffset, planeNumber);
        this.maptype= imageHeader.maptype;
        this.cdelt1= imageHeader.cdelt1;
        this.coordinateSys= imageHeader.determineCoordSys();


        float1d = FitsReadUtil.getImageHDUDataInFloatArray(hdu); //convert to float to do all the calculations

        if (clearHdu) hdu= null;

        double bscale= getBscale();
        double bzero= getBzero();
        double datamax = header.getDoubleValue("DATAMAX", Double.NaN);
        double datamin = header.getDoubleValue("DATAMIN", Double.NaN);
        hist= new Histogram(float1d, (datamin - bzero) / bscale, (datamax - bzero) / bscale);
    }


    public boolean isTileCompress() { return tileCompress; }
    public int getNaxis() { return header.getIntValue("NAXIS",0); }
    public int getNaxis1() { return header.getIntValue("NAXIS1",0); }
    public int getNaxis2() { return header.getIntValue("NAXIS2",0); }
    public int getNaxis3() { return (getNaxis2() > 2) ? header.getIntValue("NAXIS3") : 1; }
    public String getBUnit() { return this.bunit;}
    public double getBscale() { return header.getDoubleValue("BSCALE", 1.0); }
    public double getBzero() { return header.getDoubleValue("BZERO", 0.0); }
    public double getCdelt1() { return this.cdelt1; }
    public double getBlankValue() {return header.getDoubleValue("BLANK", Double.NaN);}
    public int getBitPix() {return header.getIntValue("BITPIX"); }

    public String getOrigin() {
        return header.getStringValue(ImageHeader.ORIGIN)!=null ? header.getStringValue(ImageHeader.ORIGIN) : "";
    }
    public CoordinateSys getImageCoordinateSystem() { return this.coordinateSys; }
    public int getProjectionType() { return this.maptype; }

    public String getExtType() {
        HeaderCard hc= header.findCard("EXTTYPE");
        return (hc!=null) ? hc.getValue() : "";
    }

    public float[] getRawFloatAry() { return float1d; }

    public static RangeValues getDefaultFutureStretch() { return DEFAULT_RANGE_VALUE; }

    public static void setDefaultFutureStretch(RangeValues defaultRangeValues) {
        DEFAULT_RANGE_VALUE = defaultRangeValues;
    }
    

    /**
     * Add the mask layer to the existing image
     * @param pixelData array of byte data
     * @param startPixel  start pixel in the data
     * @param lastPixel last pixel
     * @param startLine start line
     * @param lastLine list line
     * @param lsstMasks mask array
     */
    public synchronized void doStretchMask(
                                       byte[] pixelData,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine,
                                       ImageMask[] lsstMasks)  {

        byte blank_pixel_value = (byte) 255;
        int[] pixelhist = new int[256];
        ImageStretch.stretchPixelsForMask(startPixel, lastPixel, startLine, lastLine, this.getNaxis1(),
                        blank_pixel_value, float1d, pixelData, pixelhist, lsstMasks);
    }



    /**
     * Get flux of pixel at given "ImagePt" coordinates
     * "ImagePt" coordinates have 0,0 lower left corner of lower left pixel
     * of THIS image
     *
     * @param ipt ImagePt coordinates
     */
    public double getFlux(ImagePt ipt)
            throws PixelValueException {


        int xint = (int) Math.round(ipt.getX() - 0.5);
        int yint = (int) Math.round(ipt.getY() - 0.5);

        if ((xint < 0) || (xint >= this.getNaxis1()) ||
                (yint < 0) || (yint >= this.getNaxis2())) {
            throw new PixelValueException("location not on the image");
        }

        int index = yint * this.getNaxis1() + xint;

        double raw_dn = float1d[index];

        return (!getOrigin().startsWith(ImageHeader.PALOMAR_ID)) ?
                ImageStretch.getFluxStandard(raw_dn,getBlankValue(),getBscale(),getBzero()) :
                ImageStretch.getFluxPalomar(raw_dn,getBlankValue(), this.header);
    }

    public String getFluxUnits() {
        String bunit = getBUnit();
        if (bunit.startsWith("HITS")) return "frames";
        if (getOrigin().startsWith(ImageHeader.PALOMAR_ID)) return "mag";
        return bunit;
    }

    public boolean hasHdu() { return hdu!=null;}

    public BasicHDU getHDU() {
        if (hdu==null) throw new IllegalArgumentException("HDU has been cleared, there is not longer access to it.");
        return hdu;
    }


    /**
     * Return an array where each element corresponds to an element of
     * the histogram, and the value in each element is the screen pixel
     * value which would result from an image pixel which falls into that
     * histogram bin.
     *
     * @return array of byte (4096 elements)
     */
    public byte[] getHistColors(Histogram hist, RangeValues rangeValues) {
        return ImageStretch.getHistColors( hist, rangeValues, float1d,
                getBzero(), getBscale(), getNaxis1(), getNaxis2(), getBitPix(), getBlankValue());
    }

    public Header getHeader() { return header; }
    public Header getZeroHeader() { return zeroHeader; }
    public Histogram getHistogram() { return hist; }
    public int getImageScaleFactor() { return 1; }



    public static RangeValues getDefaultRangeValues() { return (RangeValues) DEFAULT_RANGE_VALUE.clone(); }







    /**
     * return the plane number indicating which plane in a FITS cube
     * this image came from.
     * return value:
     * 0:  this was the only image - there was no cube
     * 1:  this was the first plane in the FITS cube
     * 2:  this was the second plane in the FITS cube
     * etc.
     */
    public int getPlaneNumber() {
        return planeNumber;
    }

    /**
     * return the extension number indicating which extension this image
     * was in the original FITS image
     * return value:
     * 0:  this was the primary image (not an extension) in a FITS file with
     * extensions
     * 1:  this was the first extension in the FITS file
     * 2:  this was the second extension in the FITS file
     * etc.
     */
    public int getHduNumber() { return hduNumber; }

    /**
     * The Bscale  keyword shall be used, along with the BZERO keyword, when the array pixel values are not the true  physical  values,
     * to transform the primary data array  values to the true physical values they represent, using Eq. 5.3. The value field shall contain a
     * floating point number representing the coefficient of the linear term in the scaling equation, the ratio of physical value to array value
     * at zero offset. The default value for this keyword is 1.0.BZERO Keyword
     * BZERO keyword shall be used, along with the BSCALE keyword, when the array pixel values are not the true  physical values, to transform
     * the primary data array values to the true values. The value field shall contain a floating point number representing the physical value corresponding to an array value of zero. The default value for this keyword is 0.0.
     * The transformation equation is as follows:
     * physical_values = BZERO + BSCALE × array_value	(5.3)
     *
     * This method return the physical data value at the pixels as an one dimensional array
     */
    public float[] getDataFloat() {
        if (getBscale()==1.0 && getBzero()==0) return float1d;
        float[] fData = new float[float1d.length];
        float bscale= (float) getBscale();
        float bzero= (float) getBzero();
        for (int i = 0; i < float1d.length; i++) {
            fData[i] = float1d[i] * bscale  + bzero;
        }
        return fData;
    }

    public void freeResources() {
        float1d = null;
        header = null;
        hist= null;
        hdu= null;
    }

    public void writeSimpleFitsFile(OutputStream stream) throws FitsException, IOException{
        createNewFits().write(new DataOutputStream(stream));
    }

    public void clearHDU() { this.hdu= null; }

    public Fits createNewFits() throws FitsException, IOException {
        if (hdu==null) {
            throw new IOException("HDU has been clear, this FitsRead no longer supports re-writing the FITS file");
        }
        Fits outputFits = new Fits();
        outputFits.addHDU(hdu);
        return outputFits;
    }

}