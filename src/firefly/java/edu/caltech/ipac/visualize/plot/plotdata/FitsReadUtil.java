/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.UndefinedData;
import nom.tam.fits.UndefinedHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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


    //    public boolean isSameProjection(FitsRead secondFitsread, ImageHeader H1) {
//        boolean result = false;
//
//        //ImageHeader H1 = getImageHeader();
//        ImageHeader H2 = secondFitsread.getImageHeader();
//
//        if (H1.maptype == H2.maptype) {
//            if (H1.maptype == Projection.PLATE) {
//                result = checkPlate(H1, H2);
//            } else {
//                result = checkOther(H1, H2);
//            }
//        }
//        return result;
//    }


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

//        newHeader.deleteKey("BITPIX");
//        newHeader.setBitpix(-32);
//        newHeader.deleteKey("NAXIS");
//        newHeader.setNaxes(2);
//        newHeader.deleteKey("NAXIS1");
//        newHeader.setNaxis(1, pixels[0].length);
//        newHeader.deleteKey("NAXIS2");
//        newHeader.setNaxis(2, pixels.length);

//        newHeader.deleteKey("DATAMAX");
//        newHeader.deleteKey("DATAMIN");
//        newHeader.deleteKey("NAXIS3");
//        newHeader.deleteKey("NAXIS4");
//        newHeader.deleteKey("BLANK");

        ImageData new_image_data = new ImageData(pixels);
        return new ImageHDU(newHeader, new_image_data);

    }

    public static ArrayList<BasicHDU> getImageHDUList(BasicHDU[] HDUs) throws FitsException {
        ArrayList<BasicHDU> HDUList = new ArrayList<>();

        String delayedExceptionMsg = null; // the exception can be ignored if HDUList size is greater than 0
        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU) && !(HDUs[j] instanceof CompressedImageHDU)) {
                continue;   //ignore non-image extensions
            }

            //process image HDU or compressed image HDU as ImageHDU
            BasicHDU hdu = HDUs[j];


            Header header = (hdu != null) ? hdu.getHeader() : null;
            if (header == null)
                throw new FitsException("Missing header in FITS file");

            int naxis = header.getIntValue("NAXIS", -1);


            // check whether image is valid
            boolean goodImage = true;
            if (naxis <= 0) goodImage = false;
            else if (naxis == 1) {
                delayedExceptionMsg = "One-dimensional images (NAXIS==1) are not currently supported.";
                goodImage = false;
            } else {
                for (int i = 1; i <= naxis; i++) {
                    int naxisValue = header.getIntValue("NAXIS" + i, 0);
                    if (naxisValue == 0) {
                        delayedExceptionMsg = "FITS image has NAXIS" + i + "=0";
                        goodImage = false;
                    }
                }
            }


            if (goodImage) {
                insertPositionIntoHeader(header, j, hdu.getFileOffset());

                int naxis3 = header.getIntValue("NAXIS3", -1);
                if ((naxis > 2) && (naxis3 > 1)) { //it is a cube data
                    if (SUTDebug.isDebug())
                        System.out.println("GOT A FITS CUBE");
                    BasicHDU[] splitHDUs = splitFitsCube( hdu);
                    /* for each plane of cube */
                    Collections.addAll(HDUList, splitHDUs);
                } else {
                    HDUList.add(hdu);
                }
            }

            //when the header is added to the new fits file, the card number could be increased if the header is a primary
            //header.resetOriginalSize();

        } //end j loop

        if (HDUList.size() == 0 && delayedExceptionMsg != null ) {
            throw new FitsException(delayedExceptionMsg);
        }

        return HDUList;
    }

    private static void insertPositionIntoHeader(Header header, int pos, long hduOffset) throws FitsException {
        if (hduOffset<0) hduOffset= 0;
        if (pos<0) pos= 0;
        long headerSize= header.getOriginalSize()>0 ? header.getOriginalSize() : header.getSize();
        int bitpix = header.getIntValue("BITPIX", -1);
        header.addLine(new HeaderCard( "SPOT_HS", headerSize, "Header block size on disk (added by Firefly)"));
        header.addLine(new HeaderCard( "SPOT_EXT", pos, "Extension Number (added by Firefly)"));
        header.addLine(new HeaderCard( "SPOT_OFF", hduOffset, "Extension Offset (added by Firefly)"));
        header.addLine(new HeaderCard( "SPOT_BP", bitpix, "Original Bitpix value (added by Firefly)"));
        header.resetOriginalSize();
    }


    private static BasicHDU[] splitFits3DCube(BasicHDU inHdu, float[][][] data32) throws FitsException {
        ImageHDU hdu = (inHdu instanceof ImageHDU) ? (ImageHDU) inHdu : ((CompressedImageHDU) inHdu).asImageHDU();  // if we have to uncompress a cube it could take a long time
        BasicHDU[] hduList = new BasicHDU[hdu.getHeader().getIntValue("NAXIS3", 0)];

        for (int i = 0; i < hduList.length; i++) {
            hduList[i] = makeHDU(hdu,data32[i] );
            //set the header pointer to the BITPIX location to add the new key. Without calling this line, the pointer is point
            //to the end of the Header, the SPOT_PL is added after the "END" key, which leads the image loading failure. 
            hduList[i].getHeader().getIntValue("BITPIX", -1);
            hduList[i].getHeader().addLine(new HeaderCard("SPOT_PL", i, "Plane of FITS cube (added by Firefly)"));
            hduList[i].getHeader().resetOriginalSize();

        }
        return hduList;

    }

    private static BasicHDU[] splitFitsCube(BasicHDU inHdu) throws FitsException {
        ImageHDU hdu = (inHdu instanceof ImageHDU) ? (ImageHDU) inHdu : ((CompressedImageHDU) inHdu).asImageHDU();  // if we have to uncompress a cube it could take a long time
        int naxis = inHdu.getHeader().getIntValue("NAXIS", -1);

        switch (naxis) {
            case 3:

                float[][][] data3D = (float[][][]) ArrayFuncs.convertArray(hdu.getData().getData(), Float.TYPE, true);
                return splitFits3DCube(inHdu,data3D);

            case 4:
               float[][][][] data4D = (float[][][][]) ArrayFuncs.convertArray(hdu.getData().getData(), Float.TYPE, true);
               ArrayList<BasicHDU> hduListArr = new ArrayList<>();
                int naxis4 = inHdu.getHeader().getIntValue("NAXIS4", -1);
                if (naxis4==1) {
                    for (int i = 0; i < naxis4; i++) {
                        BasicHDU[] hduList = splitFits3DCube(inHdu, data4D[i]);
                        for (int k = 0; k < hduList.length; k++) {
                            hduListArr.add(hduList[k]);
                        }
                    }
                    return hduListArr.toArray(new BasicHDU[0]);
                }
                else {
                    throw new IllegalArgumentException("naxis4>1 is not supported");

                }
            default:
                throw new IllegalArgumentException("naxis="+naxis + " is not supported");

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


    private boolean isTableHDUValid(BinaryTableHDU tableHDU){

        int nCols = tableHDU.getNCols();
        int nRow = tableHDU.getNRows();
        if (nCols==0 || nCols>2 || nRow==0 || nRow>1) {
            return false;
        }
        return true;
    }

    public static float[] getImageHDUDataInFloatArrayOLD(BasicHDU inHDU) throws FitsException{
        
        ImageHDU imageHDU;
        if (inHDU instanceof ImageHDU) {
            imageHDU = (ImageHDU) inHDU;
        }
        else if (inHDU instanceof CompressedImageHDU) {
            imageHDU = ((CompressedImageHDU) inHDU).asImageHDU();
        }
        else {
            throw new FitsException("hdu much be a ImageHDU or a CompressedImageHDU");
        }
        Header header= imageHDU.getHeader();
        double cdelt2 = header.getDoubleValue("CDELT2");
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");

         float[]  float1d =
                         (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageHDU.getData().getData(), Float.TYPE, true));

         /* pixels are upside down - reverse them in y */
         if (cdelt2 < 0) float1d = reversePixData(naxis1, naxis2, float1d);
         return float1d;
     }

    public static float[] getImageHDUDataInFloatArray(BasicHDU inHDU) throws FitsException {

        ImageHDU imageHDU;
        float[] float1d;

        if (inHDU instanceof ImageHDU) imageHDU = (ImageHDU) inHDU;
        else if (inHDU instanceof CompressedImageHDU) imageHDU = ((CompressedImageHDU) inHDU).asImageHDU();
        else throw new FitsException("hdu much be a ImageHDU or a CompressedImageHDU");

        ImageData imageDataObj= imageHDU.getData();
        if (imageDataObj==null) throw new FitsException("No data in HDU");



        Header header= imageHDU.getHeader();
        double cdelt2 = header.getDoubleValue("CDELT2");
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");

        try {
            if (imageDataObj.getTiler()!=null) {
                Object unknownArrayOfData= imageDataObj.getTiler().getTile(new int[] {0,0}, new int[] {naxis2,naxis1});
                float1d = (float[]) ArrayFuncs.convertArray(unknownArrayOfData, Float.TYPE, true);
            }
            else {
                float1d = (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageDataObj.getData(), Float.TYPE, true));
            }
        } catch (IOException e) {
            float1d = (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageDataObj.getData(), Float.TYPE, true));
        }


        if (cdelt2 < 0) float1d = reversePixData(naxis1, naxis2, float1d);// pixels are upside down - reverse them in y
        return float1d;


    }

    /**
     * This returns a 1d array of double.  This is not interchangable with getImageHDUDataInFloatArray. It is used
     * mostly for tables and if not as efficent.
     * @param inHDU
     * @return
     * @throws FitsException
     */
    public static double[] getImageHDUDataInDoubleArray(BasicHDU inHDU) throws FitsException {



        if (inHDU instanceof UndefinedHDU) {
            UndefinedData data= (UndefinedData)inHDU.getData();
            if (data==null) throw new FitsException("No data in HDU");
            return (double[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(data.getData(), Double.TYPE, true));
        }


        ImageHDU imageHDU;
        if (inHDU instanceof ImageHDU) imageHDU = (ImageHDU) inHDU;
        else if (inHDU instanceof CompressedImageHDU) imageHDU = ((CompressedImageHDU) inHDU).asImageHDU();
        else throw new FitsException("hdu much be a ImageHDU or a CompressedImageHDU or a UndefinedHDU");

        ImageData imageDataObj= imageHDU.getData();
        if (imageDataObj==null) throw new FitsException("No data in HDU");

        return (double[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageDataObj.getData(), Double.TYPE, true));
    }




    private static float[] reversePixData(int naxis1, int naxis2,float[] float1d) {

        // pixels are upside down - reverse them in y
        float[] temp = new float[float1d.length];
        int index_src = 0;
        for (int y = 0; y < naxis2; y++) {
            int indexDest = (naxis2 - y - 1) * naxis1;
            for (int x = 0; x < naxis1; x++) {
                temp[indexDest++] = float1d[index_src++];
            }
        }
        float1d = temp;
        return float1d;
    }


}
