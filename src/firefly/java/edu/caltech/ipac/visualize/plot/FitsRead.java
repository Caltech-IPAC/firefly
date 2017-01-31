package edu.caltech.ipac.visualize.plot;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;


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
 */
public class FitsRead implements Serializable {
    //class variable
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();
    static {
        FitsFactory.setAllowTerminalJunk(true);
        FitsFactory.setUseHierarch(true);
    }

    //private variables
    private final int planeNumber;
    private final int extension_number;
    private final BasicHDU hdu;
    private float[] float1d;
    private ImageHeader imageHeader;
    private Header header;
    private int indexInFile = -1;  // -1 unknown, >=0 index in file
    private Histogram hist;
    private  double betaValue;

    private static ArrayList<Integer> SUPPORTED_BIT_PIXS = new ArrayList<Integer>(Arrays.asList(8, 16, 32, -32, -64));

    /**
     * a private constructor for image Fits file
     *
     *
     * @param imageHdu
     * @throws FitsException
     */
    private FitsRead( ImageHDU imageHdu) throws FitsException {


        hdu = imageHdu;
        header = imageHdu.getHeader();

        planeNumber = header.getIntValue("SPOT_PL", 0);
        extension_number = header.getIntValue("SPOT_EXT", -1);
        checkHeader();
        long HDUOffset = getHDUOffset(imageHdu);
        imageHeader = new ImageHeader(header, HDUOffset, planeNumber);


        if (!SUPPORTED_BIT_PIXS.contains(new Integer(imageHeader.bitpix))) {
            System.out.println("Unimplemented bitpix = " + imageHeader.bitpix);
        }

        //convert the data to float to do all the calculations
        float1d = getImageHDUDataInFloatArray(imageHdu);

        hist= computeHistogram();
        /* The error in asinh algorithm is
         *  V(mu) = [ a * sigma^2)/(4b^2+f^2)] where f is a flux
         *  When f=0, the error reaches it maximum, if we choose beta = 2b = sigma,
         *  the error:
         *  V(mu) = a *simga^2/beta^2 = a
         *  Thus, we use sigma as a default beta value
         */
        betaValue = computeSigma(float1d, imageHeader);
    }

    public double getBeta() {return betaValue;}

    /**
     * read a fits with extensions or cube data to create a list of the FistRead object
     *
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits)
            throws FitsException {

        //get all the Header Data Unit from the fits file
        BasicHDU[] HDUs = fits.read();


        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }


        ArrayList<BasicHDU> HDUList = getHDUList(HDUs);

        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        FitsRead[] fitsReadAry = new FitsRead[HDUList.size()];
        for (int i = 0; i < HDUList.size(); i++) {
            fitsReadAry[i] = new FitsRead((ImageHDU) HDUList.get(i));
            fitsReadAry[i].indexInFile = i;
        }

        return fitsReadAry;
    }


    /**
     * This method is used by FitsImageCube only
     * @param fits
     * @param hdu
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits, BasicHDU hdu)
            throws FitsException {


        if (hdu == null || hdu.getData().getSize()==0) {
            // Error: file doesn't seem to have any HDUs!
            return null;
        }

        BasicHDU[] HDUs={hdu};
        ArrayList<BasicHDU> HDUList = getHDUList(HDUs);

        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        FitsRead[] fitsReadAry = new FitsRead[HDUList.size()];
        for (int i = 0; i < HDUList.size(); i++) {
            fitsReadAry[i] = new FitsRead( (ImageHDU) HDUList.get(i));
            fitsReadAry[i].indexInFile = i;

        }


        return fitsReadAry;
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
                    return createFitsReadPositionAngle(fitsReader, -angleToRotate, CoordinateSys.EQ_J2000);
                }
            }
            else {
                return createFitsReadPositionAngle(fitsReader, -positionAngle+ rotationAngle, CoordinateSys.EQ_J2000);
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
     * Get the world point location
     *
     * @param imageHeader
     * @param aCoordinateSys
     * @return
     * @throws FitsException
     */
    private static WorldPt getWorldPt(ImageHeader imageHeader, CoordinateSys aCoordinateSys) throws FitsException {
        CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection proj = imageHeader.createProjection(inCoordinateSys);


        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        WorldPt worldPt;
        try {
            worldPt = proj.getWorldCoords(centerX - 1, centerY - 1);

        } catch (ProjectionException pe) {
            if (SUTDebug.isDebug()) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }


        if (!aCoordinateSys.equals(imageHeader.getCoordSys())) {
            worldPt = Plot.convert(worldPt, aCoordinateSys);
        }
        return worldPt;
    }

    /**
     * The input refHeader will be modified and new keys/values are added
     *
     * @param imageHeader
     * @param refHeader
     * @param aPositionAngle
     * @param aCoordinateSys
     * @throws FitsException
     */
    private static void updateRefHeader(ImageHeader imageHeader, Header refHeader,
                                        double aPositionAngle, CoordinateSys aCoordinateSys)
            throws FitsException {


        refHeader.addValue("CDELT1", -Math.abs(imageHeader.cdelt1), "");
        refHeader.addValue("CDELT2", Math.abs(imageHeader.cdelt2), "");
        refHeader.addValue("CRPIX1", imageHeader.naxis1 / 2, "");
        refHeader.addValue("CRPIX2", imageHeader.naxis2 / 2, "");
        refHeader.addValue("CROTA2", aPositionAngle, "");
        if (aCoordinateSys.equals(CoordinateSys.EQ_J2000)) {
            refHeader.addValue("CTYPE1", "RA---TAN", "");
            refHeader.addValue("CTYPE2", "DEC--TAN", "");
            refHeader.addValue("EQUINOX", 2000.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.EQ_B1950)) {
            refHeader.addValue("CTYPE1", "RA---TAN", "");
            refHeader.addValue("CTYPE2", "DEC--TAN", "");
            refHeader.addValue("EQUINOX", 1950.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.ECL_J2000)) {
            refHeader.addValue("CTYPE1", "ELON-TAN", "");
            refHeader.addValue("CTYPE2", "ELAT-TAN", "");
            refHeader.addValue("EQUINOX", 2000.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.ECL_B1950)) {
            refHeader.addValue("CTYPE1", "ELON-TAN", "");
            refHeader.addValue("CTYPE2", "ELAT-TAN", "");
            refHeader.addValue("EQUINOX", 1950.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.GALACTIC)) {
            refHeader.addValue("CTYPE1", "GLON-TAN", "");
            refHeader.addValue("CTYPE2", "GLAT-TAN", "");
        } else {
            throw new FitsException("Could not rotate image.\n -  unrecognized coordinate system");
        }
    }

    /**
     * a new reference header is created
     *
     * @param geom
     * @param fitsRead
     * @param positionAngle
     * @param coordinateSys
     * @return
     * @throws FitsException
     * @throws IOException
     * @throws GeomException
     */
    private static Header getRefHeader(Geom geom, FitsRead fitsRead, double positionAngle,
                                       CoordinateSys coordinateSys)
            throws FitsException, IOException, GeomException {

        ImageHeader imageHeader = geom.open_in(fitsRead);  // throws GeomException
       /* new try - create a Fits with CDELTs and CROTA2, discarding */
       /* CD matrix, PLATE projection stuff, and SIP corrections */
        Header refHeader = new Header();
        refHeader.setSimple(true);
        refHeader.setNaxes(2);
        /* values for cropped.fits */
        refHeader.setBitpix(16);  // ignored - geom sets it to -32
        refHeader.setNaxis(1, imageHeader.naxis1);
        refHeader.setNaxis(2, imageHeader.naxis2);
        geom.n_override_naxis1 = true;  // make geom recalculate NAXISn
    /*
        pixel at center of object
	    18398  DN at RA = 60.208423  Dec = -89.889959
	    pixel one up
	    18398  DN at RA = 59.995226  Dec = -89.889724
	    (a distance of 0.028349 arcmin or 0.00047248 degrees)
	*/

        //get the world point worldPt based on the imageHeader and aCoordinatesSys
        WorldPt worldPt = getWorldPt(imageHeader, coordinateSys);

        refHeader.addValue("CRVAL1", worldPt.getX(), "");
        refHeader.addValue("CRVAL2", worldPt.getY(), "");

        updateRefHeader(imageHeader, refHeader, positionAngle, coordinateSys);

        return refHeader;
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
        Header refHeader = getRefHeader(geom, fitsRead, positionAngle, coordinateSys);

        //create a ImageHDU with the null data
        ImageHDU refHDU = new ImageHDU(refHeader, null);
        Fits refFits = new Fits();
        refFits.addHDU(refHDU);

        refFits = geom.do_geom(refFits);  // throws GeomException
        FitsRead[] fitsReadArray = createFitsReadArray(refFits);
        fitsRead = fitsReadArray[0];
        return fitsRead;
    }

    private static boolean isImageGood(Header aHeader) {

        int naxis = aHeader.getIntValue("NAXIS", -1);
        boolean goodImage = true;
        if (naxis == 0) {
            goodImage = false;
        } else {
            for (int i = 1; i <= naxis; i++) {
                int naxisValue = aHeader.getIntValue("NAXIS" + i, -1);

                if (naxisValue == 0) {
                    goodImage = false;
                    break;
                }
            }
        }
        return goodImage;
    }


    private static void updateHeader(Header header, int pos, long hduOffset)

            throws FitsException {
        header.addLine(new HeaderCard(
                "SPOT_EXT", pos, "EXTENSION NUMBER (IN SPOT)"));

        header.addLine(new HeaderCard(
                "SPOT_OFF", hduOffset,
                "EXTENSION OFFSET (IN SPOT)"));
        header.resetOriginalSize();
    }


    private static ArrayList<BasicHDU> getHDUList(BasicHDU[] HDUs) throws FitsException {
        ArrayList<BasicHDU> HDUList = new ArrayList<BasicHDU>();

        boolean hasExtension = HDUs.length > 1 ? true : false;
        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU)) {
                continue;   //ignor non-image extensions
            }
            //process image HDU
            Header header = HDUs[j].getHeader();
            if (header == null)
                throw new FitsException("Missing header in FITS file");



            int naxis = header.getIntValue("NAXIS", -1);
            boolean goodImage = isImageGood(header);

            if (goodImage) {
                if (hasExtension) { // update this hdu by adding keywords/values
                    updateHeader(header, j, HDUs[j].getFileOffset());
                }

                int naxis3 = header.getIntValue("NAXIS3", -1);
                if ((naxis > 2) && (naxis3 > 1)) { //it is a cube data
                    if (SUTDebug.isDebug())
                        System.out.println("GOT A FITS CUBE");
                    BasicHDU[] splitHDUs = splitFitsCube( (ImageHDU) HDUs[j]);
                    /* for each plane of cube */
                    for (int jj = 0; jj < splitHDUs.length; jj++) {
                        HDUList.add(splitHDUs[jj]);
                    }
                } else {
                    HDUList.add(HDUs[j]);
                }
            }

            //when the header is added to the new fits file, the card number could be increased if the header is a primary
            //header.resetOriginalSize();

        } //end j loop
        return HDUList;
    }

    private static BasicHDU[] splitFitsCube(ImageHDU hdu)
            throws FitsException {

        Header header = hdu.getHeader();
        int bitpix = header.getIntValue("BITPIX", -1);

        if (!SUPPORTED_BIT_PIXS.contains(new Integer(bitpix))) {
            System.out.println("Unimplemented bitpix = " + bitpix);
        }


        int naxis3 = header.getIntValue("NAXIS3", 0);
        float[][][] data32 = (float[][][]) ArrayFuncs.convertArray(hdu.getData().getData(), Float.TYPE, true);

        BasicHDU[] hduList = new BasicHDU[naxis3];
        for (int i = 0; i < naxis3; i++) {
            hduList[i] = makeHDU(hdu,data32[i] );
            hdu.addValue("SPOT_PL", i + 1, "PLANE OF FITS CUBE (IN SPOT)");
            hdu.getHeader().resetOriginalSize();
         }

        return hduList;
    }



    public static RangeValues getDefaultFutureStretch() {
        return DEFAULT_RANGE_VALUE;
    }

    public static void setDefaultFutureStretch(RangeValues defaultRangeValues) {
        DEFAULT_RANGE_VALUE = defaultRangeValues;
    }

    /**
     * Creates a new ImageHDU given the original HDU and the new array of pixels
     * The new header part reflects the 2-dim float data
     * The new data part contains the new pixels
     * Sets NAXISn according to the actual dimensions of pixels[][], which is
     * not necessarily the dimensions of the original image
     *
     * @param hdu    ImageHDU for the open FITS file
     * @param pixels The 2-dim float array of new pixels
     * @return The new ImageHDU
     */
    private static ImageHDU makeHDU(ImageHDU hdu, float[][] pixels)
            throws FitsException {
        Header header = hdu.getHeader();

        // first clone the header
        Cursor iter = header.iterator();
        String cards[] = new String[header.getNumberOfCards()];
        HeaderCard card;
        int i = 0;
        while (iter.hasNext()) {
            card = (HeaderCard) iter.next();
            cards[i] = card.toString();
            i++;
        }
        Header newHeader = new Header(cards);

        newHeader.deleteKey("BITPIX");
        newHeader.setBitpix(-32);
        newHeader.deleteKey("NAXIS");
        newHeader.setNaxes(2);
        newHeader.deleteKey("NAXIS1");
        newHeader.setNaxis(1, pixels[0].length);
        newHeader.deleteKey("NAXIS2");
        newHeader.setNaxis(2, pixels.length);

        newHeader.deleteKey("DATAMAX");
        newHeader.deleteKey("DATAMIN");
        newHeader.deleteKey("NAXIS3");
        newHeader.deleteKey("NAXIS4");
        newHeader.deleteKey("BLANK");

        ImageData new_image_data = new ImageData(pixels);
        hdu = new ImageHDU(newHeader, new_image_data);
        return hdu;
    }


    private long getHDUOffset(ImageHDU image_hdu) {
        long HDU_offset;
        if (extension_number == -1) {
            HDU_offset = image_hdu.getFileOffset();
        } else {
            HDU_offset = header.getIntValue("SPOT_OFF", 0);
        }

        if (HDU_offset < 0) HDU_offset = 0;
        return HDU_offset;

    }

    private void checkHeader() throws FitsException {


        // now get SPOT planeNumber from FITS cube (zero if not from a cube)
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_PL: " + planeNumber);

        // now get SPOT extension_number from FITS header
        // -1 if the image had no extensions
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_EXT: " + extension_number);

        if (header == null)
            throw new FitsException("FITS file appears corrupt");


    }

    private float[] reversePixData(float[] float1d) {

        int naxis1 = imageHeader.naxis1;
        int naxis2 = imageHeader.naxis2;
        if (imageHeader.cdelt2 < 0) {
            /* pixels are upside down - reverse them in y */
            float[] temp = new float[float1d.length];
            int index_src = 0;
            for (int y = 0; y < naxis2; y++) {

                int indexDest = (naxis2 - y - 1) * naxis1;
                for (int x = 0; x < naxis1; x++) {
                    temp[indexDest++] = float1d[index_src++];
                }
            }
            float1d = temp;
            imageHeader.cdelt2 = -imageHeader.cdelt2;
            imageHeader.crpix2 =
                    imageHeader.naxis2 - imageHeader.crpix2 + 1;

        }
        return float1d;
    }

    private float[] getImageHDUDataInFloatArray(ImageHDU imageHDU) throws FitsException {

        float[]  float1d =
                        (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageHDU.getData().getData(), Float.TYPE, true));

        /* pixels are upside down - reverse them in y */
        if (imageHeader.cdelt2 < 0) float1d = reversePixData(float1d);


        return float1d;
    }

    private static double[] getMinMaxData(float[] float1d){
        double min=Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i=0; i<float1d.length; i++){
            if (float1d[i]<min){
                min=float1d[i];
            }
            if (float1d[i]>max){
                max = float1d[i];
            }
        }
        double[] ret = {min, max};
        return ret;
    }

    public synchronized void doStretch(RangeValues rangeValues,
                                       byte[] pixelData,
                                       boolean mapBlankToZero,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine){





        double slow = getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = getShigh(rangeValues, float1d, imageHeader, hist);

        byte blank_pixel_value = mapBlankToZero ? 0 : (byte) 255;


        stretchPixels(startPixel, lastPixel, startLine, lastLine,imageHeader.naxis1, imageHeader, hist,
                blank_pixel_value, float1d, pixelData, rangeValues, slow, shigh);


    }


    /**
     * A pixel is a cell or small rectangle which stores the information the computer can handle. A discrete pixels make the map.
     * Each pixel store a value which represents the color of the map.
     * Byte image is the pixel having a value in the range of [0, 255].  One byte has 8 bit.  The stretch algorithm is able to convert
     * some invisible pixel value to become recognizable.
     * There are several stretch algorithm: liner, log, log-log etc.  Using those technique to calculate new pixel values.  For example:
     * Suppose you have a certain image in which the values range from 55 to 103. When this map is stretched linearly to output range 0 to
     * 255: the minimum input value 55 is brought to output value 0, and maximum input value 103 is brought to output value 255, and all
     * other values in between change accordingly (using the same formula). As 0 is by default displayed in black, and 255 in white, the
     * contrast will be better when the image is displayed.
     *
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param blank_pixel_value
     */
    private static void stretchPixels(int startPixel,
                                      int lastPixel,
                                      int startLine,
                                      int lastLine,
                                      int naxis1,
                                      ImageHeader imageHeader,
                                      Histogram hist,
                                      byte blank_pixel_value,
                                      float[] float1dArray,
                                      byte[] pixeldata,
                                      RangeValues rangeValues,
                                      double slow,
                                      double shigh) {





        /*
         * This loop will go through all the pixels and assign them new values based on the
         * stretch algorithm
         */
        if (rangeValues.getStretchAlgorithm()==RangeValues.STRETCH_ASINH) {
            stretchPixelsUsingAsin( startPixel, lastPixel,startLine,lastLine, naxis1, imageHeader,
               float1dArray, pixeldata, rangeValues,slow,shigh);


        }
        else {
            stretchPixelsUsingOtherAlgorithms(startPixel, lastPixel, startLine, lastLine,imageHeader.naxis1, hist,
                    blank_pixel_value,float1dArray, pixeldata, rangeValues, slow, shigh);
        }
    }

    private static void stretchPixelsUsingOtherAlgorithms(int startPixel,
                                                         int lastPixel,
                                                         int startLine,
                                                         int lastLine,
                                                          int naxis1,
                                                         Histogram hist,
                                                         byte blank_pixel_value,
                                                         float[] float1dArray,
                                                         byte[] pixeldata,
                                                         RangeValues rangeValues,
                                                         double slow,
                                                         double shigh){

        double sdiff = slow == shigh ? 1.0 : shigh - slow;

        double[] dtbl = new double[256];
        if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LOG
                || rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LOGLOG) {
            dtbl = getLogDtbl(sdiff, slow, rangeValues);
        }
        else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) {
            dtbl = hist.getTblArray();
        }
        else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED){
            dtbl = getSquaredDbl(sdiff, slow, rangeValues);
        }
        else if( rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT) {
            dtbl = getSquaredDbl(sdiff, slow, rangeValues);
        }
        int deltasav = sdiff > 0 ? 64 : -64;

        double gamma=rangeValues.getGammaValue();
        int pixelCount = 0;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {   // stretch each pixel
                    if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LINEAR) {

                        double dRunval = ((float1dArray[index] - slow) * 254 / sdiff);
                        pixeldata[pixelCount] = getLinearStrectchedPixelValue(dRunval);

                    } else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_POWERLAW_GAMMA) {

                        pixeldata[pixelCount] = (byte) getPowerLawGammaStretchedPixelValue(float1dArray[index], gamma, slow, shigh);
                    } else {

                        pixeldata[pixelCount] = (byte) getNoneLinerStretchedPixelValue(float1dArray[index], dtbl, deltasav);
                    }
                    pixeldata[pixelCount] = rangeValues.computeBiasAndContrast(pixeldata[pixelCount]);
                }
                pixelCount++;

            }
        }

    }
    private static void stretchPixelsUsingAsin(int startPixel,
                                      int lastPixel,
                                      int startLine,
                                      int lastLine,
                                      int naxis1,
                                      ImageHeader imageHeader,
                                      float[] float1dArray,
                                      byte[] pixeldata,
                                      RangeValues rangeValues,
                                      double slow,
                                      double shigh){

        double beta = rangeValues.getBetaValue();
        // Here we use flux instead of data since the original paper is using flux. But I don't think it is matter.
        // flux = raw_dn * imageHeader.bscale + imageHeader.bzero, when bscale=1 and bzero=0, flux=raw_dn
        double maxFlux = getFlux(shigh, imageHeader);
        double minFlux = getFlux(slow, imageHeader);
        if (Double.isNaN(minFlux) || Double.isInfinite((minFlux))){
            double[] minMax=getMinMaxData(float1dArray);
            minFlux = getFlux(minMax[0],imageHeader);
        }

        if ( Double.isNaN(maxFlux) || Double.isInfinite((maxFlux)) ) {
            double[] minMax=getMinMaxData(float1dArray);
            minFlux = getFlux(minMax[1], imageHeader);
        }
        int pixelCount = 0;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;
            for (int index = start_index; index <= last_index; index++) {
                pixeldata[pixelCount] =(byte)  getASinhStretchedPixelValue(getFlux(float1dArray[index], imageHeader), maxFlux, minFlux, beta);
                pixelCount++;
            }
        }

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


        stretchPixels(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1,
                        blank_pixel_value, float1d, masks, pixelData, pixelhist, lsstMasks);


    }

    /**
     * add a new stretch method to do the mask plot
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param naxis1
     * @param blank_pixel_value
     * @param float1dArray
     * @param masks
     * @param pixeldata
     * @param pixelhist
     */
    private static void stretchPixels(int startPixel,
                                      int lastPixel,
                                      int startLine,
                                      int lastLine,
                                      int naxis1,
                                      byte blank_pixel_value,
                                      float[] float1dArray,
                                      short[] masks,
                                      byte[] pixeldata,
                                      int[] pixelhist,
                                      ImageMask[] lsstMasks) {


        int pixelCount = 0;
        ImageMask combinedMask = ImageMask.combineWithAnd(lsstMasks);  //mask=33, index=0 and 6 are set

        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {
                    /*
                     The IndexColorModel is designed in the way that each pixel[index] contains the color in
                     lsstMasks[index].  In pixel index0, it stores the lsstMasks[0]'s color. Thus, assign
                     pixelData[pixelCount]=index of the lsstMasks, this pixel is going to be plotted using the
                     color stored there.  The color model is indexed.  For 8 bit image, it has 256 maximum colors.
                     For detail, see the indexColorModel defined in ImageData.java.
                     */
                    if (combinedMask.isSet( masks[index])) {
                        for (int i = 0; i < lsstMasks.length; i++) {
                            if (lsstMasks[i].isSet(masks[index])) {
                                pixeldata[pixelCount] = (byte) i;
                                break;
                            }
                        }
                    }
                    else {

                        /*
                        The transparent color is stored at pixel[lsstMasks.length].  The pixelData[pixelCount]=(byte) lsstMasks.length,
                        this pixel will be transparent.
                         */
                        pixeldata[pixelCount]= (byte) lsstMasks.length;
                    }

                    pixelhist[pixeldata[pixelCount] & 0xff]++;
                }
                pixelCount++;

            }
        }


    }


    private static Zscale.ZscaleRetval getZscaleValue(float[] float1d, ImageHeader imageHeader, RangeValues rangeValues) {

        double contrast = rangeValues.getZscaleContrast();
        int optSize = rangeValues.getZscaleSamples();

        int lenStdline = rangeValues.getZscaleSamplesPerLine();

        Zscale.ZscaleRetval zscaleRetval = Zscale.cdl_zscale(float1d,
                imageHeader.naxis1, imageHeader.naxis2,
                imageHeader.bitpix, contrast / 100.0, optSize, lenStdline,
                imageHeader.blank_value );

        return zscaleRetval;
    }

       private static double getSlow(RangeValues rangeValues,  float[] float1d, ImageHeader imageHeader, Histogram hist) {
        double slow = 0.0;
        switch (rangeValues.getLowerWhich()) {
            case RangeValues.ABSOLUTE:
                slow = (rangeValues.getLowerValue() - imageHeader.bzero) /imageHeader.bscale;
                break;
            case RangeValues.PERCENTAGE:
                slow = hist.get_pct(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.SIGMA:
                slow = hist.get_sigma(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.ZSCALE:

                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, imageHeader, rangeValues);
                slow = zscale_retval.getZ1();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getLowerWhich()");
        }
        return slow;
    }
    private double getShigh(RangeValues rangeValues, float[] float1d, ImageHeader imageHeader, Histogram hist) {
        double shigh = 0.0;
        switch (rangeValues.getUpperWhich()) {
            case RangeValues.ABSOLUTE:
                shigh = (rangeValues.getUpperValue() - imageHeader.bzero) / imageHeader.bscale;
                break;
            case RangeValues.PERCENTAGE:
                shigh = hist.get_pct(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.SIGMA:
                shigh = hist.get_sigma(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.ZSCALE:
                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, imageHeader, rangeValues);
                shigh = zscale_retval.getZ2();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getUpperWhich()");
        }
        return shigh;
    }



    private void printInfo(double slow, double shigh, int bitpix, RangeValues rangeValues) {

        System.out.println("slow = " + slow + "    shigh = " + shigh +
                "   bitpix = " + bitpix);
        if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LINEAR)
            System.out.println("stretching STRETCH_LINEAR");
        else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LOG)
            System.out.println("stretching STRETCH_LOG");
        else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LOGLOG)
            System.out.println("stretching STRETCH_LOGLOG");
        else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_EQUAL)
            System.out.println("stretching STRETCH_EQUAL");
        else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_SQUARED)
            System.out.println("stretching STRETCH_SQUARED");

    }


    private static double getPowerLawGammaStretchedPixelValue(double x, double gamma, double zp, double mp){

        double  rd =  x-zp;
        double  nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
        double pixValue = 255*nsd;

        return pixValue;

    }

    /**
     * The asinh stretch algorithm is defined in the paper by Robert Lupton et al at "The Astronomical Journal 118: 1406-1410, 1999 Sept"
     * In the paper:
     *    magnitude = m_0-2.5log(b) - a* asinh (x/2b)  = mu_0  -a * asinh (x/2b), where mu_0 =m_0 - 2.5 * ln(b), a =2.5ln(e) = 1.08574,
     *    m_o=2.5log(flux_0), flux_0 is the flux of an object with magnitude 0.0.
     *    b is an arbitrary "softening" which determines the flux level as which the liner behavior is set in, and x is the flux,
     *
     *
     *   Since  mu_0 and a are constant, we can use just:
     *     mu =  asinh( flux/(2.0*b)) ) = asin(x/beta); where beta=2*b;
     * @param beta
     * @return
     */
    private static double  getASinhStretchedPixelValue(double flux, double maxFlux, double minFlux, double beta)  {

        /*
         Since the data range is from minFlux to maxFlux, we can shift the data to  the range [0 - (maxFlux-minFlux)].
         Thus,
                   flux_new = flux-minFlux,
                   minFlux_new = 0
                   maxFlux_new = maxFlux - minFlux
         min = asinh( abs(minFlux_new) -square(minFlux_new*minFlux_new+1) ) =0
         max = (max-min) = asinh( maxFlux_new/beta) = asinh( (maxFlux-minFlux)/beta)
         diff = max - min = max
         */
        double asinhMagnitude =  asinh( (flux-minFlux) / beta); //beta = 2*b

        //normalize to 0 - 255:  (nCorlor-1 )*(x - Min)/(Max - Min), 8 bit nCorlor=256
        //this formula is referred from IDL function: BYTSCL
        double diff =   asinh ( (maxFlux-minFlux)/beta );
        return  255* asinhMagnitude/ diff ;

    }


    private static double asinh(double x) {

        double y  = Math.log( Math.abs(x ) + Math.sqrt(x * x + 1));
        y = x<0? -y:y;

        return y;

    }

    /**
     * Calculate the mean flux value
     * @param data
     * @return
     */
     private static double getMean(double [] data ) {

           int size = data.length;
           float sum = 0.0f;
            for(double a : data)
                sum += a;
            return sum/size;
     }

    /**
     * Calculate variance and then standard deviation
     * @param data
     * @return
     */
      private static double getStdDev(double[] data) {

          int size = data.length;
          double mean = getMean(data);
          double temp = 0.0f;
          for(double a :data)
              temp += (mean - a) * (mean - a);

          return Math.sqrt(temp/size);
      }

    /**
     * Process the image fluxes to exclude the 0.0 and NaN and infinity values
     * @return
     */
      private static double[] getNoneZeroValidReadoutArray(float[] float1d, ImageHeader imageHeader){
        ArrayList<Double> list= new ArrayList<>();

        for (int i=0; i<float1d.length; i++){
            if (!Double.isNaN(float1d[i]) && !Double.isInfinite(float1d[i]) && float1d[1]!=0.0){
               list.add( getFlux(float1d[i], imageHeader ) );
            }
        }
        double[] arr = new double[list.size()];
        for(int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }


    /**
     * This sigma value is calculated using the whole image data.
     * sigma = SQRT [  ( sum (xi-x_average)^2 )/n
     *   where x_average is the mean value of the x array
     *   n is the total number of the element of x array
     * @return
     */
    private static double computeSigma(float[] float1d, ImageHeader imageHeader) {

        //get none zero and finite flux values
        double [] validData = getNoneZeroValidReadoutArray(float1d, imageHeader);
        /*
         When the index.length>25, the IDL atv uses sky to computer sigma. However the sky.pro uses many other
         numerical receipt methods such as value_local, fitting etc. Here we uses stddev instead.
       */
       if (validData.length>5 ){
           return getStdDev( validData);
        }
        else {
            return  1.0;

        }


    }


    private static int getNoneLinerStretchedPixelValue(double dRunVal,  double[] dtbl, int delta) {

        int pixval = 128;

        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;
        delta >>= 1;
        if (dtbl[pixval] >= dRunVal)
            pixval -= 1;
        return pixval;
    }


    private static byte getLinearStrectchedPixelValue(double dRenVal) {

        if (dRenVal < 0)
            return 0;
        else if (dRenVal > 254)
            return (byte) 254;
        else
            return (byte) dRenVal;
    }




    private static double[] getLogDtbl(double sdiff, double slow, RangeValues rangeValues) {

        double[] dtbl = new double[256];
        for (int j = 0; j < 255; ++j) {

            double atbl = Math.pow(10., j / 254.0);
            if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOGLOG) {
                atbl = Math.pow(10., (atbl - 1.0) / 9.0);
            }
            dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;


        }
        dtbl[255] = Double.MAX_VALUE;
        return dtbl;
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

          //calling stretch_pixel to calculate pixeldata, pixelhist
        byte[] pixeldata = new byte[4096];


        float[] hist_bin_values = new float[4096];
        for (int i = 0; i < 4096; i++) {
            hist_bin_values[i] = (float) hist.getDNfromBin(i);
        }

        double slow = getSlow(rangeValues, float1d, imageHeader,hist);
        double shigh = getShigh(rangeValues, float1d, imageHeader, hist);

        int start_pixel = 0;
        int last_pixel = 4095;
        int start_line = 0;
        int last_line = 0;
        int naxis1 = 1;
        byte blank_pixel_value = 0;

        stretchPixels(start_pixel, last_pixel,
                start_line, last_line, naxis1,imageHeader, hist,
                blank_pixel_value, hist_bin_values,
                pixeldata,  rangeValues,slow,shigh);

        return pixeldata;
    }


    /**
     * fill the 256 element table with values for a squared stretch
     *
     */
    private static double[] getSquaredDbl(double sdiff, double slow,RangeValues rangeValues) {
        double[] dtbl = new double[256];

        for (int j = 0; j < 255; ++j) {
            if ( rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED){
                dtbl[j] = Math.sqrt(sdiff * sdiff / 254 * j) + slow;
            }
            else {
               double dd = Math.sqrt(sdiff) / 254 * j;
               dtbl[j] = dd * dd + slow;
            }
                   }
        dtbl[255] = Double.MAX_VALUE;
        return dtbl;
    }



    /**
     * Get flux of pixel at given "ImagePt" coordinates
     * "ImagePt" coordinates have 0,0 lower left corner of lower left pixel
     * of THIS image
     *
     * @param ipt ImagePt coordinates
     */

    public  double getFlux(ImagePt ipt)
            throws PixelValueException {


        int xint = (int) Math.round(ipt.getX() - 0.5);
        int yint = (int) Math.round(ipt.getY() - 0.5);

        if ((xint < 0) || (xint >= imageHeader.naxis1) ||
                (yint < 0) || (yint >= imageHeader.naxis2)) {
            throw new PixelValueException("location not on the image");
        }

        int index = yint * imageHeader.naxis1 + xint;

        double raw_dn = float1d[index];
        return getFlux(raw_dn, imageHeader);

    }

    /**
     *
     * @param raw_dn
     * @return
     */
    private static double getFlux(double  raw_dn, ImageHeader imageHeader){



        if ((raw_dn == imageHeader.blank_value) || (Double.isNaN(raw_dn))) {
            //throw new PixelValueException("No flux available");
            return Double.NaN;

        }

        if (imageHeader.origin.startsWith("Palomar Transient Factory")) {
            return  -2.5 * .43429 * Math.log(raw_dn / imageHeader.exptime) +
                    imageHeader.imagezpt +
                    imageHeader.extinct * imageHeader.airmass;
			/* .43429 changes from natural log to common log */
        } else {
            return raw_dn * imageHeader.bscale + imageHeader.bzero;
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

    private boolean checkDistortion(ImageHeader H1, ImageHeader H2) {
        boolean result = false;
        if ((H1.ap_order == H2.ap_order) &&
                (H1.a_order == H2.a_order) &&
                (H1.bp_order == H2.bp_order) &&
                (H1.b_order == H2.b_order)) {
            result = true;
            for (int i = 0; i <= H1.a_order; i++) {
                for (int j = 0; j <= H1.a_order; j++) {
                    if ((i + j <= H1.a_order) && (i + j > 0)) {
                        if (H1.a[i][j] != H2.a[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.ap_order; i++) {
                for (int j = 0; j <= H1.ap_order; j++) {
                    if ((i + j <= H1.ap_order) && (i + j > 0)) {
                        if (H1.ap[i][j] != H2.ap[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.b_order; i++) {
                for (int j = 0; j <= H1.b_order; j++) {
                    if ((i + j <= H1.b_order) && (i + j > 0)) {
                        if (H1.b[i][j] != H2.b[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.bp_order; i++) {
                for (int j = 0; j <= H1.bp_order; j++) {
                    if ((i + j <= H1.bp_order) && (i + j > 0)) {
                        if (H1.bp[i][j] != H2.bp[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }
        return result;

    }

    private boolean checkOther(ImageHeader H1, ImageHeader H2) {
        boolean result = false;
        if (
                (H1.naxis1 == H2.naxis1) &&
                        (H1.naxis2 == H2.naxis2) &&
                        (H1.crpix1 == H2.crpix1) &&
                        (H1.crpix2 == H2.crpix2) &&
                        (H1.cdelt1 == H2.cdelt1) &&
                        (H1.cdelt2 == H2.cdelt2) &&
                        (H1.crval1 == H2.crval1) &&
                        (H1.crval2 == H2.crval2) &&
                        (H1.crota2 == H2.crota2) &&
                        (H1.getJsys() == H2.getJsys()) &&
                        (H1.file_equinox == H2.file_equinox)) {
                        /* OK so far - now check distortion correction */
            if (H1.map_distortion &&
                    H2.map_distortion) {
                result = checkDistortion(H1, H2);

            } else {
                result = true;
            }
        }
        return result;
    }

    private boolean checkPlate(ImageHeader H1, ImageHeader H2) {

        boolean result = false;
        if ((H1.plate_ra == H2.plate_ra) &&
                (H1.plate_dec == H2.plate_dec) &&
                (H1.x_pixel_offset == H2.x_pixel_offset) &&
                (H1.y_pixel_offset == H2.y_pixel_offset) &&
                (H1.plt_scale == H2.plt_scale) &&
                (H1.x_pixel_size == H2.x_pixel_size) &&
                (H1.y_pixel_size == H2.y_pixel_size)) {

            result = true;

              /* OK so far - now check coefficients */
            for (int i = 0; i < 6; i++) {
                if (H1.ppo_coeff[i] != H2.ppo_coeff[i]) {
                    result = false;
                    break;
                }
            }
            for (int i = 0; i < 20; i++) {
                if (H1.amd_x_coeff[i] != H2.amd_x_coeff[i]) {
                    result = false;
                    break;
                }
                if (H1.amd_y_coeff[i] != H2.amd_y_coeff[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public boolean isSameProjection(FitsRead secondFitsread) {
        boolean result = false;

        ImageHeader H1 = getImageHeader();
        ImageHeader H2 = secondFitsread.getImageHeader();

        if (H1.maptype == H2.maptype) {
            if (H1.maptype == Projection.PLATE) {
                result = checkPlate(H1, H2);
            } else {
                result = checkOther(H1, H2);
            }
        }
        return result;
    }


    public BasicHDU getHDU() {
        return hdu;
    }


    public Header getHeader() {
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
    static Header cloneHeader(Header header) {
        // first collect cards from old header
        Cursor iter = header.iterator();
        String cards[] = new String[header.getNumberOfCards()];
        int i = 0;
        while (iter.hasNext()) {
            HeaderCard card = (HeaderCard) iter.next();
            cards[i] = card.toString();
            i++;
        }

        Header clonedHeader = new Header(cards);

        clonedHeader.resetOriginalSize();
        return clonedHeader;
    }

    public void writeSimpleFitsFile(OutputStream stream) throws FitsException, IOException{
        createNewFits().write(new DataOutputStream(stream));
    }

    public Fits createNewFits() throws FitsException, IOException {

        Fits outputFits = new Fits();
        outputFits.addHDU(hdu);
        return outputFits;
    }

   static Class getDataType(int bitPix){
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
    private ImageData getImageData(BasicHDU refHdu, float[] float1d) throws FitsException {
        Header header = refHdu.getHeader();
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");
        int dims2[] = new int[]{naxis1, naxis2};
        float [][]  fdata =  (float[][]) ArrayFuncs.curl(float1d,dims2);
        Object data =ArrayFuncs.convertArray(fdata, getDataType(refHdu.getBitPix()), true);
        ImageData imageData= new ImageData(data);
        return imageData;
    }
    public static void writeFitsFile(OutputStream stream, FitsRead[] fitsReadAry, Fits refFits) throws FitsException, IOException{
        Fits output_fits = new Fits();
        for(FitsRead fr : fitsReadAry) {
             BasicHDU  refHdu = refFits.getHDU(0);
             ImageHDU imageHDU = new ImageHDU(refHdu.getHeader(),  fr.getImageData(refHdu, fr.float1d) );
            output_fits.addHDU(imageHDU);
        }
        output_fits.write(new DataOutputStream(stream));
    }

    static void usage()
    {
        System.out.println("usage java edu.caltech.ipac.astro.FITSTableReader <fits_filename> <ipac_filename>");
        System.exit(1);
    }


    /**
     * Test the FitsImaegCube
     * @param args
     * @throws FitsException
     * @throws IOException
     */
    public static void main(String[] args) throws FitsException, IOException {
        if (args.length != 2) {
            usage();
        }

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
