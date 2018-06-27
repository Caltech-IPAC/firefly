package edu.caltech.ipac.util;

import edu.caltech.ipac.visualize.plot.*;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import nom.tam.fits.ImageData;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

/**
 * LZ June 2018
 * Since the FitsRead class is heavily used, it is better to keep it shorter.  Thus, I moved many methods
 * to this Utility class, FitsReadUtil.java.   This class contains the public static methods which are
 * purely used by FitsRead.
 *
 *
 */
public class FitsReadUtil {

    /**
     * This method is to check if the BasicHDU array contains a BinaryTableHDU. The BinaryTableHDU's
     * header has to have a "EXTNAME" keyword and its value has to be the same as the primary header
     * under the key PS3_0.
     * @param extName - String, the value of PS3_0 keyword defined in the primary header
     * @param HDUs
     * @return
     */
    public static BinaryTableHDU getBinaryTableHdu(BasicHDU[] HDUs, String extName){


        //The extName in the TAB lookup table's header has to match the same name stored
        //in Image Header's PS3_0.
        for (int i = 0; i < HDUs.length; i++) {
            if (  HDUs[i] instanceof BinaryTableHDU  &&
                    extName.equalsIgnoreCase(HDUs[i].getHeader().getStringValue("EXTNAME")) ) {

                return (BinaryTableHDU) HDUs[i];
            }

        }

        return null;
    }


    public static boolean checkDistortion(ImageHeader H1, ImageHeader H2) {
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

    public static boolean checkOther(ImageHeader H1, ImageHeader H2) {
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

    public static boolean checkPlate(ImageHeader H1, ImageHeader H2) {

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

    public boolean isSameProjection(FitsRead secondFitsread, ImageHeader H1) {
        boolean result = false;

        //ImageHeader H1 = getImageHeader();
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


    public static ImageData getImageData(BasicHDU refHdu, float[] float1d) throws FitsException {
        Header header = refHdu.getHeader();
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");
        int dims2[] = new int[]{naxis1, naxis2};
        float [][]  fdata =  (float[][]) ArrayFuncs.curl(float1d,dims2);
        Object data =ArrayFuncs.convertArray(fdata,FitsRead.getDataType(refHdu.getBitPix()), true);
        ImageData imageData= new ImageData(data);
        return imageData;
    }


    public static long getHDUOffset(ImageHDU image_hdu, int extension_number) {
        long HDU_offset;

        Header header = image_hdu.getHeader();
        if (extension_number == -1) {
            HDU_offset = image_hdu.getFileOffset();
        } else {
            HDU_offset = header.getIntValue("SPOT_OFF", 0);
        }

        if (HDU_offset < 0) HDU_offset = 0;
        return HDU_offset;

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
    public static float[] getDataFloat(float[] float1d, ImageHeader imageHeader) {

        float[] fData = new float[float1d.length];

        for (int i = 0; i < float1d.length; i++) {
            fData[i] = float1d[i] * (float) imageHeader.bscale + (float) imageHeader.bzero;
        }
        return fData;
    }



    /** validation the FITS file
     If a FITS ﬁle contains multiple XTENSION HDUs (header-data units) with the speciﬁed EXTNAME, EXTLEVEL,and
     EXTVER, then the result of the WCS table lookup is undeﬁned. If the speciﬁed FITS BINTABLE contains no
     column, or multi-ple columns, with the speciﬁed TTYPEn, then the result of the WCS table lookup is undeﬁned.
     The speciﬁed FITS BINTABLE must contain only one row.
     */
    public static boolean isLookupTableValid(BasicHDU[] HDUs, String extName){

        int binaryHDUCount=0;
        for (int i=0; i<HDUs.length; i++){
            if (  HDUs[i] instanceof BinaryTableHDU &&
                    HDUs[i].getHeader().getStringValue("EXTNAME").equalsIgnoreCase(extName)){
                binaryHDUCount++;
            }
        }
        if (binaryHDUCount>1) return false;



        return true;
    }
    public static void checkHeader(Header header, int planeNumber, int extension_number) throws FitsException {


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

    public static Header cloneHeaderFrom(Header header) throws HeaderCardException {
        Cursor iter = header.iterator();
        Header clonedHeader = new Header();

        while (iter.hasNext()) {
            HeaderCard card = (HeaderCard) iter.next();
            clonedHeader.addLine(card.copy());
        }

        return clonedHeader;
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
    public static ImageHDU makeHDU(ImageHDU hdu, float[][] pixels)
            throws FitsException {
        Header header = hdu.getHeader();

        Header newHeader = cloneHeaderFrom(header);

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

    public static ArrayList<BasicHDU> getImageHDUList(BasicHDU[] HDUs, ArrayList<Integer> SUPPORTED_BIT_PIXS ) throws FitsException {
        ArrayList<BasicHDU> HDUList = new ArrayList<BasicHDU>();

        boolean hasExtension = HDUs.length > 1 ? true : false;
        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU) && !(HDUs[j] instanceof CompressedImageHDU)) {
                continue;   //ignore non-image extensions
            }

            //process image HDU or compressed image HDU as ImageHDU
            ImageHDU hdu = (HDUs[j] instanceof ImageHDU) ? (ImageHDU) HDUs[j] : ((CompressedImageHDU) HDUs[j]).asImageHDU();


            Header header = (hdu != null) ? hdu.getHeader() : null;
            if (header == null)
                throw new FitsException("Missing header in FITS file");

            int naxis = header.getIntValue("NAXIS", -1);
            boolean goodImage = FitsReadUtil.isImageGood(header);

            if (goodImage) {
                if (hasExtension) { // update this hdu by adding keywords/values
                    updateHeader(header, j, hdu.getFileOffset());
                }

                int naxis3 = header.getIntValue("NAXIS3", -1);
                if ((naxis > 2) && (naxis3 > 1)) { //it is a cube data
                    if (SUTDebug.isDebug())
                        System.out.println("GOT A FITS CUBE");
                    BasicHDU[] splitHDUs = splitFitsCube( hdu,SUPPORTED_BIT_PIXS );
                    /* for each plane of cube */
                    for (int jj = 0; jj < splitHDUs.length; jj++) {
                        HDUList.add(splitHDUs[jj]);
                    }
                } else {
                    HDUList.add(hdu);
                }
            }

            //when the header is added to the new fits file, the card number could be increased if the header is a primary
            //header.resetOriginalSize();

        } //end j loop
        return HDUList;
    }
    private  static void updateHeader(Header header, int pos, long hduOffset)

            throws FitsException {
        header.addLine(new HeaderCard(
                "SPOT_EXT", pos, "EXTENSION NUMBER (IN SPOT)"));

        header.addLine(new HeaderCard(
                "SPOT_OFF", hduOffset,
                "EXTENSION OFFSET (IN SPOT)"));
        header.resetOriginalSize();
    }

    private static BasicHDU[] splitFitsCube(ImageHDU hdu, ArrayList<Integer> SUPPORTED_BIT_PIXS )
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
            if (hdu.getHeader().getStringValue("CTYPE3").toUpperCase().startsWith("WAVE")||
                    hdu.getHeader().getStringValue("CTYPE3").toUpperCase().startsWith("AWAV")  ){
                //the third axis is wavelength, add the z coordinate to the header
                hdu.addValue("zPixel", i, "The coordinate value in the third axis");
            }

            hdu.getHeader().resetOriginalSize();
        }

        return hduList;
    }
    public static boolean anyCompressedImage(BasicHDU[] HDUs) {
        for (int j = 0; j < HDUs.length; j++) {
            if (HDUs[j] instanceof CompressedImageHDU)
                return true;
        }
        return false;
    }



    public static boolean isImageGood(Header aHeader) {

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
    public static Header getRefHeader(Geom geom, FitsRead fitsRead, double positionAngle,
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
     * Get the world point location
     *
     * @param imageHeader
     * @param aCoordinateSys
     * @return
     * @throws FitsException
     */
    private  static WorldPt getWorldPt(ImageHeader imageHeader, CoordinateSys aCoordinateSys) throws FitsException {
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
     *
     * @param raw_dn
     * @return
     */
    public static double getFlux(double  raw_dn, ImageHeader imageHeader){



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


    /**
     * This sigma value is calculated using the whole image data.
     * sigma = SQRT [  ( sum (xi-x_average)^2 )/n
     *   where x_average is the mean value of the x array
     *   n is the total number of the element of x array
     * @return
     */
    public static double computeSigma(float[] float1d, ImageHeader imageHeader) {

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
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *                      N
     *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
     *                      j=1
     *
     *  lamda = lamda_r + omega
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param ipt
     * @return
     * @throws PixelValueException
     */
    public static double[] getPixelCoords(ImagePt ipt, ImageHeader imageHeader, Header header) throws PixelValueException {

        int p0 = (int) Math.round(ipt.getX() - 0.5); //x
        int p1 = (int) Math.round(ipt.getY() - 0.5); //y
        int p2 = header.getIntValue("zPixel", 0); //z  default is 0



        if (    (p0 < 0) || (p0 >= imageHeader.naxis1) ||
                (p1 < 0) || (p1 >= imageHeader.naxis2) //|| (p2 < 0) || (p2 >= imageHeader.naxis3) //
                ) {
            throw new PixelValueException("location " + p0 + " "+ p1 + " " +p2 + " not on the image");
        }
        double[] p_j={p0, p1, p2};
        return p_j;
    }
    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r + omega
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
    private static double getWaveLengthLinear(double[] pixCoords ,int N,  double[] r_j, double[] pc_3j, double s_3,  double lamda_r) throws PixelValueException {

        return lamda_r + getOmega(pixCoords,  N, r_j, pc_3j, s_3);
    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r * exp (omega/s_3)
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
     private static double getWaveLengthLog(double[] pixCoords,int N,  double[] r_j, double[] pc_3j, double s_3,  double lamda_r) throws PixelValueException {


        double omega= getOmega(pixCoords,  N, r_j, pc_3j, s_3);
        return lamda_r* Math.exp(omega/lamda_r);

    }


    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *
     *  omega  = x_3 = s_3* Sum [ m3_j * (p_j - r_j) ]
     *
     *  lamda = lamda_r * exp (omega/s_3)
     *
     *  lamda_r : is the reference value,  given by CRVAL3
     *
     * @param pixCoords
     * @return
     * @throws PixelValueException
     */
    private static double getWaveLengthNonLinear(Header header, double[] pixCoords,int N, String algorithmCode, double[] r_j, double[] pc_3j, double s_3,  double lamda_r) throws PixelValueException {

        double lamda=Double.NaN;
        double omega = getOmega(pixCoords,  N, r_j, pc_3j, s_3);


        switch (algorithmCode){
            case "F2W":
                lamda =lamda_r *  lamda_r/(lamda_r - omega);
                break;
            case "V2W":
                double lamda_0 = header.getDoubleValue("RESTWAV", 0);
                double b_lamda = ( Math.pow(lamda_r, 4) - Math.pow(lamda_0, 4) + 4 *Math.pow(lamda_0, 2)*lamda_r*omega )/
                        Math.pow((Math.pow(lamda_0, 2) + Math.pow(lamda_r, 2) ), 2);
                lamda = lamda_0 - Math.sqrt( (1 + b_lamda)/(1-b_lamda) );
                break;
        }
        return lamda;

    }


    private static int isSorted(double[] intArray) {

        int end = intArray.length-1;
        int counterAsc = 0;
        int counterDesc = 0;

        for (int i = 0; i < end; i++) {
            if(intArray[i] < intArray[i+1]){
                counterAsc++;
            }
            else if(intArray[i] > intArray[i+1]){
                counterDesc++;
            }
        }
        if(counterDesc==0){
            return 1;
        }
        else if (counterAsc==0){
            return -1;
        }
        else return 0;
    }


    private static int searchIndex(double[] indexVec, double psi) throws DataFormatException {
        /*Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
          until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
          for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
          for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
         */

        int sort = isSorted(indexVec); //1:ascending, -1: descending, 0: not sorted
        if (sort==0){
            throw new DataFormatException("The vector index array has to be either ascending or descending");
        }


        for (int i=1; i<indexVec.length; i++){
            if (sort==1 && indexVec[i-1]<=psi  && psi<=indexVec[i]){
                return i;
            }
            if (sort==-1 && indexVec[i-1]>=psi && psi>=indexVec[i]){
                return i;

            }
        }
        return -1;

    }
    private static double calculateGamma_m(double[] indexVec, double psi, int idx) throws DataFormatException {
        /*Scan the indexing vector, (Ψ1, Ψ2,...), sequen-tially starting from the ﬁrst element, Ψ1,
          until a successive pair of index values is found that encompass ψm (i.e. such that Ψk ≤ ψm ≤ Ψk+1
          for monotonically increasing index values or Ψk ≥ ψm ≥ Ψk+1 for monotonically decreasing index values
          for some k). Then, when Ψk  Ψk+1, interpolate linearly on the indices
         */


        if (idx!=-1 && indexVec[idx-1]!=indexVec[idx]){
            // Υm = k + (ψm − Ψk) / (Ψk+1− Ψk)
            return  idx + (psi-indexVec[idx-1])/(indexVec[idx]-indexVec[idx-1]);
        }
        else {
            throw new DataFormatException("No index found in the index array, gamma is undefined, so is coordinate");
        }

    }



    private static double[] convertNumbericDataToDouble(Object obj){


        if (obj instanceof float[] ){
            float[] fData = (float[]) obj;
            double[] data = new double[fData.length];
            for (int i=0; i<fData.length; i++){
                data[i] = new Double(fData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof double[]){
            return (double[]) obj;
        }
        else if (obj instanceof int[] ){
            int[] iData = (int[]) obj;
            double[] data = new double[iData.length];
            for (int i=0; i<iData.length; i++){
                data[i] = new Double(iData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof long[] ){
            long[] lData = (long[]) obj;
            double[] data = new double[lData.length];
            for (int i=0; i<lData.length; i++){
                data[i] = new Double(lData[i]).doubleValue();
            }
            return data;
        }
        else if (obj instanceof short[] ) {
            short[] sData = (short[]) obj;
            double[] data = new double[sData.length];
            for (int i=0; i<sData.length; i++){
                data[i] = new Double(sData[i]).doubleValue();
            }
            return data;
        }
        return null;
    }
    private boolean isTableHDUValid(BinaryTableHDU tableHDU){

        int nCols = tableHDU.getNCols();
        int nRow = tableHDU.getNRows();
        if (nCols==0 || nCols>2 || nRow==0 || nRow>1) {
            return false;
        }
        return true;
    }
    /**
     * The Table look up table contains only one row, one or two columns.  The coordinates column and an optional index
     * vector column.
     * The data stored as an array in each cell.  The array size in coordinate and index can be different.
     *
     * Using FitsTableReader, the array in the cell is flatten and the result table is the multi-row with single value
     * at each cell.  The length of rows equals to the maximum array length between two columns. There fore the
     * table processed is two column and muliti-rows table.
     *
     *
     * @param columnName
     * @return
     */
    private static double[] getArrayDataFromTable(BinaryTableHDU tableHDU, String columnName)  throws DataFormatException, FitsException {


        if (tableHDU!=null) {
            //use binaryTable
            BinaryTable table  = tableHDU.getData();
            int nCols = table.getNCols();
            for (int i=0; i<nCols; i++){
                if (tableHDU.getColumnName(i).equalsIgnoreCase(columnName)){
                    Object obj = table.getFlattenedColumn(i);
                    return convertNumbericDataToDouble(obj);
                }

            }
        }
        else {
            throw new  DataFormatException("The Tab Lookup table is not provided");

        }
        return null;
    }


    /**
     *
     * The pre-requirement is that the FITS has three axies, ra (1), dec(2) and wavelength (3).
     * Therefore the keyword for i referenced in the paper is 3
     * @param pixCoords: is the pixel coordinates
     * @param N : is the dimensionality of the WCS representation given by NAXIS or WCSAXES
     * @param r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
     * @param s_3: is a scaling factor given by CDELT3
     * @param lamda_r : is the reference value,  given by CRVAL3
     * @return
     * @throws PixelValueException
     */
    public static double getWaveLengthTable(BinaryTableHDU bHDU, double[] pixCoords, int N, double[] r_j, double s_3,  double lamda_r, Header header) throws PixelValueException, FitsException, IOException, DataFormatException {



        double[] pc_3j = new double[N];

        //The indexing vectors exist, the CDEL
        String key =  header.containsKey("CDELT3")?"PC3":"CD3";
        for (int i = 0; i <N; i++) { //add default since the FITS file created by the python omiting some PCi_j if they are 0.
            pc_3j[i]=header.getDoubleValue(key +"_"+i+1, 0);
        }
        double omega = getOmega(pixCoords,  N, r_j, pc_3j, s_3);

        double cdelt_3 = header.getDoubleValue("CDELT3");
        double psi_m = header.containsKey("CDELT3")?lamda_r + cdelt_3* omega:lamda_r + omega;

        //read the cell coordinate from the FITS table and save to two one dimensional arrays
        double [] coordData = getArrayDataFromTable(bHDU,"COORDS");

        double[] indexData=null;
        if (header.containsKey("PS3_2")) {
            indexData =  getArrayDataFromTable(bHDU,"INDEX");

        }

        if (indexData!=null) {


            int psiIndex = searchIndex(indexData, psi_m);
            if (psiIndex==-1) {
                throw new DataFormatException("No index found in the index array, gamma is undefined, so is coordinate");
            }
            double gamma_m = calculateGamma_m(indexData, psi_m, psiIndex);


            return  coordData[psiIndex] + (gamma_m - psiIndex) * (coordData[psiIndex+1] - coordData[psiIndex]);
        }
        else {
            return coordData[ (int) psi_m];
        }
    }

    /**
     *
     * The algorithm is based on the fact that the spectral data is stored in the naxis3.  The naxis1, ctype1:ra
     * naxis2, ctype2: dec, naxis3, ctype3 : wavelength
     * Thus, the k value referenced in the paper is 3 here
     *                      N
     *  omega  = x_3 = s_3* ∑ [ m3_j * (p_j - r_j) ]
     *                      j=1
     *
     *  N: is the dimensionality of the WCS representation given by NAXIS or WCSAXES
     *  p_j: is the pixel coordinates
     *  r_j: is the pixel coordinate of the reference point given by CRPIXJ where j=1... N, where N=3 in our case
     *  s_3: is a scaling factor given by CDELT3
     *  m3_j: is a linear transformation matrix given either by PC3_j or CD3_j
     *
     *
     * @param pixCoords
     * @param N
     * @param r_j
     * @param pc_3j
     * @param s_3
     * @return
     */
    private static double getOmega(double[] pixCoords, int N,  double[] r_j, double[] pc_3j, double s_3){
        double omega =0.0;
        for (int i=0; i<N; i++){
            omega += s_3 * pc_3j[i] * (pixCoords[i]-r_j[i]);
        }
        return omega;
    }
    public static float[] getImageHDUDataInFloatArray(ImageHDU imageHDU, ImageHeader imageHeader) throws FitsException {

        float[]  float1d =
                (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageHDU.getData().getData(), Float.TYPE, true));

        /* pixels are upside down - reverse them in y */
        if (imageHeader.cdelt2 < 0) float1d = reversePixData(imageHeader, float1d);


        return float1d;
    }



   static float[] reversePixData(ImageHeader imageHeader,float[] float1d) {

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


    public static void stretchPixelsUsingOtherAlgorithms(int startPixel,
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

    private static double getPowerLawGammaStretchedPixelValue(double x, double gamma, double zp, double mp){
        if (x <= zp) { return 0d; }
        if (x >= mp) { return 254d; }
        double  rd =  x-zp;
        double  nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
        double pixValue = 255*nsd;

        return pixValue;

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
    public static void stretchPixels(int startPixel,
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
        if (flux <= minFlux) { return 0d; }
        if (flux >= maxFlux) { return 254d; }

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




    public static double getShigh(RangeValues rangeValues, float[] float1d, ImageHeader imageHeader, Histogram hist) {
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

    public static double getSlow(RangeValues rangeValues,  float[] float1d, ImageHeader imageHeader, Histogram hist) {
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
    public static byte[] getHistColors(Histogram hist, RangeValues rangeValues, float[] float1d, ImageHeader imageHeader) {

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
    public static void stretchPixels(int startPixel,
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
                    blank_pixel_value, float1dArray, pixeldata, rangeValues,slow,shigh);


        }
        else {
            stretchPixelsUsingOtherAlgorithms(startPixel, lastPixel, startLine, lastLine,imageHeader.naxis1, hist,
                    blank_pixel_value,float1dArray, pixeldata, rangeValues, slow, shigh);
        }
    }

    private static void stretchPixelsUsingAsin(int startPixel,
                                               int lastPixel,
                                               int startLine,
                                               int lastLine,
                                               int naxis1,
                                               ImageHeader imageHeader,
                                               byte blank_pixel_value,
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
        double flux;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;
            for (int index = start_index; index <= last_index; index++) {
                flux = getFlux(float1dArray[index], imageHeader);
                if (Double.isNaN(flux)) { //original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {
                    pixeldata[pixelCount] = (byte) getASinhStretchedPixelValue(flux, maxFlux, minFlux, beta);
                }
                pixelCount++;
            }
        }

    }

    public static double calculateWavelength(Header header, BinaryTableHDU tableHDU, double[] pixelCoords, String fitsType)
            throws PixelValueException, FitsException, IOException, DataFormatException {

        String algorithm =getAlgorithm(fitsType);
        int N = header.getIntValue("WCSAXES", -1);
        if (N==-1) {
            N = header.getIntValue("NAXIS", -1);
            if (N==-1){
                throw new  DataFormatException("Dimension value is not avaiable, Please set either NAXIS or WCSAXES in the header ");
            }
        }

        double[] r_j = new double[N];
        double[] pc_3j = new double[N];
        //The pi_j can be either CDi_j or PCi_j, so we have to test both
        for (int i = 0; i < N; i++) {
            r_j[i] = header.getDoubleValue("CRPIX" + (i+1), Double.NaN);

            //matrix mij can be either PCij or CDij
            pc_3j[i]=header.getDoubleValue("PC3" +"_"+ (i+1), Double.NaN);
            if (Double.isNaN(pc_3j[i])) {
                pc_3j[i]=header.getDoubleValue("CD3" +"_"+ (i+1), Double.NaN);
            }

            if (Double.isNaN(r_j[i])){
                throw new DataFormatException("CRPIXka  is not defined");
            }
            if ( Double.isNaN(pc_3j[i]) ){
                throw new DataFormatException("Either PC3_i or CD3_i has to be defined");
            }

            if (algorithm.equalsIgnoreCase("LOG")){
                //the values in CRPIXk and CDi_j (PC_i_j) are log based on 10 rather than natural log, so a factor is needed.
                r_j[i] *=Math.log(10);
                pc_3j[i] *=Math.log(10);
            }
        }

        double s_3 = header.getDoubleValue("CDELT3", 0);
        double lamda_r = header.getDoubleValue("CRVAL3");

        if (algorithm==null) return Double.NaN;
        double lamda=Double.NaN;

        switch (algorithm.toUpperCase()){
            case "LINEAR":
                lamda = getWaveLengthLinear(pixelCoords,N, r_j, pc_3j, s_3, lamda_r);
                break;
            case "LOG":
                lamda =getWaveLengthLog(pixelCoords,N, r_j, pc_3j, s_3, lamda_r);
                break;
            case "F2W":
            case "V2W":
                lamda =getWaveLengthNonLinear(header,pixelCoords,N, algorithm,r_j, pc_3j, s_3, lamda_r);
                break;
            case "TAB":
                lamda = FitsReadUtil.getWaveLengthTable(tableHDU, pixelCoords,  N,  r_j,  s_3,   lamda_r, header) ;
                break;
        }
        return lamda;

    }
    /**
     * This method will return the algorithm specified in the FITS header.
     * If the algorithm is TAB, the header has to contain the keyword "EXTNAME".
     * The fitsType = header.getStringValue("CTYPE3"), will tell what algorithm it is.
     * The value of "CTYPE3" is WAVE-AAA, the AAA means algorithm.  If the fitsType only has
     * "WAVEE', it is linear.
     * @return
     */
    private static String getAlgorithm(String fitsType){

        String[] sArray = fitsType.split("-");
        if (sArray.length==0) return null;

        //the length has to be larger than 1
        if (sArray.length==1){
            return "Linear";

        }
        else if (sArray[1].trim().equalsIgnoreCase("LOG")){
            return "Log";
        }
        else {
            return sArray[1];
        }
    }

}
