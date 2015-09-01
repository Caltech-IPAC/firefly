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

import java.io.*;
import java.util.*;


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
 * 8/25/15 Fixed the HDU bug at spliting HDU
 *
 */
public class FitsRead implements Serializable {
    //class variable
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();
    static {
        FitsFactory.setAllowTerminalJunk(true);
        FitsFactory.setUseHierarch(true);
    }

    //private variables
    private int planeNumber;
    private int extension_number;
    private float[] float1d;
   // static private float[] physicalData;
    private Fits fits;
    private ImageHeader imageHeader;
    private Header header;
    private BasicHDU hdu;
    private int imageScaleFactor = 1;
    private int indexInFile = -1;  // -1 unknown, >=0 index in file
    private String srcDesc = null;
    private  short[] masks=null;


    private static ArrayList<Integer> SUPPORTED_BIT_PIXS = new ArrayList<Integer>(Arrays.asList(8, 16, 32, -32, -64));

    /**
     * a private constructor for image Fits file
     *
     * @param fits
     * @param imageHdu
     * @throws FitsException
     */
    private FitsRead(Fits fits, ImageHDU imageHdu) throws FitsException {

        //assign some instant variables
        this.fits = fits;
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
        //get the data and store into float array

        float1d = getImageHDUDataInFloatArray(imageHdu);

        //mask in the Fits file, each FitsRead in the Fits file has the same mask data
        masks =getMasksInFits(fits);

    }


    /**
     * This constructor may not be needed it.
     * @param fits
     * @param imageHdu
     * @throws FitsException
     */
     private FitsRead(Fits fits, ImageHDU imageHdu, int maskExtension) throws FitsException {

        //assign some instant variables
        this.fits = fits;
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
        //get the data and store into float array
        float1d = getImageHDUDataInFloatArray(imageHdu);
        masks = getMasksInFits(maskExtension);


     }

    private short[] getMasksInFits(int maskExtension) throws FitsException {

        //get all the Header Data Unit from the fits file
        BasicHDU[] HDUs = fits.read();
        short[] sMask=   (short[]) ArrayFuncs.flatten(HDUs[maskExtension].getData().getData());
        return getMasks(HDUs[maskExtension].getHeader(), sMask);

    }


    private short[] getMasksInFits(Fits fits) throws FitsException {

        //get all the Header Data Unit from the fits file
        BasicHDU[] HDUs = fits.read();

        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU)) {
                continue;   //ignor non-image extensions
            }
            Header header =  HDUs[j].getHeader();
            if (header == null) {
                throw new FitsException("Missing header in FITS file");
            }
             else  if ( header.containsKey("EXTTYPE")  &&  header.getStringValue("EXTTYPE").equalsIgnoreCase("mask") ){
                short[] mArray=(short[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(HDUs[j].getData().getData(), Short.TYPE));
                return getMasks(header, mArray);
           }
        }
        return null;


     }
/*
    //The mask data is the phyiscal data, therefore, it needs to be converted.
     private static short[] getMasksInFits(ArrayList<BasicHDU> HDUList) throws FitsException {
         for (int i=0; i<HDUList.size(); i++){
             Header header =  HDUList.get(i).getHeader();
             if (header == null) {
                 throw new FitsException("Missing header in FITS file");
             }
             if ( header.containsKey("EXTTYPE")  &&  header.getStringValue("EXTTYPE").equalsIgnoreCase("mask") ) {
                 Object data = HDUList.get(i).getData().getData();
                 short[] sData =  (short[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(HDUList.get(i).getData().getData(), Short.TYPE));
                 masks = new short[sData.length];
                 for (int ii = 0; i < sData.length; i++) {
                     masks[i] = (short) (sData[i] * (short) maskImageHeader.bscale + (short) maskImageHeader.bzero);
                 }

                 return   (short[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(HDUList.get(i).getData().getData(), Short.TYPE));
             }

         }
         return null;

     }
*/
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
        // boolean hasExtension = HDUs.length>1? true:false;

        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }


        ArrayList<BasicHDU> HDUList = getHDUList(HDUs);

        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        FitsRead[] fitsReadAry = new FitsRead[HDUList.size()];
        for (int i = 0; i < HDUList.size(); i++) {
            fitsReadAry[i] = new FitsRead(fits, (ImageHDU) HDUList.get(i));
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
            fitsReadAry[i] = new FitsRead(fits, (ImageHDU) HDUList.get(i));
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
     * @param aFitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, flipped image
     */

    public static FitsRead createFitsReadFlipLR(FitsRead aFitsReader)
            throws FitsException, GeomException {
        FlipLR flipLr = new FlipLR();
        return (flipLr.do_flip(aFitsReader));
    }

    /**
     * Rotate an image so that Equatorial North is up in the new image
     *
     * @param fitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUp(FitsRead fitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(fitsReader, 0.0, CoordinateSys.EQ_J2000));
    }

    /**
     * Rotate an image so that Galactic North is up in the new image
     *
     * @param aFitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUpGalactic(FitsRead aFitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(aFitsReader, 0.0, CoordinateSys.GALACTIC));
    }

    /**
     * Rotate an image by a specified amount
     *
     * @param fitsReader    FitsReadLZ object for the input image
     * @param rotationAngle number of degrees to rotate the image counter-clockwise
     * @return FitsReadLZ object for the new, rotated image
     */
    public static FitsRead createFitsReadRotated(FitsRead fitsReader, double rotationAngle)
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
            double positionAngle = -VisUtil.getPositionAngle(worldPt1.getX(),
                    worldPt1.getY(), worldPt2.getX(), worldPt2.getY());

            positionAngle += rotationAngle;
            return (createFitsReadPositionAngle(fitsReader, positionAngle, CoordinateSys.EQ_J2000));
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
            aFitsRead.imageScaleFactor = imageScaleFactor;

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
     * @param fitsRead      FitsReadLZ object for the input image
     * @param positionAngle desired position angle in degrees
     * @param coordinateSys desired coordinate system for output image
     * @return FitsReadLZ object for the new, rotated image
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
        float[][][] data32 = (float[][][]) ArrayFuncs.convertArray(hdu.getData().getData(), Float.TYPE);

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
        //new_header.deleteKey("BSCALE");
        //new_header.deleteKey("BZERO");

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


        //convert data to float if the bitpix is not 32
        float[] float1d =
                (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageHDU.getData().getData(), Float.TYPE));

        /* pixels are upside down - reverse them in y */
        if (imageHeader.cdelt2 < 0) float1d = reversePixData(float1d);


        return float1d;
    }



    public synchronized void doStretch(RangeValues rangeValues,
                                       byte[] pixelData,
                                       boolean mapBlankToZero,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine){




        Histogram hist= getHistogram();


        double slow = getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = getShigh(rangeValues, float1d, imageHeader, hist);
        if (SUTDebug.isDebug()) {
            printInfo(slow, shigh, imageHeader.bitpix, rangeValues);
        }

        byte blank_pixel_value = mapBlankToZero ? 0 : (byte) 255;

        //byte pixelData[] = passedPixelData;


        stretchPixels(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1, hist,
                blank_pixel_value, float1d,  pixelData,  rangeValues,slow,shigh);

        System.out.println("debug:check pixelData here");

    }

    /**
     * Add the mask layer to the existing image
     * @param rangeValues
     * @param
     * @param mapBlankToZero
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param lsstMasks
     */
    public synchronized void doStretch(RangeValues rangeValues,
                                       byte[] pixelData,
                                       boolean mapBlankToZero,
                                       int startPixel,
                                       int lastPixel,
                                       int startLine,
                                       int lastLine, ImageMask[] lsstMasks){




        Histogram hist= getHistogram();


        double slow = getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = getShigh(rangeValues, float1d, imageHeader, hist);
        if (SUTDebug.isDebug()) {
            printInfo(slow, shigh, imageHeader.bitpix, rangeValues);
        }

        byte blank_pixel_value = mapBlankToZero ? 0 : (byte) 255;

        //byte pixelData[] = passedPixelData;

        int[] pixelhist = new int[256];




        stretchPixels(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1,
                blank_pixel_value, float1d, masks, pixelData, pixelhist,  lsstMasks);






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


        ImageMask[] bitOffsetOrderedMasks = sortLSSTMaskArrayInOrder(lsstMasks);

        ImageMask combinedMask = ImageMask.combine(lsstMasks);


        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //orignal pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {   // stretch each pixel


                    if (combinedMask.isSet( masks[index])) {


                        for (int i = 0; i < lsstMasks.length; i++) {
                            if (bitOffsetOrderedMasks[i].isSet(masks[index])) {
                                pixeldata[pixelCount] = (byte) bitOffsetOrderedMasks[i].getValue();
                                break;
                            }
                        }
                    }
                    else {

                        pixeldata[pixelCount]= (byte) 256; //transparent;


                    }

                    pixelhist[pixeldata[pixelCount] & 0xff]++;
                }
                pixelCount++;

            }
        }


    }
    private static ImageMask[] sortLSSTMaskArrayInOrder(ImageMask[] lsstMasks){

        Map<Integer, ImageMask> unsortedMap= new HashMap<Integer, ImageMask>();
        for (int i=0;i<lsstMasks.length; i++){
            unsortedMap.put(new Integer((int) lsstMasks[i].getValue()), lsstMasks[i]);
        }

        Map<Integer, ImageMask> treeMap = new TreeMap<Integer, ImageMask>(unsortedMap);
        return treeMap.values().toArray(new ImageMask[0]);
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
            case RangeValues.MAXMIN:
                slow = hist.get_pct(0.0, false);
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
            case RangeValues.MAXMIN:
                shigh = hist.get_pct(100.0, true);
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
                                      Histogram hist,
                                      byte blank_pixel_value,
                                      float[] float1dArray,
                                      byte[] pixeldata,
                                      RangeValues rangeValues,
                                      double slow,
                                      double shigh) {


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

        /*
         * This loop will go through all the pixels and assign them new values based on the
         * stretch algorithm
         */

        double dr= rangeValues.getDrValue();
        double gamma=rangeValues.getGammaValue();
        double bp= rangeValues.getBPValue();
        double wp=rangeValues.getWPValue();
        int pixelCount = 0;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //orignal pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {   // stretch each pixel
                    if (rangeValues.getStretchAlgorithm() ==RangeValues.STRETCH_LINEAR) {

                        double dRunval = ((float1dArray[index] - slow ) * 254 / sdiff);
                        pixeldata[pixelCount] = getLinearStrectchedPixelValue(dRunval);

                    } else if (rangeValues.getStretchAlgorithm()==RangeValues.STRETCH_ASINH) {
                        //test simple asinh stretch
                     pixeldata[pixelCount] = (byte) getASinhStretchedPixelValue(float1dArray[index], dr, slow, shigh, bp, wp);
                    }
                    else if (rangeValues.getStretchAlgorithm()==RangeValues.STRETCH_POWERLAW_GAMMA) {

                        pixeldata[pixelCount] = (byte) getPowerLawGammaStretchedPixelValue(float1dArray[index], gamma, slow, shigh);
                    }
                    else {

                        pixeldata[pixelCount] = (byte) getNoneLinerStretchedPixelValue(float1dArray[index],  dtbl, deltasav);
                    }
                    pixeldata[pixelCount] = rangeValues.computeBiasAndContrast(pixeldata[pixelCount]);
                }
                pixelCount++;

            }
        }

    }

    private static double getPowerLawGammaStretchedPixelValue(double x, double gamma, double zp, double mp){

        double  rd =  x-zp;
        double  nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
        double pixValue = 255*nsd;

        return pixValue;

    }
    private static double asinh(double x) {

        return Math.log(x + Math.sqrt(x * x + 1));

    }
    private static double getASinhStretchedPixelValue(double x, double dr, double zp, double mp, double bp, double wp)  {



        double rd = dr * (x - zp) / mp; //in the range of 0 - dr (when zp=0.0)
        double nsd = asinh(rd) / asinh(mp - zp); // in the range 0-1 when zp=0.0
        double pixValue = 255 * (nsd - bp) / wp;

         return pixValue;


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
                start_line, last_line, naxis1, hist,
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


    public Fits getFits() {
        return (fits);
    }

    public BasicHDU getHDU() {
        return hdu;
    }

    public Header getHeader() {
        return header;
    }

    public ImageHeader getImageHeader() {
        return (imageHeader);
    }



    public static RangeValues getDefaultRangeValues() {
        return (RangeValues) DEFAULT_RANGE_VALUE.clone();

    }

    public int getImageScaleFactor() {
        return (imageScaleFactor);
    }

    /**
     * get a description of the fits file that created this fits read
     * This can be any text.
     *
     * @return the description of the fits file
     */
    public String getSourceDec() {
        return (srcDesc);
    }

    /**
     * Set a description of the fits file that created this fits read.
     * This can be any text.
     *
     * @param s the description
     */
    public void setSourceDesc(String s) {
        srcDesc = s;
    }

    Histogram getHistogram() {


        double bscale = imageHeader.bscale;
        double bzero = imageHeader.bzero;

        return new Histogram(float1d, (imageHeader.datamin - bzero) / bscale,
                (imageHeader.datamax - bzero) / bscale);


    }

    /**
     * return the index of where this fits data was i a fits file.  If a -1
     * is returned the we do not know or many this FitsReadLZ was created with
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

    /**
     * get the masks data in the Fits file, null if there is no mask data
     * @return
     */
    public short[] getMasks(){
        return masks;
    }
    /**
     * reeturn the physical data value
     * @param mask
     * @return
     */
   private  short[] getMasks(Header header, short[] mask){

        short[] sMask = new short[mask.length];

        double bscale = header.getDoubleValue("BSCALE", 1.0);
        double bzero = header.getDoubleValue("BZERO", 0.0);
        for (int i = 0; i < mask.length; i++) {
            sMask[i] = (short) (mask[i] * bscale + bzero);
        }
        return sMask;
    }
    /**
     * This method seemed not being used.
     * Takes an open FITS file and returns a 2-dim float array of the pixels
     * Works for all 5 FITS data types and all FITS array dimensionality
     * Observes BLANK value for integer data
     * Applies BSCALE and BZERO
     *
     * @param imageHDU ImageHDU for the open FITS file
     * @return 2-dim float array of pixels
     *
     * This method returns the physical pixel values in the given imageHDU as a two dimensional array
     */
    
    //LZ 6/16/15 this method seems not used.  I comment it out.
    /**
    public static float[][] getDataFloat(ImageHDU imageHDU)
            throws FitsException {
        Header header = imageHDU.getHeader();
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");
        double bscale = header.getDoubleValue("BSCALE", 1.0);
        double bzero = header.getDoubleValue("BZERO", 0.0);

        float[][] floatData = new float[naxis2][naxis1];
        float[] float32 =
                (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageHDU.getData().getData(), Float.TYPE));

        int i = 0;
        for (int line = 0; line < naxis2; line++) {
          for (int sample = 0; sample < naxis1; sample++) {
                floatData[line][sample] = (float)
                        (float32[i] * bscale + bzero);
                i++;
            }
        }

        return floatData;
    }
    */


    public void freeResources() {
        float1d = null;
        fits = null;
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
            //System.out.println("RBH card.toString() = " + card.toString());
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

        ImageHDU newHDU =  new ImageHDU(header, getImageData(header, float1d));
        Fits outputFits = new Fits();
        outputFits.addHDU(newHDU);
        return outputFits;
    }


    private ImageData getImageData(Header header, float[] float1d){
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");
        int dims2[] = new int[]{naxis1, naxis2};
        float [][]  data =  (float[][]) ArrayFuncs.curl(float1d,dims2);
        ImageData imageData= new ImageData(data);
        return imageData;
    }
    public static void writeFitsFile(OutputStream stream, FitsRead[] fitsReadAry, Fits refFits) throws FitsException, IOException{
        Fits output_fits = new Fits();
        for(FitsRead fr : fitsReadAry) {
            BasicHDU one_image_hdu = refFits.getHDU(0);
            Header header = one_image_hdu.getHeader();
            //Data data = one_image_hdu.getData();
            //ImageHDU image_hdu = new ImageHDU(header, data);

            ImageHDU imageHDU = new ImageHDU(header,  fr.getImageData(header, fr.float1d));
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
