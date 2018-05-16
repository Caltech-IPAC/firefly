package edu.caltech.ipac.visualize.plot;
import edu.caltech.ipac.astro.FITSTableReader;
import edu.caltech.ipac.astro.IpacTableToFITS;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import nom.tam.util.ArrayFuncs;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;


/**
 * Lijun Zhang
 * 02/06/15
 * Refactor this class
 * Change all data type to float no matter what the bitpix value is
 *
 * 04/08/15
 * reactor doStretch and related methods
 *
 * 6/12/15
 * Refactored stretch related methods and renamed all methods using the camelCase name
 * Add the asinh and Power Law Gamma algorithms
 * Cleaned up some unused methods
 * 7/29/15
 *  Add a new method to createFitsReadArray(Fits, BasicHDU) for Herschel data
 *  Add a new method to createFitsImageCube(Fits)
 *  Add a new doStretch and stretchPixel for testing mask plot
 *  A method " public int[] getScreenHistogram()" is never used.  It is removed and so the pixelhist variables.
 *  The pixelHist is removed from the input argument list in stretchPixel method
 *
 *  *
 * 8/25/15 Fixed the HDU bug at splitting HDU
 * 9/11/15
 *   Modified the stretchPixel for mask plot
 *   Removed unused methods (commented out)
 *
 * 9/24/15
 *  remove the mask testing codes since the mask is done in the mask branch.
 * 9/26/16
 *  DM-4127
 *
 *  3/1/17
 *  DM-9581
 *
 *  5/2/18
 *  IRSA-1498: Firefly needs to be able to read and understand FITS image with 3rd alternative coordinate/axis
 *  The implementation is based on the reference paper
 *      :https://www.aanda.org/articles/aa/pdf/2006/05/aa3818-05.pdf
 *  1. Read the FITS header to find out if the third axis exists.
 *  2. If the third axis exits, the CTYPEka keywords are processed to find the algorithm used, the quantity is sampled
 *     and expressed.
 *  3. Based on the information in step 2, either wavelength or frequency or velocity is calculated based on the
 *     formula in the paper.
 *  Implementation considerations:
 *  1. In order to save processing time at the run time when each wavelength calculation is processed, for TAB lookup,
 *     the binaryTable is found when the FitsRead array is created.  Thus, there is no need to get it again when
 *     getWaveLength is called.
 *  2. Extra CreateFisReadArray method is added to handle the cases the TAB look up table is in separated FITS file
 *  3. If the FITS image files have multiple image extensions, each image should have one corresponding look up
 *     table if the TAB is used.
 *
 *
 */
public class FitsRead implements Serializable {
    //class variable
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();
    private final static String  IMAGETYPE = "2D-IMAGE";
    private static final ArrayList<Integer> SUPPORTED_BIT_PIXS = new ArrayList<>(Arrays.asList(8, 16, 32, -32, -64));

    //private variables
    private int planeNumber;
    private int extension_number;
    private BasicHDU hdu;
    private float[] float1d;
    private ImageHeader imageHeader;
    private Header header;
    private int indexInFile = -1;  // -1 unknown, >=0 index in file
    private final Histogram hist;
    private String fitsType;
    private  double defBetaValue= Double.NaN;
    private BinaryTableHDU tableHDU=null;
    private boolean tileCompress = false;


    /**
     * a private constructor for image Fits file
     *
     *
     * @param imageHdu
     * @throws FitsException
     */
    private FitsRead( ImageHDU imageHdu, boolean clearHdu) throws FitsException {

        hdu = imageHdu;
        header = imageHdu.getHeader();

        planeNumber = header.getIntValue("SPOT_PL", 0);
        extension_number = header.getIntValue("SPOT_EXT", -1);

        FitsReadUtil.checkHeader(header, planeNumber,extension_number);
        long HDUOffset = FitsReadUtil.getHDUOffset(imageHdu,extension_number);
        imageHeader = new ImageHeader(header, HDUOffset, planeNumber);


        if (!SUPPORTED_BIT_PIXS.contains(new Integer(imageHeader.bitpix))) {
            System.out.println("Unimplemented bitpix = " + imageHeader.bitpix);
        }

        //convert the data to float to do all the calculations
        float1d = FitsReadUtil.getImageHDUDataInFloatArray(imageHdu, imageHeader);

        if (clearHdu) {
            hdu= null;
        }
        hist= computeHistogram();

        String type = header.getStringValue("CTYPE3");
        fitsType = type!=null? type:IMAGETYPE;

    }

    /**
     *
     * 05/24/18
     * This is newly added private constructor to process the spectra wavelength.
     * When the FitsRead array is created by an input FITS file which contains a
     * a TAB lookup Table, this constructor will used.
     * @param imageHdu
     * @param tableHDU
     * @param clearHdu
     * @throws FitsException
     */
    private FitsRead( ImageHDU imageHdu, BinaryTableHDU tableHDU, boolean clearHdu) throws FitsException {

        this(imageHdu, clearHdu);
        this.tableHDU = tableHDU;
    }


    public double getDefaultBeta() {
        if (Double.isNaN(this.defBetaValue)) {
            this.defBetaValue= FitsReadUtil.computeSigma(float1d, imageHeader);
        }
        return this.defBetaValue;
    }


    /**
     *
     * Create the FitsRead array.  According to TAB lookup table's value to decide which FitsRead
     * constructor is used.
     *
     * @param imageHDUs
     * @param tableHDUs
     * @param isCompressed
     * @param clearHdu
     * @return
     * @throws FitsException
     */
    private static FitsRead[] getFitsReadArray(BasicHDU[]  imageHDUs, BasicHDU[] tableHDUs,
                                               boolean isCompressed,boolean clearHdu)throws FitsException{

        FitsRead[] fitsReadAry = new FitsRead[imageHDUs.length];
        for (int i = 0; i < imageHDUs.length; i++) {
            BinaryTableHDU bHdu=null;
            String extName = imageHDUs[i].getHeader().getStringValue("PS3_0");
            if (extName!=null) {
                if (!FitsReadUtil.isLookupTableValid(tableHDUs, extName)) {
                    Logger.info("The wavelength by table look up can not be calculated " +
                            "because the look up table is invalid.  The FITS should only have one Table extension with" +
                            "'EXTNAME' specified");
                }
                bHdu = FitsReadUtil.getBinaryTableHdu(tableHDUs, extName);
            }
            fitsReadAry[i] = bHdu!=null? new FitsRead((ImageHDU) imageHDUs[i], bHdu, clearHdu):
                    new FitsRead((ImageHDU) imageHDUs[i], clearHdu);
            fitsReadAry[i].indexInFile = i;
            fitsReadAry[i].tileCompress = isCompressed;
        }
        return fitsReadAry;
    }


    /**
     * This method is added to handle the case that the spectra lookup table is in the separated FITS file
     * @param imageFits
     * @param tableFits
     * @return
     * @throws FitsException
     * @throws IOException
     */
    public static FitsRead[] createFitsReadArray(Fits imageFits, Fits tableFits) throws FitsException, IOException {

        //get all the Header Data Unit from the image FITS file
        BasicHDU[] HDUs = imageFits.read();

        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }

        boolean isCompressed = FitsReadUtil.anyCompressedImage(HDUs);
        ArrayList<BasicHDU> HDUList = FitsReadUtil.getImageHDUList(HDUs, SUPPORTED_BIT_PIXS);

        if (HDUList.size() == 0) { //The FITS file does not have any Image
            throw new FitsException("No image headers in FITS file");
        }

        //Get the TAB look up Binary Table HDU
        BasicHDU[] tblHDUs = tableFits.read();
        if (tblHDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }

        return getFitsReadArray(HDUList.toArray(new BasicHDU[0]), tblHDUs, isCompressed, false);
    }

    /**
     *
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits) throws FitsException {

        return createFitsReadArray(fits,false);

    }


       /**
     * read a fits with extensions or cube data to create a list of the FistRead object
     *
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits, boolean clearHdu)
            throws FitsException {

        //get all the Header Data Unit from the fits file
        BasicHDU[] HDUs = fits.read();

        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }

        return createFitsReadArray(HDUs,clearHdu);

    }

    /**
     *
     * @param HDUs
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray( BasicHDU[] HDUs, boolean clearHdu)
            throws FitsException {


        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }

        boolean isCompressed = FitsReadUtil.anyCompressedImage(HDUs);

        ArrayList<BasicHDU> HDUList = FitsReadUtil.getImageHDUList(HDUs, SUPPORTED_BIT_PIXS);
        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        return getFitsReadArray(HDUList.toArray(new BasicHDU[0]), HDUs, isCompressed, clearHdu);

    }


    /**
     * This method will return a FitsImageCube object
     * This method is added in parallel as createFitsReadArray
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsImageCube createFitsImageCube(Fits fits)throws FitsException {

        return new FitsImageCube(fits);
    }
    /**
     * Flip an image left to right so that pixels read backwards
     *
     * @param aFitsReader FitsRead object for the input image
     * @return FitsRead object for the new, flipped image
     */

    public static FitsRead createFitsReadFlipLR(FitsRead aFitsReader)
            throws FitsException, GeomException {

        return new FlipXY(aFitsReader,"yAxis").doFlip();


    }

    /**
     * Rotate an image so that Equatorial North is up in the new image
     *
     * @param fitsReader FitsRead object for the input image
     * @return FitsRead object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUp(FitsRead fitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(fitsReader, 0.0, CoordinateSys.EQ_J2000));
    }

    /**
     * Rotate an image so that Galactic North is up in the new image
     *
     * @param aFitsReader FitsRead object for the input image
     * @return FitsRead object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUpGalactic(FitsRead aFitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(aFitsReader, 0.0, CoordinateSys.GALACTIC));
    }

    /**
     * Rotate an image by a specified amount
     *
     * @param fitsReader    FitsRead object for the input image
     * @param rotationAngle number of degrees to rotate the image counter-clockwise
     * @param fromNorth if true that the rotation angle is from the north
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadRotated(FitsRead fitsReader, double rotationAngle, boolean fromNorth)
            throws FitsException, IOException, GeomException {

        ImageHeader imageHeader = fitsReader.getImageHeader();

        CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection projection = imageHeader.createProjection(inCoordinateSys);

        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        try {
            WorldPt worldPt1 = projection.getWorldCoords(centerX, centerY - 1);
            WorldPt worldPt2 = projection.getWorldCoords(centerX, centerY);
            double positionAngle = VisUtil.getPositionAngle(worldPt1.getX(),
                    worldPt1.getY(), worldPt2.getX(), worldPt2.getY());
            if (fromNorth) {
                long angleToRotate= Math.round((180+ rotationAngle) % 360);
                if (angleToRotate==Math.round(positionAngle)) {
                    return fitsReader;
                }
                else {
                    return createFitsReadPositionAngle(fitsReader, -angleToRotate, inCoordinateSys);
                }
            }
            else {
                return createFitsReadPositionAngle(fitsReader, -positionAngle+ rotationAngle, inCoordinateSys);
            }
        } catch (ProjectionException pe) {
            if (SUTDebug.isDebug()) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }

    }

    public static FitsRead createFitsReadWithGeom(FitsRead aFitsRead,
                                                  FitsRead aRefFitsRead,
                                                  boolean aDoscale) throws
            FitsException,
            IOException,
            GeomException {

        //update the input aFitsRead only if the aRefFitsRead is not null
        if (aRefFitsRead != null) {
            ImageHeader refHeader = aRefFitsRead.getImageHeader();
            Geom geom = new Geom();
            //geom.override_naxis1=0;
            geom.n_override_naxis1 = aDoscale;

            ImageHeader imageHeader = geom.open_in(aFitsRead);
            double primCdelt1 = Math.abs(imageHeader.cdelt1);
            double refCdelt1 = Math.abs(refHeader.cdelt1);
            int imageScaleFactor = 1;
            boolean shouldScale = 2 * refCdelt1 < primCdelt1;
            if (aDoscale && shouldScale) {
                imageScaleFactor = (int) (primCdelt1 / refCdelt1);
                geom.override_cdelt1 = refHeader.cdelt1 * imageScaleFactor;
                geom.n_override_cdelt1 = true;
                geom.override_cdelt2 = refHeader.cdelt2 * imageScaleFactor;
                geom.n_override_cdelt2 = true;
                if (refHeader.using_cd) {
                    geom.override_CD1_1 = refHeader.cd1_1 * imageScaleFactor;
                    geom.override_CD1_2 = refHeader.cd1_2 * imageScaleFactor;
                    geom.override_CD2_1 = refHeader.cd2_1 * imageScaleFactor;
                    geom.override_CD2_2 = refHeader.cd2_2 * imageScaleFactor;
                    geom.n_override_CDmatrix = true;
                }

                geom.crpix1_base = refHeader.crpix1;
                geom.crpix2_base = refHeader.crpix2;
                geom.imageScaleFactor = imageScaleFactor;
                geom.need_crpix_adjusted = true;
                if (SUTDebug.isDebug()) {
                    System.out.println(
                            "RBH ready for do_geom:  imageScaleFactor = "
                                    + imageScaleFactor + "  geom = " + geom);
                }
            }

            //make a copy of the reference  fits
            Fits modFits = geom.do_geom(aRefFitsRead);

            FitsRead[] fitsReadArray = createFitsReadArray(modFits);
            aFitsRead = fitsReadArray[0];

        }
        return aFitsRead;
    }


    /**
     * Rotate an image so that North is at the specified position angle in the new image
     *
     * @param fitsRead      FitsRead object for the input image
     * @param positionAngle desired position angle in degrees
     * @param coordinateSys desired coordinate system for output image
     * @return FitsRead object for the new, rotated image
     */
    public static FitsRead createFitsReadPositionAngle(FitsRead fitsRead, double positionAngle,
                                                       CoordinateSys coordinateSys)
            throws FitsException, IOException, GeomException {

        Geom geom = new Geom();
        Header refHeader = FitsReadUtil.getRefHeader(geom, fitsRead, positionAngle, coordinateSys);

        //create a ImageHDU with the null data
        ImageHDU refHDU = new ImageHDU(refHeader, null);
        Fits refFits = new Fits();
        refFits.addHDU(refHDU);

        refFits = geom.do_geom(refFits);  // throws GeomException
        FitsRead[] fitsReadArray = createFitsReadArray(refFits);
        fitsRead = fitsReadArray[0];
        return fitsRead;
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





        double slow = FitsReadUtil.getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = FitsReadUtil.getShigh(rangeValues, float1d, imageHeader, hist);

        byte blank_pixel_value = mapBlankToZero ? 0 : (byte) 255;


        FitsReadUtil.stretchPixels(startPixel, lastPixel, startLine, lastLine,imageHeader.naxis1, imageHeader, hist,
                blank_pixel_value, float1d, pixelData, rangeValues, slow, shigh);


    }


    /**
     * Add the mask layer to the existing image
     * @param
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param lsstMasks
     */
    public synchronized void doStretchMask(
                                       byte[] pixelData,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine, ImageMask[] lsstMasks)  {

        byte blank_pixel_value = (byte) 255;

        int[] pixelhist = new int[256];

        //covert the raw mask data to real mask : rawMask * imageHeader.bscale + imageHeader.bzero;
        float[] fMasks = getDataFloat();

        //convert to its original type
        short[] masks= (short[]) ArrayFuncs.convertArray(fMasks, Short.TYPE, true);


        FitsReadUtil.stretchPixels(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1,
                        blank_pixel_value, float1d, masks, pixelData, pixelhist, lsstMasks);


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
        return FitsReadUtil.getFlux(raw_dn, imageHeader);

    }

    /**
     * This method will calculate wavelength at any given image point.  The calculation is based on
     * the paper "Representations of spectral coordinates in FITS", by E. W. Greisen1,M.R.Calabretta2, F.G.Valdes3,
     * and S. L. Allen.
     *
     * Current, the Linear, Log and Non-linear algorithms based on the WCS parameters/keywords in the headers
     * are implemented.  The TAB look up is also implemented.  For algorithm details, please read this paper.
     * The TAB algorithm is very complicated.  Since we only have one coordinate (the wavelength), our case is
     * less complicated.
     *
     * @param ipt: image point
     * @return
     * @throws PixelValueException
     * @throws FitsException
     * @throws IOException
     * @throws DataFormatException
     */
     public double getWaveLength(ImagePt ipt) throws PixelValueException, FitsException, IOException, DataFormatException {


        double[] pixelCoords= FitsReadUtil.getPixelCoords(ipt, imageHeader, header);

        int N = header.getIntValue("WCSAXES", -1);
        if (N==-1) {
            N = header.getIntValue("NAXIS", -1);
            if (N==-1){
                throw new  DataFormatException("Dimension value is not avaiable, Please set either NAXIS or WCSAXES in the header ");
            }
         }

         double lamda = FitsReadUtil.calculateWavelength(header, tableHDU, pixelCoords, fitsType);
         //vacuum wavelength
         if (fitsType.startsWith("WAVE")){
            return lamda;
         }
         else if( fitsType.startsWith("AWAV")) { //calculate the air wavelength and then convert to vacuum wavelength
             double n_lamda = 1 + Math.pow(10, -6) * ( 287.6155 + 1.62887/Math.pow(lamda, 2) + 0.01360/Math.pow(lamda, 4) );
             return lamda/n_lamda;
         }
         else {
             throw new  DataFormatException("This "+ fitsType + " is not supported");
         }
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

    public int getProjectionType() {
        return getImageHeader().maptype;
    }


    public boolean isSameProjection(FitsRead secondFitsread) {
        boolean result = false;

        ImageHeader H1 = getImageHeader();
        ImageHeader H2 = secondFitsread.getImageHeader();

        if (H1.maptype == H2.maptype) {
            if (H1.maptype == Projection.PLATE) {
                result = FitsReadUtil.checkPlate(H1, H2);
            } else {
                result = FitsReadUtil.checkOther(H1, H2);
            }
        }
        return result;
    }

    public boolean hasHdu() { return hdu!=null;}

    public BasicHDU getHDU() {
        if (hdu==null) {
            throw new IllegalArgumentException("HDU has been cleared, there is not longer access to it.");
        }
        return hdu;
    }

    public static Class getDataType(int bitPix){
        Class type=null;
        switch (bitPix){
            case 8:
                type = Byte.TYPE;
                break;
            case 16:
                type = Short.TYPE;
                break;
            case 32:
                type =Integer.TYPE;
                break;
            case 64:
                type =Long.TYPE;
                break;
            case -32:
                type = Float.TYPE;
                break;
            case -64:
                type = Double.TYPE;
                break;
        }
        return type;
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

        return FitsReadUtil.getHistColors( hist, rangeValues, float1d, imageHeader);
    }

    public Header getHeader() throws HeaderCardException {
        return cloneHeader(header);
    }

    public String getExtType() {
        HeaderCard hc= header.findCard("EXTTYPE");
        return (hc!=null) ? hc.getValue() : null;
    }


    public ImageHeader getImageHeader() {
        return imageHeader;
    }


    public static RangeValues getDefaultRangeValues() {
        return (RangeValues) DEFAULT_RANGE_VALUE.clone();

    }

    public int getImageScaleFactor() {
        return 1;
    }


    private Histogram  computeHistogram() {


        double bscale = imageHeader.bscale;
        double bzero = imageHeader.bzero;

        return new Histogram(float1d, (imageHeader.datamin - bzero) / bscale,
                (imageHeader.datamax - bzero) / bscale);

    }
    Histogram getHistogram() {
     return hist;
    }


    /**
     * return the index of where this fits data was i a fits file.  If a -1
     * is returned the we do not know or many this FitsRead was created with
     * geom.  Otherwise if a number >= 0 other is return then that is the
     * location in the fits file
     *
     * @return index of where this fits data was in file
     */
    public int getIndexInFile() {
        return indexInFile;
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
     * -1:  this was the only image, the primary one - there were no extensions
     * 0:  this was the primary image (not an extension) in a FITS file with
     * extensions
     * 1:  this was the first extension in the FITS file
     * 2:  this was the second extension in the FITS file
     * etc.
     */
    public int getExtensionNumber() {
        return extension_number;
    }

    public boolean isTileCompress() { return tileCompress; }
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

        float[] fData = new float[float1d.length];

        for (int i = 0; i < float1d.length; i++) {
            fData[i] = float1d[i] * (float) imageHeader.bscale + (float) imageHeader.bzero;
        }
        return fData;
    }



    public void freeResources() {
        float1d = null;
        imageHeader = null;
        header = null;


    }

    static Header cloneHeaderFrom(Header header) throws HeaderCardException {
         return FitsReadUtil.cloneHeaderFrom(header);
    }

    static Header cloneHeader(Header header) throws HeaderCardException {
        Header clonedHeader =cloneHeaderFrom(header);

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
         FitsRead[] frArray = FitsRead.createFitsReadArray(fits);


        if (frArray[0].fitsType.startsWith("WAVE") || frArray[0].fitsType.startsWith("AWAV")){
            int naxis1 = frArray[0].header.getIntValue("NAXIS1");
            int naxis2 = frArray[0].header.getIntValue("NAXIS2");
            ArrayList<Double> ret = new ArrayList<>();
            double[][] lamda= new double[naxis1][naxis2];

            ImagePt imagePt;
            for (int i=0; i<naxis1; i++){
                for (int j=0; j<naxis2; j++){
                    imagePt = new ImagePt(i, j);
                    lamda[i][j] = frArray[0].getWaveLength(imagePt);
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




       /*
        String inFitsName = args[0];
        String outFitsName = args[1];
        Fits fits = new Fits(inFitsName);
        FitsImageCube fic = FitsRead.createFitsImageCube(fits);
        Object[] keys = fic.getMapKeys();
        FitsRead fitsRead0 = fic.getFitsReadMap().get(keys[0])[1];
        FileOutputStream fo = new java.io.FileOutputStream(outFitsName+"fitsRead1ReadAsImageCube.fits");
        fitsRead0.writeSimpleFitsFile(fo);
        fo.close();

        FitsRead[] fry = FitsRead.createFitsReadArray(fits);
        fo  = new java.io.FileOutputStream(outFitsName+"fitsRead1ReadAsFitsRead.fits");//"f-32AsFitsRead.fits");//
        fry[1].writeSimpleFitsFile(fo);
        fo.close();

        Fits newFits = new Fits(outFitsName+"fitsRead1ReadAsFitsRead.fits");
        BasicHDU[] hdus = newFits.read();
*/

    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
