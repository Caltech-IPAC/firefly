/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableToFITS;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;


/**
 *
 *
 */
public class FitsRead implements Serializable {
    private static final ArrayList<Integer> SUPPORTED_BIT_PIXS = new ArrayList<>(Arrays.asList(8, 16, 32, -32, -64));
    //class variable
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();

    //private variables
    private int planeNumber;
    private int hduNumber;
    private BasicHDU hdu;
    private float[] float1d;
    private ImageHeader imageHeader;
    private Header header;
    private Histogram hist;
    private  double defBetaValue= Double.NaN;
    private final boolean tileCompress;


    private BinaryTableHDU tableHDU=null; //todo - don't define tableHDU in FitsRead

    /**
     * Cachable class made for holding FITS data.
     *
     *
     * @param imageHdu
     * @throws FitsException
     */
    FitsRead( BasicHDU imageHdu, boolean clearHdu) throws FitsException {

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


        header = hdu.getHeader();
        planeNumber = header.getIntValue("SPOT_PL", 0);
        hduNumber = header.getIntValue("SPOT_EXT", 0);
        long HDUOffset= header.getIntValue("SPOT_OFF", 0);

        imageHeader = new ImageHeader(header, HDUOffset, planeNumber);
        if (!SUPPORTED_BIT_PIXS.contains(imageHeader.bitpix)) Logger.warn("Unimplemented bitpix = " + imageHeader.bitpix);


        //convert the data to float to do all the calculations
        float1d = FitsReadUtil.getImageHDUDataInFloatArray(hdu, imageHeader);

        if (clearHdu) hdu= null;

        hist= computeHistogram();
    }

    /**
     * todo - remove this constructor
     * 05/24/18
     * This is newly added package private constructor to process the spectra wavelength.
     * When the FitsRead array is created by an input FITS file which contains a
     * a TAB lookup Table, this constructor will used.
     * @param imageHdu
     * @param tableHDU
     * @param clearHdu
     * @throws FitsException
     */
    FitsRead( BasicHDU imageHdu, BinaryTableHDU tableHDU, boolean clearHdu) throws FitsException {

        this(imageHdu, clearHdu);
        this.tableHDU = tableHDU;
    }

    public boolean isTileCompress() { return tileCompress; }
    public int getNaxis1() { return this.imageHeader.naxis1; }
    public int getNaxis2() { return this.imageHeader.naxis2; }
    public double getBscale() { return this.imageHeader.bscale; }
    public double getBzero() { return this.imageHeader.bzero; }
    public double getCdelt1() { return this.imageHeader.cdelt1; }
    public double getCdelt2() { return this.imageHeader.cdelt2; }
    public CoordinateSys determineCoordSys() { return this.imageHeader.determineCoordSys(); }
    public int getProjectionType() { return imageHeader.maptype; }

    public String getExtType() {
        HeaderCard hc= header.findCard("EXTTYPE");
        return (hc!=null) ? hc.getValue() : "";
    }

    public BinaryTableHDU getTableHDU() { return tableHDU; } //todo - remove this methdo




    public double getDefaultBeta() {
        if (Double.isNaN(this.defBetaValue)) {
            this.defBetaValue= ImageStretch.computeSigma(float1d, imageHeader);
        }
        return this.defBetaValue;
    }


    public static RangeValues getDefaultFutureStretch() {
        return DEFAULT_RANGE_VALUE;
    }

    public static void setDefaultFutureStretch(RangeValues defaultRangeValues) {
        DEFAULT_RANGE_VALUE = defaultRangeValues;
    }



    public synchronized void doStretch(RangeValues rangeValues,
                                       byte[] pixelData,
                                       boolean mapBlankToZero,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine){





        double slow = ImageStretch.getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = ImageStretch.getShigh(rangeValues, float1d, imageHeader, hist);

        byte blank_pixel_value = mapBlankToZero ? 0 : (byte) 255;


        ImageStretch.stretchPixels(startPixel, lastPixel, startLine, lastLine,imageHeader.naxis1, imageHeader, hist,
                blank_pixel_value, float1d, pixelData, rangeValues, slow, shigh);


    }


    public short[] getDataAsMask() {
        float[] fMasks = getDataFloat();
        //convert to its original type
        short[] maskData= (short[]) ArrayFuncs.convertArray(fMasks, Short.TYPE, true);
        return maskData;

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

        //covert the raw mask data to real mask : rawMask * imageHeader.bscale + imageHeader.bzero;
        float[] fMasks = getDataFloat();

        //convert to its original type
//        int[] masks= (int[]) ArrayFuncs.convertArray(fMasks, Integer.TYPE, true);


        ImageStretch.stretchPixelsForMask(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1,
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

        if ((xint < 0) || (xint >= imageHeader.naxis1) ||
                (yint < 0) || (yint >= imageHeader.naxis2)) {
            throw new PixelValueException("location not on the image");
        }

        int index = yint * imageHeader.naxis1 + xint;

        double raw_dn = float1d[index];
        return ImageStretch.getFlux(raw_dn, imageHeader);

    }

    public String getFluxUnits() {
        String retval = imageHeader.bunit;
        if (imageHeader.bunit.startsWith("HITS")) {
            retval = "frames";
        }
        if (imageHeader.origin.startsWith(ImageHeader.PALOMAR_ID)) {
            retval = "mag";
        }
        return (retval);
    }

    public boolean hasHdu() { return hdu!=null;}

    public BasicHDU getHDU() {
        if (hdu==null) throw new IllegalArgumentException("HDU has been cleared, there is not longer access to it.");
        return hdu;
    }

    public static Class getDataType(int bitPix){
        switch (bitPix){
            case 8: return Byte.TYPE;
            case 16: return Short.TYPE;
            case 32: return Integer.TYPE;
            case 64: return Long.TYPE;
            case -32: return  Float.TYPE;
            case -64: return  Double.TYPE;
            default: return null;
        }
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

        return ImageStretch.getHistColors( hist, rangeValues, float1d, imageHeader);
    }

    public Header getHeader() { return header; }



    public static RangeValues getDefaultRangeValues() {
        return (RangeValues) DEFAULT_RANGE_VALUE.clone();

    }

    public int getImageScaleFactor() {
        return 1;
    }


    private Histogram  computeHistogram() {
        return new Histogram(float1d, (imageHeader.datamin - imageHeader.bzero) / imageHeader.bscale,
                (imageHeader.datamax - imageHeader.bzero) / imageHeader.bscale);

    }

    public Histogram getHistogram() {
        return hist;
    }



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

        if (imageHeader.bscale==1.0 && imageHeader.bzero==0) return float1d;

        float[] fData = new float[float1d.length];

        for (int i = 0; i < float1d.length; i++) {
            fData[i] = float1d[i] * (float) imageHeader.bscale + (float)imageHeader.bzero;
        }
        return fData;
    }



    public void freeResources() {
        float1d = null;
        imageHeader = null;
        header = null;
        hist= null;
    }

    static Header cloneHeader(Header header) throws HeaderCardException {
        Header clonedHeader =FitsReadUtil.cloneHeaderFrom(header);

        clonedHeader.resetOriginalSize();
        return clonedHeader;
    }

    public void writeSimpleFitsFile(OutputStream stream) throws FitsException, IOException{
        createNewFits().write(new DataOutputStream(stream));
    }

    public void clearHDU() { this.hdu= null; }

    public Fits createNewFits() throws FitsException, IOException {

        Fits outputFits = new Fits();
        if (hdu==null) {
            throw new IOException("HDU has been clear, this FitsRead no longer supports re-writing the FITS file");
        }
        outputFits.addHDU(hdu);
        return outputFits;
    }

    public static void writeFitsFile(OutputStream stream, FitsRead[] fitsReadAry, Fits refFits) throws FitsException, IOException{
        Fits output_fits = new Fits();
        for(FitsRead fr : fitsReadAry) {
             BasicHDU  refHdu = refFits.getHDU(0);
             ImageHDU imageHDU = new ImageHDU(refHdu.getHeader(),  FitsReadUtil.getImageData(refHdu, fr.float1d) );
            output_fits.addHDU(imageHDU);
        }
        output_fits.write(new DataOutputStream(stream));
    }

    static void usage()
    {
        System.out.println("usage java edu.caltech.ipac.visulaize.FitsRead <fits_filename> <ipac_filename>");
        System.exit(1);
    }



    /**
     * Test the FitsImaegCube
     * @param args
     * @throws FitsException
     * @throws IOException
     */
    public static void main(String[] args) throws FitsException, IOException, PixelValueException, DataFormatException {
        if (args.length != 2) {
            usage();
        }


        //test Table look up, the table is in extension 1, and the FITS file name is passed
        //FitsRead[] frArray = FitsRead.createFitsReadArray(args[0], 1);

        //test linear, log, non-linear, Table
         Fits fits = new Fits(args[0]);
         FitsRead[] frArray = FitsReadFactory.createFitsReadArray(fits);


        String type = frArray[0].getHeader().getStringValue("CTYPE3");
        String fitsType = type!=null ? type : "";

        if (fitsType.startsWith("WAVE") || fitsType.startsWith("AWAV")){
            int naxis1 = frArray[0].imageHeader.naxis1;
            int naxis2 = frArray[0].imageHeader.naxis2;
            ArrayList<Double> ret = new ArrayList<>();
            double[][] lamda= new double[naxis1][naxis2];

            ImagePt imagePt;

            Wavelength wl= new Wavelength(frArray[0].getHeader(),frArray[0].tableHDU);
            for (int i=0; i<naxis1; i++){
                for (int j=0; j<naxis2; j++){
                    imagePt = new ImagePt(i, j);
                    lamda[i][j] = wl.getWaveLength(imagePt);
                    ret.add(lamda[i][j]);
                }
            }



            //The following is creating a result FITS for test harness
            double [] data = new double [ret.size()];
            for (int i=0; i<ret.size(); i++){
                data[i]= ret.get(i).doubleValue();
            }

            //store to a binary fits table
            DataType[] dtypes ={ new DataType ("WaveLength", Double.class)};
            DataGroup dg = new DataGroup("result", dtypes);
            for (int i=0; i<data.length; i++){
                DataObject row = new DataObject(dg);
                //the data is float, but the IpacTableToFits does not take float
                row.setDataElement( dtypes[0],data[i]);//new Double(data[i]));//new Float(data[i]));
                dg.add(row);
            }

            IpacTableToFITS ipac_to_fits = new IpacTableToFITS();
            Fits retFits =  ipac_to_fits.convertToFITS(dg);


            retFits.write(new File(args[1]));

            //Test it if reads correctly
            String[] dataCols = {"WaveLength"};
            DataGroup table = FITSTableReader.convertFitsToDataGroup(
                    args[1],
                    dataCols,
                    null,
                    FITSTableReader.EXPAND_BEST_FIT, 1);
            System.out.println("done");

        }

    }

}

