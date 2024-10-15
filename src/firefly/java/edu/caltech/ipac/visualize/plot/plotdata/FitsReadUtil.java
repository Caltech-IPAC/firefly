/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageHeader;
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
import nom.tam.fits.UndefinedData;
import nom.tam.fits.UndefinedHDU;
import nom.tam.fits.header.Bitpix;
import nom.tam.image.StandardImageTiler;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.IOException;
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

    public static ImageData getImageData(BasicHDU<?> refHdu, float[] float1d) {
        Header h = refHdu.getHeader();
        int[] dims2 = new int[] {getNaxis1(h), getNaxis2(h)};
        float[][] fdata = (float[][]) ArrayFuncs.curl(float1d, dims2);
        Object data = ArrayFuncs.convertArray(fdata, getDataType(refHdu), true);
        return new ImageData(data);
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
            clonedHeader.addLine(iter.next().copy());
        }
        return clonedHeader;
    }


    public static boolean hasCompressedImageHDUS(BasicHDU<?>[] HDUs)  {
        return Arrays.stream(HDUs).anyMatch(h -> h instanceof CompressedImageHDU);
    }

    public static Header getTopFitsHeader(File f) {
        try (Fits fits= new Fits(f)) {
            return fits.getHDU(0).getHeader();
        } catch (IOException  e) {
            return null;
        }
    }

    public record UncompressFitsInfo(File file, BasicHDU<?>[] HDUs, Fits fits) {}

    public static UncompressFitsInfo createdUncompressVersionOfFile(BasicHDU<?>[] HDUs, File originalFile)
            throws IOException {
        String fBase= FileUtil.getBase(originalFile);
        String dir= originalFile.getParent();
        var gzType= FileUtil.isGZipFile(originalFile) ? "---gzip" : "";
        var hduType=  FitsReadUtil.hasCompressedImageHDUS(HDUs) ? "---hdu" : "";
        File retFile= new File(dir+"/"+ fBase+gzType+hduType+"-uncompressed"+".fits");
        Fits fits= new Fits();
        for (BasicHDU<?> hdu : HDUs) {
            fits.addHDU( hdu instanceof CompressedImageHDU ?  ((CompressedImageHDU) hdu).asImageHDU() : hdu );
        }
        fits.write(retFile);
        closeFits(fits);
        Fits retReadFits= new Fits(retFile);
        return new UncompressFitsInfo(retFile,fits.read(), retReadFits);
    }

    public static BasicHDU<?>[] getImageHDUArray(BasicHDU<?>[] HDUs, boolean onlyFireCubeHdu) {
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


        } //end j loop

        if (HDUList.isEmpty() && delayedExceptionMsg != null) {
            throw new FitsException(delayedExceptionMsg);
        }
        return HDUList.toArray(new BasicHDU<?>[0]);
    }



    private static void insertPositionIntoHeader(Header header, int pos, long hduOffset) {
        if (hduOffset < 0) hduOffset = 0;
        if (pos < 0) pos = 0;
        long headerSize = getHeaderSize(header);
        int bitpix = header.getIntValue("BITPIX", -1);
        header.addLine(new HeaderCard(SPOT_HS, headerSize, "Header block size on disk (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_EXT, pos, "Extension Number (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_OFF, hduOffset, "Extension Offset (added by Firefly)"));
        header.addLine(new HeaderCard(SPOT_BP, bitpix, "Original Bitpix value (added by Firefly)"));
        header.ensureCardSpace(1);
    }


    private static BasicHDU<?>[] splitFits3DCube(BasicHDU<?> inHdu, boolean onlyFirstCubeHdu) {
        ImageHDU hdu = (inHdu instanceof ImageHDU) ? (ImageHDU) inHdu : ((CompressedImageHDU) inHdu).asImageHDU();  // if we have to uncompress a cube it could take a long time
        BasicHDU<?>[] hduList = new BasicHDU<?>[hdu.getHeader().getIntValue("NAXIS3", 0)];

        for (int i = 0; i < hduList.length; i++) {
            if (onlyFirstCubeHdu && i>0) {
                hduList[i] = null;
            }
            else {
                hduList[i] = makeEmptyImageHDU(cloneHeaderFrom(hdu.getHeader()));
                //set the header pointer to the BITPIX location to add the new key. Without calling this line, the pointer is point
                //to the end of the Header, the SPOT_PL is added after the "END" key, which leads the image loading failure.
                hduList[i].getHeader().getIntValue("BITPIX", -1);
                hduList[i].getHeader().addLine(new HeaderCard(SPOT_PL, i, "Plane of FITS cube (added by Firefly)"));
                hduList[i].getHeader().ensureCardSpace(1);
            }

        }
        return hduList;

    }

    private static BasicHDU<?>[] splitFitsCube(BasicHDU<?> inHdu, boolean onlyFirstCubeHdu) {
        int naxis = inHdu.getHeader().getIntValue("NAXIS", -1);

        switch (naxis) {
            case 3 -> {
                return splitFits3DCube(inHdu, onlyFirstCubeHdu);
            }
            case 4 -> {
                ArrayList<BasicHDU<?>> hduListArr = new ArrayList<>();
                int naxis4 = inHdu.getHeader().getIntValue("NAXIS4", -1);
                if (naxis4 == 1) {
                    for (int i = 0; i < naxis4; i++) {
                        BasicHDU<?>[] hduList = splitFits3DCube(inHdu, onlyFirstCubeHdu);
                        if (onlyFirstCubeHdu) {
                            for (int j = 1; (j < hduList.length); j++) hduList[j] = null;
                        }
                        Collections.addAll(hduListArr, hduList);
                    }
                    return hduListArr.toArray(new BasicHDU<?>[0]);
                } else {
                    throw new IllegalArgumentException("naxis4>1 is not supported");

                }
            }
            default -> throw new IllegalArgumentException("naxis=" + naxis + " is not supported");
        }
    }


    /**
     * Get the world point location
     *
     * @param imHeader       the header
     * @param aCoordinateSys coordinate system
     * @return a world pt
     * @throws FitsException when something goes wrong
     */
    public static WorldPt getWorldPt(ImageHeader imHeader, CoordinateSys aCoordinateSys) throws FitsException {
        try {
            CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(imHeader.getJsys(), imHeader.file_equinox);
            double centerX = (imHeader.naxis1 + 1.0) / 2.0;
            double centerY = (imHeader.naxis2 + 1.0) / 2.0;
            WorldPt worldPt = imHeader.createProjection(inCoordinateSys).getWorldCoords(centerX - 1, centerY - 1);
            return VisUtil.convert(worldPt, aCoordinateSys);
        } catch (ProjectionException pe) {
            throw new FitsException("Could not rotate image: got ProjectionException: " + pe.getMessage());
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
        int naxis1 = getNaxis1(header);
        int naxis2 = getNaxis2(header);

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

    public static long getHeaderSize(Header header) {
        return header.getOriginalSize() > 0 ? header.getOriginalSize() : header.getSize();
    }


    public static ImageHDU makeImageHDU(Header newHeader, ImageData imageData) {
        var hdu= (ImageHDU) Fits.makeHDU(imageData);
        hdu.getHeader().updateLines(newHeader);
        return hdu;
    }

    public static ImageHDU makeEmptyImageHDU(Header newHeader) {
        return new ImageHDU(newHeader,  null);
    }

    public static void writeFitsFile(File outfile, FitsRead[] fitsReadAry, Fits refFits) throws IOException {
        Fits outputFits = new Fits();
        for (FitsRead fr : fitsReadAry) {
            BasicHDU<?> refHdu = refFits.getHDU(0);
            ImageHDU imageHDU = makeImageHDU(refHdu.getHeader(), FitsReadUtil.getImageData(refHdu, fr.getDataFloat()));
            outputFits.addHDU(imageHDU);
        }
        outputFits.write(outfile);
    }

    public static int getBitPix(Header h) {return h.getIntValue("BITPIX"); }
    public static int getNaxis(Header h) { return h.getIntValue("NAXIS", 0); }
    public static int getNaxis1(Header h) { return h.getIntValue("NAXIS1", 0); }
    public static int getNaxis2(Header h) { return h.getIntValue("NAXIS2", 0); }
    public static int getNaxis3(Header h) { return (getNaxis2(h) > 2) ? h.getIntValue("NAXIS3") : 1; }
    public static int getNaxis4(Header h) { return (getNaxis3(h) > 2) ? h.getIntValue("NAXIS4") : 1; }
    public static int getZNaxis1(Header h) { return h.getIntValue("ZNAXIS1", -1); }
    public static int getZNaxis2(Header h) { return h.getIntValue("ZNAXIS2", -1); }
    public static int getZNaxis3(Header h) { return h.getIntValue("ZNAXIS3",-1); }
    public static double getBscale(Header h) { return h.getDoubleValue("BSCALE", 1.0); }
    public static double getBzero(Header h) { return h.getDoubleValue("BZERO", 0.0); }
    public static double getBlankValue(Header h) {
        // blank value is only applicable to integer values (BITPIX > 0)
        return getBitPix(h) > 0 ? h.getDoubleValue("BLANK", Double.NaN) : Double.NaN;
    }

    public static String getBUnit(Header h) { return h.getStringValue("BUNIT", ""); }
    public static String getExtName(Header h) { return h.getStringValue("EXTNAME"); }
    public static String getExtType(Header h, String defVal) { return h.getStringValue("EXTTYPE",defVal); }
    public static String getUtype(Header h) { return h.getStringValue("UTYPE"); }
    public static String getExtNameOrType(Header h) { return getExtName(h)!=null ? getExtName(h) : getExtType(h,null);}




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
            if (fits!=null) fits.close();
        } catch (IOException ignore) {
        }
    }

    /**
     *
     * @return an Object that will be a double array or an float array
     */
    public static Object dataArrayFromFitsFile(ImageHDU hdu, int x, int y, int width, int height, int plane, Class<?> arrayType) throws IOException {
        Header header= hdu.getHeader();
        int naxis= getNaxis(header);
        if (naxis==4 && getNaxis4(header)!=1) throw new IllegalArgumentException("naxis 4 must has naxis 4 as dimension 1");
        else if (naxis!=2 && naxis!=3 && naxis!=4) throw new IllegalArgumentException("only naxis 2 or 3 or 4 is supported");
        int[] loc= null;
        int[] tileSize= null;
        StandardImageTiler tiler= hdu.getTiler();
        switch (naxis) {
            case 2 -> {
                loc = new int[]{y, x};
                tileSize = new int[]{height, width};
            }
            case 3 -> {
                loc = new int[]{plane, y, x};
                tileSize = new int[]{1, height, width};
            }
            case 4 -> {
                loc = new int[]{0, plane, y, x};
                tileSize = new int[]{1, 1, height, width};
            }
        }
        Object value= tiler.getTile(loc, tileSize);
        return ArrayFuncs.convertArray(value, arrayType, true);
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

    public static Class<?> getDataType(Bitpix bp){ return bp.getNumberType(); }

    public static Class<?> getDataType(BasicHDU<?> hdu) { return hdu.getBitpix().getNumberType(); }


}
