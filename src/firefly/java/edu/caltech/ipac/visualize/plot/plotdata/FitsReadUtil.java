/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.PaddingException;
import nom.tam.fits.UndefinedData;
import nom.tam.fits.UndefinedHDU;
import nom.tam.image.StandardImageTiler;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedFile;
import nom.tam.util.Cursor;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Since the FitsRead class is heavily used, it is better to keep it shorter.  Thus, I moved many methods
 * to this Utility class, FitsReadUtil.java.   This class contains the public static methods which are
 * mostly used by FitsRead.
 */
public class FitsReadUtil {

    public static final String SPOT_HS = "SPOT_HS";
    public static final String SPOT_EXT = "SPOT_EXT"; // HDU Number
    public static final String SPOT_OFF = "SPOT_OFF";
    public static final String SPOT_BP = "SPOT_BP"; // original bitpix
    public static final String SPOT_PL = "SPOT_PL"; // cube plane number, only used with cubes, deprecated

    public static ImageData getImageData(BasicHDU<?> refHdu, float[] float1d) throws FitsException {
        Header header = refHdu.getHeader();
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");
        int[] dims2 = new int[]{naxis1, naxis2};
        float[][] fdata = (float[][]) ArrayFuncs.curl(float1d, dims2);
        Object data = ArrayFuncs.convertArray(fdata, getDataType(refHdu.getBitPix()), true);
        return new ImageData(data);
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
     * <p>
     * This method return the physical data value at the pixels as an one dimensional array
     */
    public static float[] getDataFloat(float[] float1d, ImageHeader imageHeader) {

        float[] fData = new float[float1d.length];

        for (int i = 0; i < float1d.length; i++) {
            fData[i] = float1d[i] * (float) imageHeader.bscale + (float) imageHeader.bzero;
        }
        return fData;
    }


    public static double[] getPhysicalDataDouble(double[] double1d, ImageHeader imageHeader) {

        double[] dData = new double[double1d.length];

        for (int i = 0; i < double1d.length; i++) {
            dData[i] = double1d[i] * (float) imageHeader.bscale + (float) imageHeader.bzero;
        }
        return dData;
    }


    public static Header cloneHeaderFrom(Header header) throws HeaderCardException {
        Cursor<String, HeaderCard> iter = header.iterator();
        Header clonedHeader = new Header();

        while (iter.hasNext()) {
            HeaderCard card = iter.next();
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
        ImageData new_image_data = new ImageData(pixels);
        return new ImageHDU(newHeader, new_image_data);

    }


    public static boolean hasCompressedImageHDUS(BasicHDU<?>[] HDUs)  {
        for (BasicHDU<?> hdu : HDUs) {
            if (hdu instanceof CompressedImageHDU) return true;
        }
        return false;
    }

    public static Header getTopFitsHeader(File f) {
        try {
            Fits fits= new Fits(f);
            Header header=  fits.getHDU(0).getHeader();
            closeFits(fits);
            return header;
        } catch (FitsException|IOException  e) {
            return null;
        }
    }

    public static class UncompressFitsInfo {
        private final Fits fits;
        private final File file;
        private final BasicHDU<?>[] HDUs;

        public UncompressFitsInfo(File file, BasicHDU<?>[] HDUs, Fits fits) {
            this.fits= fits;
            this.file = file;
            this.HDUs = HDUs;
        }

        public File getFile() { return file; }
        public Fits getFits() { return fits; }
        public BasicHDU<?>[] getHDUs() { return HDUs; }
    }


    public static UncompressFitsInfo createdUncompressImageHDUFile(BasicHDU<?>[] HDUs, File originalFile)
            throws FitsException, IOException {
        String fBase= FileUtil.getBase(originalFile);
        String dir= originalFile.getParent();
        File retFile= new File(dir+"/"+ fBase+"---hdu-uncompressed"+".fits");
        List<BasicHDU<?>> outHDUsList= new ArrayList<>(HDUs.length);
        Fits fits= new Fits();
        for (BasicHDU<?> hdu : HDUs) {
            if (hdu instanceof CompressedImageHDU) {
                ImageHDU ihdu= ((CompressedImageHDU) hdu).asImageHDU();
                fits.addHDU(ihdu);
                outHDUsList.add(ihdu);
            }
            else {
                fits.addHDU(hdu);
                outHDUsList.add(hdu);
            }
        }
        BufferedFile bf = new BufferedFile(retFile.getPath(), "rw");
        fits.write(bf);
        bf.close();
        return new UncompressFitsInfo(retFile,outHDUsList.toArray(new BasicHDU[0]), fits);
    }

    public static BasicHDU<?>[] getImageHDUArray(BasicHDU<?>[] HDUs, boolean onlyFireCubeHdu) throws FitsException {
        ArrayList<BasicHDU<?>> HDUList = new ArrayList<>();

        String delayedExceptionMsg = null; // the exception can be ignored if HDUList size is greater than 0
        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU) && !(HDUs[j] instanceof CompressedImageHDU)) {
                continue;   //ignore non-image extensions
            }

            //process image HDU or compressed image HDU as ImageHDU
            BasicHDU<?> hdu = HDUs[j];


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
                    BasicHDU<?>[] splitHDUs = splitFitsCube(hdu, onlyFireCubeHdu);
                    /* for each plane of cube */
                    Collections.addAll(HDUList, splitHDUs);
                } else {
                    HDUList.add(hdu);
                }
            }

            //when the header is added to the new fits file, the card number could be increased if the header is a primary
            //header.resetOriginalSize();

        } //end j loop

        if (HDUList.size() == 0 && delayedExceptionMsg != null) {
            throw new FitsException(delayedExceptionMsg);
        }
        return HDUList.toArray(new BasicHDU<?>[0]);
    }


    private static void insertPositionIntoHeader(Header header, int pos, long hduOffset) throws FitsException {
        if (hduOffset < 0) hduOffset = 0;
        if (pos < 0) pos = 0;
        long headerSize = header.getOriginalSize() > 0 ? header.getOriginalSize() : header.getSize();
        int bitpix = header.getIntValue("BITPIX", -1);
        header.addLine(new HeaderCard(SPOT_HS, headerSize, "Header block size on disk (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_EXT, pos, "Extension Number (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_OFF, hduOffset, "Extension Offset (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_BP, bitpix, "Original Bitpix value (added by Firefly)"));
        header.resetOriginalSize();
    }


    private static BasicHDU<?>[] splitFits3DCube(BasicHDU<?> inHdu, boolean onlyFirstCubeHdu) throws FitsException {
        ImageHDU hdu = (inHdu instanceof ImageHDU) ? (ImageHDU) inHdu : ((CompressedImageHDU) inHdu).asImageHDU();  // if we have to uncompress a cube it could take a long time
        BasicHDU<?>[] hduList = new BasicHDU<?>[hdu.getHeader().getIntValue("NAXIS3", 0)];

        for (int i = 0; i < hduList.length; i++) {
            if (onlyFirstCubeHdu && i>0) {
                hduList[i] = null;
            }
            else {
                hduList[i] = makeHDU(hdu, null);
                //set the header pointer to the BITPIX location to add the new key. Without calling this line, the pointer is point
                //to the end of the Header, the SPOT_PL is added after the "END" key, which leads the image loading failure.
                hduList[i].getHeader().getIntValue("BITPIX", -1);
                hduList[i].getHeader().addLine(new HeaderCard(SPOT_PL, i, "Plane of FITS cube (added by Firefly)"));
                hduList[i].getHeader().resetOriginalSize();
            }

        }
        return hduList;

    }

    private static BasicHDU<?>[] splitFitsCube(BasicHDU<?> inHdu, boolean onlyFirstCubeHdu) throws FitsException {
        int naxis = inHdu.getHeader().getIntValue("NAXIS", -1);

        switch (naxis) {
            case 3:
                return splitFits3DCube(inHdu, onlyFirstCubeHdu);
            case 4:
                ArrayList<BasicHDU<?>> hduListArr = new ArrayList<>();
                int naxis4 = inHdu.getHeader().getIntValue("NAXIS4", -1);
                if (naxis4 == 1) {
                    for (int i = 0; i < naxis4; i++) {
                        BasicHDU<?>[] hduList = splitFits3DCube(inHdu, onlyFirstCubeHdu);
                        if (onlyFirstCubeHdu) {
                            for(int j=1; (j<hduList.length);j++) hduList[j]= null;
                        }
                        Collections.addAll(hduListArr, hduList);
                    }
                    return hduListArr.toArray(new BasicHDU<?>[0]);
                } else {
                    throw new IllegalArgumentException("naxis4>1 is not supported");

                }
            default:
                throw new IllegalArgumentException("naxis=" + naxis + " is not supported");

        }
    }

    /**
     * a new reference header is created
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
     * @param imHeader       the header
     * @param aCoordinateSys coordinate system
     * @return a world pt
     * @throws FitsException when something goes wrong
     */
    private static WorldPt getWorldPt(ImageHeader imHeader, CoordinateSys aCoordinateSys) throws FitsException {
        try {
            CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(imHeader.getJsys(), imHeader.file_equinox);
            double centerX = (imHeader.naxis1 + 1.0) / 2.0;
            double centerY = (imHeader.naxis2 + 1.0) / 2.0;
            WorldPt worldPt = imHeader.createProjection(inCoordinateSys).getWorldCoords(centerX - 1, centerY - 1);
            return Plot.convert(worldPt, aCoordinateSys);
        } catch (ProjectionException pe) {
            throw new FitsException("Could not rotate image: got ProjectionException: " + pe.getMessage());
        }
    }


    /**
     * The input refHeader will be modified and new keys/values are added
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

    public static float[] getImageHDUDataInFloatArray(BasicHDU<?> inHDU) throws FitsException {

        ImageHDU imageHDU;
        float[] float1d;

        if (inHDU instanceof ImageHDU) imageHDU = (ImageHDU) inHDU;
        else if (inHDU instanceof CompressedImageHDU) imageHDU = ((CompressedImageHDU) inHDU).asImageHDU();
        else throw new FitsException("hdu much be a ImageHDU or a CompressedImageHDU");

        ImageData imageDataObj = imageHDU.getData();
        if (imageDataObj == null) throw new FitsException("No data in HDU");


        Header header = imageHDU.getHeader();
        double cdelt2 = header.getDoubleValue("CDELT2");
        int naxis1 = header.getIntValue("NAXIS1");
        int naxis2 = header.getIntValue("NAXIS2");

        try {
            if (imageDataObj.getTiler() != null) {
                Object unknownArrayOfData = imageDataObj.getTiler().getTile(new int[]{0, 0}, new int[]{naxis2, naxis1});
                float1d = (float[]) ArrayFuncs.convertArray(unknownArrayOfData, Float.TYPE, true);
            } else {
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
     *
     * @param inHDU the fits hdu
     * @return an array
     * @throws FitsException if failed
     */
    public static double[] getImageHDUDataInDoubleArray(BasicHDU<?> inHDU) throws FitsException {


        if (inHDU instanceof UndefinedHDU) {
            UndefinedData data = (UndefinedData) inHDU.getData();
            if (data == null) throw new FitsException("No data in HDU");
            return (double[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(data.getData(), Double.TYPE, true));
        }


        ImageHDU imageHDU;
        if (inHDU instanceof ImageHDU) imageHDU = (ImageHDU) inHDU;
        else if (inHDU instanceof CompressedImageHDU) imageHDU = ((CompressedImageHDU) inHDU).asImageHDU();
        else throw new FitsException("hdu much be a ImageHDU or a CompressedImageHDU or a UndefinedHDU");

        ImageData imageDataObj = imageHDU.getData();
        if (imageDataObj == null) throw new FitsException("No data in HDU");

        return (double[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(imageDataObj.getData(), Double.TYPE, true));
    }


    private static float[] reversePixData(int naxis1, int naxis2, float[] float1d) {

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


    public static void writeFitsFile(OutputStream stream, FitsRead[] fitsReadAry, Fits refFits) throws FitsException, IOException {
        Fits output_fits = new Fits();
        for (FitsRead fr : fitsReadAry) {
            BasicHDU<?> refHdu = refFits.getHDU(0);
            ImageHDU imageHDU = new ImageHDU(refHdu.getHeader(), FitsReadUtil.getImageData(refHdu, fr.getDataFloat()));
            output_fits.addHDU(imageHDU);
        }
        output_fits.write(new DataOutputStream(stream));
    }

    public static BasicHDU<?>[] readHDUs(Fits fits) throws FitsException {
        try {
            return fits.read();
        } catch (PaddingException pe) {
            fits.addHDU(pe.getTruncatedHDU());
            return fits.read();
        }
    }

    public static int getNaxis(Header h) { return h.getIntValue("NAXIS", 0); }
    public static int getNaxis1(Header h) { return h.getIntValue("NAXIS1", 0); }
    public static int getNaxis2(Header h) { return h.getIntValue("NAXIS2", 0); }
    public static int getNaxis3(Header h) { return (getNaxis2(h) > 2) ? h.getIntValue("NAXIS3") : 1; }
    public static int getNaxis4(Header h) { return (getNaxis3(h) > 2) ? h.getIntValue("NAXIS4") : 1; }
    public static double getBscale(Header h) { return h.getDoubleValue("BSCALE", 1.0); }
    public static double getBzero(Header h) { return h.getDoubleValue("BZERO", 0.0); }
    public static double getBlankValue(Header h) { return h.getDoubleValue("BLANK", Double.NaN); }
    public static String getExtName(Header h) { return h.getStringValue("EXTNAME"); }
    public static String getUtype(Header h) { return h.getStringValue("UTYPE"); }




    static List<String> alphabetAry= Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWZYZ".split(""));

    /**
     * return an array of all the alt projections in this file.
     * @param h the fits header
     * @return {string[]}
     */
    public static List<String> getAtlProjectionIDs(Header h) {
        return alphabetAry.stream().filter( (c) -> h.containsKey("CTYPE1"+c)).toList();
    }


    public static String findHeaderValue(Header header, String... keys) {
        String value= null;
        for(String k : keys) {
            if (k!=null) value= header.getStringValue(k);
            if (value!=null) return value;
        }
        return null;
    }



    public static void closeFits(Fits fits) {
        try {
            if (fits != null && fits.getStream() != null) fits.getStream().close();
        } catch (IOException ignore) {
        }
    }

    private static double averageArray(double[] ary) {
        if (ary.length == 0) return Double.NaN;
        if (ary.length == 1) return ary[0];
        float sum = 0;
        float cnt = 0;
        for (double v : ary) {
            if (!Double.isNaN(v)) {
                sum += v;
                cnt++;
            }
        }
        return cnt > 0 ? sum / cnt : Float.NaN;
    }



    interface Extractor { double[] extractAry(BasicHDU<?>[] hdus, int hduNum) throws FitsException, IOException; }


    public static List<ExtractionResults> extractFromRelatedHDUs(File fitsFile, int refHduNum,
                                                                 boolean allMatchingHDUs, Extractor extractor)
            throws FitsException, IOException {
        Fits fits = null;
        try {
            fits = new Fits(fitsFile);
            BasicHDU<?>[] hdus = readHDUs(fits);
            BasicHDU<?> hdu = hdus[refHduNum];
            validateImageAtHDU(hdus, refHduNum);
            Header refHeader = hdu.getHeader();
            int dims = getNaxis(refHeader);
            int xLen = getNaxis1(refHeader);
            int yLen = getNaxis2(refHeader);
            int zLen = getNaxis3(refHeader);
            List<ExtractionResults> retList = new ArrayList<>();

            if (allMatchingHDUs) {
                for (int i = 0; (i < hdus.length); i++) {
                    Header h = hdus[i].getHeader();
                    if (getNaxis(h) == dims && getNaxis1(h) == xLen && getNaxis2(h) == yLen && getNaxis3(h) == zLen) {
                        double[] ary = extractor.extractAry(hdus, i);
                        retList.add(new ExtractionResults(i, getExtName(h), ary, i == refHduNum, h));
                    }
                }
            } else {
                double[] ary = extractor.extractAry(hdus, refHduNum);
                retList.add(new ExtractionResults(refHduNum, getExtName(refHeader), ary, true, refHeader));
            }
            return retList;
        } finally {
            closeFits(fits);
        }
    }

    public static double[] extractFromHDU(File fitsFile, int hduNum, Extractor extractor)
            throws FitsException, IOException {
        Fits fits= null;
        try {
            fits = new Fits(fitsFile);
            return extractor.extractAry(readHDUs(fits), hduNum);
        }
        finally {
            closeFits(fits);
        }
    }

    public static List<ExtractionResults> getAllPointsFromRelatedHDUs(ImagePt[] ptAry, File fitsFile,
                                                                      int refHduNum, int plane,
                                                                      boolean allMatchingHDUs, int ptSize)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus, hduNum) -> getPointDataAry(ptAry, plane, hdus, hduNum, ptSize));
    }

    public static List<ExtractionResults> getAllLinesFromRelatedHDUs(ImagePt pt, ImagePt pt2, File fitsFile,
                                                                     int refHduNum, int plane,
                                                                     boolean allMatchingHDUs, int ptSize)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus, hduNum) -> getLineDataAry(pt, pt2, plane, hdus, hduNum, ptSize));
    }

    public static List<ExtractionResults> getAllZAxisAryFromRelatedCubes(ImagePt pt, File fitsFile, int refHduNum,
                                                                         boolean allMatchingHDUs, int ptSize)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus,hduNum) -> getZAxisAry(pt,hdus,hduNum,ptSize) );
    }

    public static double[] getPointDataAryFromFile(ImagePt[] ptAry, int plane, File fitsFile, int hduNum, int ptSize)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getPointDataAry(ptAry,plane, hdus,num,ptSize));
    }

    public static double[] getLineDataAryFromFile(ImagePt pt, ImagePt pt2, int plane, File fitsFile, int hduNum, int ptSize)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getLineDataAry(pt,pt2,plane, hdus,num,ptSize));
    }

    public static double[] getZAxisAryFromCube(ImagePt pt, File fitsFile, int hduNum, int ptSize)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getZAxisAry(pt,hdus,num,ptSize));
    }



    private static double valueFromFitsFile(ImageHDU hdu, int x, int y, int plane, int ptSize) throws IOException {
        Header header= hdu.getHeader();
        int naxis1= getNaxis1(header);
        int naxis2= getNaxis2(header);
        if (ptSize<1) ptSize= 1;
        else if (ptSize>5) ptSize= 5;
        int adjust= (int)Math.floor((ptSize-1) / 2.0);
        x= x - adjust;
        y= y - adjust;
        if (x<0) x= 0;
        if (y<0) y= 0;
        if (x+ptSize>=naxis1) x-= (x+ptSize-naxis1+1);
        if (y+ptSize>=naxis2) y-= (y+ptSize-naxis2+1);
        double[] doubleAry= (double[]) dataArrayFromFitsFile(hdu,x,y,ptSize,ptSize,plane,true);
        double aveValue= averageArray(doubleAry);
        return ImageStretch.getFluxStandard( aveValue, getBlankValue(header), getBscale(header), getBzero(header));
    }

    /**
     *
     * @param doDouble true for double array, false for float ary
     * @return an Object that will be a double array or an float array
     */
    public static Object dataArrayFromFitsFile(ImageHDU hdu, int x, int y, int width, int height, int plane, boolean doDouble) throws IOException {
        Header header= hdu.getHeader();
        int naxis= getNaxis(header);
        if (naxis==4 && getNaxis4(header)!=1) throw new IllegalArgumentException("naxis 4 must has naxis 4 as dimension 1");
        else if (naxis!=2 && naxis!=3 && naxis!=4) throw new IllegalArgumentException("only naxis 2 or 3 or 4 is supported");
        int[] loc= null;
        int[] tileSize= null;
        StandardImageTiler tiler= hdu.getTiler();
        switch (naxis) {
            case 2:
                loc= new int [] {y,x};
                tileSize= new int[] {height,width};
                break;
            case 3:
                loc= new int [] {plane,y,x};
                tileSize= new int[] {1,height,width};
                break;
            case 4:
                loc= new int [] {0, plane,y,x};
                tileSize= new int[] {1, 1,height,width};
                break;
        }
        Object value= tiler.getTile(loc, tileSize);
        return ArrayFuncs.convertArray(value, doDouble ? Double.TYPE : Float.TYPE, true);
    }


    public static double[] getZAxisAry(ImagePt pt, BasicHDU<?>[] hdus, int hduNum, int ptSize)
            throws FitsException, IOException {
        validateCubeAtHDU(hdus,hduNum);
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        Header header= hdu.getHeader();
        int zLen= getNaxis3(header);
        double[] retAry= new double[zLen];
        for(int i=0;i<zLen; i++) {
            retAry[i]=valueFromFitsFile(hdu,(int)pt.getX(), (int)pt.getY(),i,ptSize);
        }
        return retAry;
    }

    private static ImageHDU getImageHDU(BasicHDU<?>[] hdus, int idx) throws FitsException {
        if ( !(hdus[idx] instanceof ImageHDU) && !(hdus[idx] instanceof CompressedImageHDU) ) {
            throw new FitsException(idx + " is not a cube");
        }
        return (hdus[idx] instanceof CompressedImageHDU) ?
                        ((CompressedImageHDU) hdus[idx]).asImageHDU() : (ImageHDU) hdus[idx];
    }

    private static void validateCubeAtHDU(BasicHDU<?>[] hdus, int hduNum) throws FitsException {
        validateImageAtHDU(hdus,hduNum);
        BasicHDU<?> basicHDU= hdus[hduNum];
        Header header= basicHDU.getHeader();
        String hduNumStr= "HDU #"+hduNum;
        int nAxis= getNaxis(header);
        if (nAxis<3) throw new FitsException(hduNumStr + " is not a cube");
        if (nAxis==4 && getNaxis4(header)!=1) throw new FitsException(hduNumStr + " is not a cube, 4 axes");
    }

    private static void validateImageAtHDU(BasicHDU<?>[] hdus, int hduNum) throws FitsException {
        String hduNumStr= "HDU #"+hduNum;
        if (hduNum>=hdus.length) throw new FitsException("no "+hduNumStr);
        BasicHDU<?> basicHDU= hdus[hduNum];
        if ( !(basicHDU instanceof ImageHDU) && !(basicHDU instanceof CompressedImageHDU) ) {
            throw new FitsException(hduNumStr+ " is not a image HDU");
        }
    }


    public static double[] getPointDataAry(ImagePt[] ptAry, int plane, BasicHDU<?>[] hdus, int hduNum, int ptSize)
            throws FitsException, IOException {
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        double[] pts= new double[ptAry.length];
        for(int i=0;(i<ptAry.length);i++) {
            ImagePt pt= ptAry[i];
            pts[i] = valueFromFitsFile(hdu, (int)pt.getX(),(int)pt.getY(),plane,ptSize);
        }
        return pts;
    }



    public static double[] getLineDataAry(ImagePt pt1, ImagePt pt2, int plane, BasicHDU<?>[] hdus, int hduNum, int ptSize)
            throws FitsException, IOException {
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        double x1 = pt1.getX();
        double y1 = pt1.getY();
        double x2 = pt2.getX();
        double y2 = pt2.getY();

        // delta X and Y in image pixels
        double deltaX = Math.abs(pt2.getX() - pt1.getX());
        double deltaY = Math.abs(pt2.getY() - pt1.getY());
        double slope;
        double yIntercept;

        int x, y;
        if (deltaX > deltaY) {
            slope = (y2-y1)/(x2-x1);
            yIntercept = y1-slope*x1;

            int minX = (int)Math.min(x1, x2);
            int maxX = (int)Math.max(x1, x2) ;
            int n = (int)Math.rint(Math.ceil(maxX-minX))+1;
            double [] pts = new double[n];
            int idx = 0;
            for (x=minX; x<=maxX; x+=1) {
                y = (int)(slope*x + yIntercept);
                pts[idx] = valueFromFitsFile(hdu, x,y,plane,ptSize);
                idx++;
            }
            return pts;
        } else if (y1 != y2) {
            double  islope = (x2-x1)/(y2-y1);
            double xIntercept = x1-islope*y1;

            int minY = (int)Math.min(y1, y2);
            int maxY = (int)Math.max(y1, y2);
            int n = (int)Math.rint(Math.ceil(maxY - minY))+1;
            double [] pts = new double[n];

            int idx = 0;
            for (y=minY; y<=maxY; y+=1) {
                x = (int)(islope*y + xIntercept);
                pts[idx] = valueFromFitsFile(hdu, x,y,plane,ptSize);
                idx++;
            }
            return pts;
        }
        return null;
    }

    public static Class<?> getDataType(int bitPix){
        return switch (bitPix) {
            case 8 -> Byte.TYPE;
            case 16 -> Short.TYPE;
            case 32 -> Integer.TYPE;
            case 64 -> Long.TYPE;
            case -32 -> Float.TYPE;
            case -64 -> Double.TYPE;
            default -> null;
        };
    }


    public record ExtractionResults(int hduNum, String extName, double[] aryData, boolean refHDU, Header header) { }
}
