package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.visualize.plot.ImagePt;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FitsExtract {

    public enum CombineType {AVG, SUM, OR}

    private static  Number combineArray(List<Number> aryList, CombineType ct, Class<?> type) {
        if (aryList.isEmpty()) return Double.NaN;
        if (aryList.size() == 1) return aryList.get(0);
        double cnt = 0;
        var realCt=  (ct==CombineType.OR && (type==Float.TYPE || type==Double.TYPE)) ? CombineType.AVG : ct;
        if (realCt==CombineType.AVG || realCt==CombineType.SUM) {
            double sum = 0;
            for (Number v : aryList) {
                if (!isNaN(v)) {
                    sum += v.doubleValue();
                    cnt++;
                }
            }
            var result= (cnt==0||ct==CombineType.SUM) ? sum : sum/cnt;
            return cnt > 0 ? result : Double.NaN;
        } else if (ct == CombineType.OR) {
            long anded=0;
            for (Number v : aryList) {
                if (!isNaN(v)) {
                    anded |= v.longValue();
                    cnt++;
                }
            }
            Number n;
            if (type==Long.TYPE) n= anded;
            else n= (int)anded;
            return cnt > 0 ? n : Double.NaN;
        }
        else  {
            return Double.NaN;
        }
    }


    private static Number getNan(Number v) {
        if (v instanceof Double) return Double.NaN;
        if (v instanceof Float) return Float.NaN;
        return Double.NaN;
    }
    private static boolean isNaN(Number v) {
        if (v instanceof Double d) return d.isNaN();
        if (v instanceof Float f) return f.isNaN();
        return false;
    }

    private static List<Number> objToNumberAry(Object obj, Class<?> type) {
        List<Number> list= new ArrayList<>();

        if (type == Double.TYPE) {
            double[] ary= (double[])obj;
            for (double v : ary) list.add(v);
        }
        else if (type == Float.TYPE) {
            float[] ary= (float[])obj;
            for (float v : ary) list.add(v);
        }
        else if (type == Integer.TYPE) {
            int[] ary= (int[])obj;
            for (int v : ary) list.add(v);
        }
        else if (type == Long.TYPE) {
            long[] ary= (long[])obj;
            for (long v : ary) list.add(v);
        }

        return list;
    }



    /**
     *
     * @throws IOException if it can't read the fits file
     */
    static Number valueFromFitsFile(ImageHDU hdu, int x, int y, int plane, int ptSizeX, int ptSizeY, CombineType ct, boolean primaryHdu) throws IOException {
        Header header= hdu.getHeader();
        int naxis1= FitsReadUtil.getNaxis1(header);
        int naxis2= FitsReadUtil.getNaxis2(header);
        int bitpix= FitsReadUtil.getBitPix(header);
        if (ptSizeX<1) ptSizeX= 1;
        else if (ptSizeX>5) ptSizeX= 5;
        if (ptSizeY<1) ptSizeY= 1;
        else if (ptSizeY>5) ptSizeY= 5;
        int adjustX= (int)Math.floor((ptSizeX-1) / 2.0);
        int adjustY= (int)Math.floor((ptSizeY-1) / 2.0);
        x= x - adjustX;
        y= y - adjustY;
        if (x<0) x= 0;
        if (y<0) y= 0;

        if (x+ptSizeX>=naxis1) x-= (x+ptSizeX-naxis1+1);
        if (y+ptSizeY>=naxis2) y-= (y+ptSizeY-naxis2+1);
        if (x<0) x= 0;
        if (y<0) y= 0;

        Class<?> arrayType= switch (bitpix) {
            case -32 -> Float.TYPE;
            case 8, 16, 32 -> Integer.TYPE;
            case 64 -> Long.TYPE;
            default -> Double.TYPE;
        };

        if (!primaryHdu && (arrayType==Integer.TYPE || arrayType==Long.TYPE)) {
           ct= CombineType.OR;
        }
        Object ary= FitsReadUtil.dataArrayFromFitsFile(hdu,x,y,ptSizeX,ptSizeY,plane,arrayType);
        Number aveValue= combineArray(objToNumberAry(ary,arrayType), ct, arrayType);

        var bscale= FitsReadUtil.getBscale(header);
        var bzero= FitsReadUtil.getBzero(header);
        var blankValue= FitsReadUtil.getBlankValue(header);

        if (bscale==1.0D && bzero==0D && !isNaN(aveValue) && aveValue.doubleValue()!=blankValue) return aveValue;
        if (arrayType==Float.TYPE && Float.isNaN(aveValue.floatValue())) return Float.NaN;
        if (arrayType==Double.TYPE && Double.isNaN(aveValue.doubleValue())) return Double.NaN;

        double newValue= ImageStretch.getFluxStandard( aveValue.doubleValue(), blankValue, bscale, bzero, bitpix);
        if (Double.isNaN(newValue)) return newValue;

        return switch (arrayType.toString()) {
            case "float" -> (float)newValue;
            case "int" -> (int)newValue;
            case "long" -> (bscale==1.0D) ? (long)newValue : aveValue.longValue() + bzero;
            default -> newValue;
        };
    }

    static ImageHDU getImageHDU(BasicHDU<?>[] hdus, int idx) throws FitsException {
        if ( !(hdus[idx] instanceof ImageHDU) && !(hdus[idx] instanceof CompressedImageHDU) ) {
            throw new FitsException(idx + " is not a cube");
        }
        return (hdus[idx] instanceof CompressedImageHDU cHDU) ? cHDU.asImageHDU() : (ImageHDU) hdus[idx];
    }

    public static List<Number> getPointDataAry(ImagePt[] ptAry, int plane, BasicHDU<?>[] hdus, int hduNum, int refHduNum, int ptSizeX, int ptSizeY, CombineType ct)
            throws FitsException, IOException {
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        boolean primaryHdu= hduNum==refHduNum;
        var pts= new ArrayList<Number>(ptAry.length);
        for (ImagePt pt : ptAry) {
            pts.add(valueFromFitsFile(hdu, (int) pt.getX(), (int) pt.getY(), plane, ptSizeX, ptSizeY, ct, primaryHdu));
        }
        return pts;
    }

    public static List<Number> getLineDataAry(ImagePt pt1, ImagePt pt2, int plane, BasicHDU<?>[] hdus,
                                              int hduNum, int refHduNum, int ptSize, CombineType ct)
            throws FitsException, IOException {
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        boolean primaryHdu= hduNum==refHduNum;
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
            int n = maxX - minX +1;
            List<Number> pts= new ArrayList<>(n);
            for (x=minX; x<=maxX; x+=1) {
                y = (int)(slope*x + yIntercept);
                pts.add(valueFromFitsFile(hdu, x,y,plane,ptSize,ptSize,ct,primaryHdu));
            }
            return pts;
        } else if (y1 != y2) {
            double  islope = (x2-x1)/(y2-y1);
            double xIntercept = x1-islope*y1;

            int minY = (int)Math.min(y1, y2);
            int maxY = (int)Math.max(y1, y2);
            int n = maxY - minY +1;
            List<Number> pts= new ArrayList<>(n);

            for (y=minY; y<=maxY; y+=1) {
                x = (int)(islope*y + xIntercept);
                pts.add(valueFromFitsFile(hdu, x,y,plane,ptSize,ptSize,ct, primaryHdu));
            }
            return pts;
        }
        return null;
    }

    public static List<ExtractionResults> extractFromRelatedHDUs(File fitsFile, int refHduNum,
                                                                 boolean allMatchingHDUs, Extractor extractor)
            throws FitsException, IOException {
        try (Fits fits = new Fits(fitsFile)) {
            BasicHDU<?>[] hdus = fits.read();
            BasicHDU<?> hdu = hdus[refHduNum];
            validateImageAtHDU(hdus, refHduNum);
            Header refHeader = hdu.getHeader();
            int dims = FitsReadUtil.getNaxis(refHeader);
            int xLen = FitsReadUtil.getNaxis1(refHeader);
            int yLen = FitsReadUtil.getNaxis2(refHeader);
            int zLen = FitsReadUtil.getNaxis3(refHeader);
            List<ExtractionResults> retList = new ArrayList<>();

            if (allMatchingHDUs) {
                for (int i = 0; (i < hdus.length); i++) {
                    Header h = hdus[i].getHeader();
                    if (FitsReadUtil.getNaxis(h) == dims && FitsReadUtil.getNaxis1(h) == xLen && FitsReadUtil.getNaxis2(h) == yLen && FitsReadUtil.getNaxis3(h) == zLen) {
                        var list = extractor.extractAry(hdus, i);
                        retList.add(new ExtractionResults(i, FitsReadUtil.getExtNameOrType(h), list, i == refHduNum, h));
                    }
                }
            } else {
                var list = extractor.extractAry(hdus, refHduNum);
                retList.add(new ExtractionResults(refHduNum, FitsReadUtil.getExtNameOrType(refHeader), list,true, refHeader));
            }
            return retList;
        }
    }

    public static List<Number> extractFromHDU(File fitsFile, int hduNum, Extractor extractor)
            throws FitsException, IOException {
        try (Fits fits= new Fits(fitsFile)) {
            return extractor.extractAry(fits.read(), hduNum);
        }
    }

    public static List<ExtractionResults> getAllPointsFromRelatedHDUs(ImagePt[] ptAry, File fitsFile,
                                                                      int refHduNum, int plane,
                                                                      boolean allMatchingHDUs, int ptSizeX, int ptSizeY,
                                                                      CombineType ct)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus, hduNum) -> getPointDataAry(ptAry, plane, hdus, hduNum, refHduNum, ptSizeX, ptSizeY,  ct));
    }

    public static List<ExtractionResults> getAllLinesFromRelatedHDUs(ImagePt pt, ImagePt pt2, File fitsFile,
                                                                     int refHduNum, int plane,
                                                                     boolean allMatchingHDUs, int ptSize,
                                                                     CombineType ct)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus, hduNum) -> getLineDataAry(pt, pt2, plane, hdus, hduNum, refHduNum, ptSize, ct));
    }

    public static List<ExtractionResults> getAllZAxisAryFromRelatedCubes(ImagePt pt, File fitsFile, int refHduNum,
                                                                         boolean allMatchingHDUs, int ptSize,
                                                                         CombineType ct)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs,
                (hdus,hduNum) -> getZAxisAry(pt,hdus,hduNum,refHduNum,ptSize,ct) );
    }

    public static List<Number> getPointDataAryFromFile(ImagePt[] ptAry, int plane, File fitsFile, int hduNum, int refHduNum,
                                                       int ptSizeX, int ptSizeY, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getPointDataAry(ptAry,plane, hdus,num,refHduNum, ptSizeX, ptSizeY, ct));
    }

    public static List<Number> getLineDataAryFromFile(ImagePt pt, ImagePt pt2, int plane, File fitsFile, int hduNum, int refHduNum,
                                                      int ptSize, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getLineDataAry(pt,pt2,plane, hdus,num,refHduNum,ptSize,ct));
    }

    public static List<Number> getZAxisAryFromCube(ImagePt pt, File fitsFile, int hduNum, int ptSize, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, (hdus,num) -> getZAxisAry(pt,hdus,num,hduNum,ptSize,ct));
    }

    public static List<Number> getZAxisAry(ImagePt pt, BasicHDU<?>[] hdus, int hduNum, int refHduNum, int ptSize, CombineType ct)
            throws FitsException, IOException {
        validateCubeAtHDU(hdus,hduNum);
        boolean primaryHdu= hduNum==refHduNum;
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        Header header= hdu.getHeader();
        int zLen= FitsReadUtil.getNaxis3(header);
        List<Number> retList= new ArrayList<>(zLen);
        for(int i=0;i<zLen; i++) {
            retList.add(valueFromFitsFile(hdu,(int)pt.getX(), (int)pt.getY(),i,ptSize, ptSize, ct,primaryHdu));
        }
        return retList;
    }

    private static void validateCubeAtHDU(BasicHDU<?>[] hdus, int hduNum) throws FitsException {
        validateImageAtHDU(hdus,hduNum);
        BasicHDU<?> basicHDU= hdus[hduNum];
        Header header= basicHDU.getHeader();
        String hduNumStr= "HDU #"+hduNum;
        int nAxis= FitsReadUtil.getNaxis(header);
        if (nAxis<3) throw new FitsException(hduNumStr + " is not a cube");
        if (nAxis==4 && FitsReadUtil.getNaxis4(header)!=1) throw new FitsException(hduNumStr + " is not a cube, 4 axes");
    }

    private static void validateImageAtHDU(BasicHDU<?>[] hdus, int hduNum) throws FitsException {
        String hduNumStr= "HDU #"+hduNum;
        if (hduNum>=hdus.length) throw new FitsException("no "+hduNumStr);
        BasicHDU<?> basicHDU= hdus[hduNum];
        if ( !(basicHDU instanceof ImageHDU) && !(basicHDU instanceof CompressedImageHDU) ) {
            throw new FitsException(hduNumStr+ " is not a image HDU");
        }
    }

    public interface Extractor { List<Number> extractAry(BasicHDU<?>[] hdus, int hduNum) throws FitsException, IOException; }

    public record ExtractionResults(int hduNum, String extName, List<Number> aryData, boolean refHDU, Header header) { }
}
