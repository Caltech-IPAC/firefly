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
import java.util.function.Function;

/**
 * @author Trey Roby
 */
public class FitsExtract {

    public enum CombineType {
        AVG,
        SUM,
        OR,
        SQRT_SUM_N2,            // SQRT(SUM(pixel_values^2))             w/ SUM,UNCERTAINTY
        INVERSE_SUM_INVERSE_N,  // 1.0 / SUM(1.0/pixel_values)           w/ SUM,WEIGHT
        SQRT_SUM_SQ_AVG,        // SQRT(SUM(pixel_values^2))/n_pixels  w/ AVG,UNCERTAINTY
        SUM_DIV_N2,             // SUM(pixel_values) / n_pixels^2,        w/ AVG,VARIANCE
        N2_DIV_SUM_INVERSE_N    // n_pixels^2 / SUM(1.0/pixel_values),   w/ AVG,WEIGHT
    }
    public enum SecondaryHduCombine {AUTO, SAME}

    public static final List<String> uncertainty= List.of("UNCERTAINTY", "ERROR", "ERR", "UNC", "SIGMA", "NOISE", "UNCRT", "STDDEV");
    public static final List<String> variance= List.of("VARIANCE", "VAR", "SIGMA2", "VARMAP", "ERRSQR");
    public static final List<String> flags= List.of("FLAGS", "MASK", "MSK", "DQ", "FLAG", "BITMASK", "QUALITY", "BADPIX", "BADMAP", "PIXELMASK");
    public static final List<String> weight= List.of("WEIGHT", "WHT", "WT", "WEIGHTMAP", "WEIGHTS");

    public static final List<String> allHdus= new ArrayList<>();

    static {
        allHdus.addAll(uncertainty);
        allHdus.addAll(variance);
        allHdus.addAll(flags);
        allHdus.addAll(weight);
    }

    private static Number combineArray(List<Number> aryList, CombineType ct, Class<?> type) {
        if (aryList.isEmpty()) return Double.NaN;
        if (aryList.size() == 1) return aryList.getFirst();
        var realCt=  (ct==CombineType.OR && (type==Float.TYPE || type==Double.TYPE)) ? CombineType.AVG : ct;

        return switch (realCt) {
            case AVG -> doResult(sumPix(aryList), (r) -> r.sum/r.cnt);
            case SUM -> doResult(sumPix(aryList), (r) -> r.sum);
            case OR -> logicalOr(aryList,type);
            case SQRT_SUM_N2 -> doResult(sumPixSq(aryList), (r) -> Math.sqrt(r.sum));
            case INVERSE_SUM_INVERSE_N -> doResult(sumPixInverse(aryList), (r) -> 1/r.sum);
            case SQRT_SUM_SQ_AVG -> doResult(sumPixSq(aryList), (r) -> Math.sqrt(r.sum)/r.cnt);
            case SUM_DIV_N2 -> doResult(sumPix(aryList), (r) -> r.sum/(r.cnt*r.cnt));
            case N2_DIV_SUM_INVERSE_N -> doResult(sumPixInverse(aryList), (r) -> (r.cnt*r.cnt)/r.sum);
        };
    }

    private static Number logicalOr(List<Number> aryList, Class<?> type) {
        double cnt = 0;
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

    private static Number doResult(SumResult r, Function<SumResult, Number> f) {
        return r.cnt > 0 ? f.apply(r) : Double.NaN;
    }

    private static Class<?> arrayTypeFromBitpix(Header header) {
        return switch (FitsReadUtil.getBitPix(header)) {
            case -32 -> Float.TYPE;
            case 8, 16, 32 -> Integer.TYPE;
            case 64 -> Long.TYPE;
            default -> Double.TYPE;
        };
    }

    private static SumResult sumPixSq(List<Number> aryList)  {
        return doSum(aryList, (v) -> v.doubleValue()*v.doubleValue());
    }

    private static SumResult sumPix(List<Number> aryList)  {
        return doSum(aryList, Number::doubleValue);
    }

    private static SumResult sumPixInverse(List<Number> aryList)  {
        return doSum(aryList, (v) -> 1/v.doubleValue());
    }

    private static SumResult doSum(List<Number> aryList, Function<Number, Double> f)  {
        double sum = 0;
        int cnt = 0;
        for (Number v : aryList) {
            if (!isNaN(v)) {
                sum+= f.apply(v);
                cnt++;
            }
        }
        return new SumResult(cnt,sum);
    }

    record SumResult(int cnt, double sum) {}

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
    static Number valueFromFitsFile(ImageHDU hdu, int x, int y, int plane, int ptSizeX, int ptSizeY, CombineType ct) throws IOException {
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
        boolean outOfImage= x < 0 || y < 0 || x+ptSizeX>=naxis1+1 ||  y+ptSizeY>=naxis2+1;

        Class<?> arrayType= arrayTypeFromBitpix(header);
        var blankValue= FitsReadUtil.getBlankValue(header);

        if (outOfImage) {
            return switch (arrayType.toString()) {
                case "float" -> Float.NaN;
                case "int" -> (int)blankValue;
                case "long" -> (long)blankValue;
                default -> Double.NaN;
            };
        }

        Object ary= FitsReadUtil.dataArrayFromFitsFile(hdu,x,y,ptSizeX,ptSizeY,plane,arrayType);
        Number aveValue= combineArray(objToNumberAry(ary,arrayType), ct, arrayType);

        var bscale= FitsReadUtil.getBscale(header);
        var bzero= FitsReadUtil.getBzero(header);

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

    public static List<Number> getPointDataAry(ImagePt[] ptAry, int plane, BasicHDU<?>[] hdus, int hduNum, int ptSizeX, int ptSizeY, CombineType ct)
            throws FitsException, IOException {
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        var pts= new ArrayList<Number>(ptAry.length);
        for (ImagePt pt : ptAry) {
            pts.add(valueFromFitsFile(hdu, (int) pt.getX(), (int) pt.getY(), plane, ptSizeX, ptSizeY, ct));
        }
        return pts;
    }

    public static List<Number> getLineDataAry(ImagePt pt1, ImagePt pt2, int plane, BasicHDU<?>[] hdus,
                                              int hduNum, int ptSize, CombineType ct)
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
            int n = maxX - minX +1;
            List<Number> pts= new ArrayList<>(n);
            for (x=minX; x<=maxX; x+=1) {
                y = (int)(slope*x + yIntercept);
                pts.add(valueFromFitsFile(hdu, x,y,plane,ptSize,ptSize,ct));
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
                pts.add(valueFromFitsFile(hdu, x,y,plane,ptSize,ptSize,ct));
            }
            return pts;
        }
        return null;
    }

    public static List<ExtractionResults> extractFromRelatedHDUs(File fitsFile, int refHduNum,
                                                                 boolean allMatchingHDUs,
                                                                 CombineType ct,
                                                                 SecondaryHduCombine secondaryCombine,
                                                                 Extractor extractor)
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
                    CombineType newCt= getCombineType(h,i,ct,secondaryCombine,refHduNum);
                    if (FitsReadUtil.getNaxis(h) == dims && FitsReadUtil.getNaxis1(h) == xLen && FitsReadUtil.getNaxis2(h) == yLen && FitsReadUtil.getNaxis3(h) == zLen) {
                        var list = extractor.extractAry(hdus, i, newCt);
                        retList.add(new ExtractionResults(i, FitsReadUtil.getExtNameOrType(h), list, i == refHduNum, h,newCt));
                    }
                }
            } else {
                var list = extractor.extractAry(hdus, refHduNum, ct);
                retList.add(new ExtractionResults(refHduNum, FitsReadUtil.getExtNameOrType(refHeader), list,true, refHeader,ct));
            }
            return retList;
        }
    }

    private static CombineType getCombineType(Header h, int hduNum, CombineType primeCt, SecondaryHduCombine secondaryCombine, int refHduNum) {

        var arrayType= arrayTypeFromBitpix(h);
        var isFloat= (arrayType==Float.TYPE || arrayType==Double.TYPE);
        if (hduNum==refHduNum || secondaryCombine==SecondaryHduCombine.SAME) {
            if (primeCt==CombineType.OR && !isFloat) return CombineType.AVG;
            return primeCt;
        }

        String type= FitsReadUtil.getExtTypeOrName(h);

        if (type==null || !allHdus.contains(type)) {
            return (primeCt==CombineType.OR && !isFloat) ? CombineType.AVG : primeCt;
        }
        if (uncertainty.contains(type.toUpperCase())) {
            return primeCt==CombineType.SUM ? CombineType.SQRT_SUM_N2  : CombineType.SQRT_SUM_SQ_AVG;
        }
        if (variance.contains(type.toUpperCase())) {
            return primeCt==CombineType.SUM ? CombineType.SUM : CombineType.SUM_DIV_N2;
        }
        if (flags.contains(type.toUpperCase())) {
            if (isFloat) return primeCt==CombineType.SUM ? CombineType.SUM : CombineType.AVG;
            return CombineType.OR;
        }
        if (weight.contains(type.toUpperCase())) {
            return primeCt==CombineType.SUM ? CombineType.INVERSE_SUM_INVERSE_N: CombineType.N2_DIV_SUM_INVERSE_N     ;
        }
        if (primeCt==CombineType.OR && !isFloat) {
            return CombineType.AVG;
        }
        return primeCt;
    }

    public static List<Number> extractFromHDU(File fitsFile, int hduNum, CombineType ct, Extractor extractor)
            throws FitsException, IOException {
        try (Fits fits= new Fits(fitsFile)) {
            return extractor.extractAry(fits.read(), hduNum, ct);
        }
    }





    public static List<ExtractionResults> getAllPointsFromRelatedHDUs(ImagePt[] ptAry, File fitsFile,
                                                                      int refHduNum, int plane,
                                                                      boolean allMatchingHDUs, int ptSizeX, int ptSizeY,
                                                                      CombineType ct, SecondaryHduCombine secondaryCombine)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs, ct, secondaryCombine,
                (hdus, hduNum, newCt) -> getPointDataAry(ptAry, plane, hdus, hduNum, ptSizeX, ptSizeY,  newCt));
    }

    public static List<ExtractionResults> getAllLinesFromRelatedHDUs(ImagePt pt, ImagePt pt2, File fitsFile,
                                                                     int refHduNum, int plane,
                                                                     boolean allMatchingHDUs, int ptSize,
                                                                     CombineType ct)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs, ct, SecondaryHduCombine.AUTO,
                (hdus, hduNum, newCt) -> getLineDataAry(pt, pt2, plane, hdus, hduNum, ptSize, newCt));
    }

    public static List<ExtractionResults> getAllZAxisAryFromRelatedCubes(ImagePt pt, File fitsFile, int refHduNum,
                                                                         boolean allMatchingHDUs, int ptSize,
                                                                         CombineType ct, SecondaryHduCombine secondaryCombine)
            throws FitsException, IOException {
        return extractFromRelatedHDUs(fitsFile, refHduNum, allMatchingHDUs, ct, secondaryCombine,
                (hdus,hduNum, newCt) -> getZAxisAry(pt,hdus,hduNum,ptSize,newCt) );
    }

    public static List<Number> getPointDataAryFromFile(ImagePt[] ptAry, int plane, File fitsFile, int hduNum,
                                                       int ptSizeX, int ptSizeY, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, ct,
                (hdus,num, newCt) -> getPointDataAry(ptAry,plane, hdus,num,ptSizeX, ptSizeY, newCt));
    }

    public static List<Number> getLineDataAryFromFile(ImagePt pt, ImagePt pt2, int plane, File fitsFile, int hduNum,
                                                      int ptSize, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, ct,
                (hdus,num, newCt) -> getLineDataAry(pt,pt2,plane, hdus,num,ptSize,newCt));
    }

    public static List<Number> getZAxisAryFromCube(ImagePt pt, File fitsFile, int hduNum, int ptSize, CombineType ct)
            throws FitsException, IOException {
        return extractFromHDU(fitsFile,hduNum, ct,
                (hdus,num, newCt) -> getZAxisAry(pt,hdus,num,ptSize,newCt));
    }

    public static List<Number> getZAxisAry(ImagePt pt, BasicHDU<?>[] hdus, int hduNum, int ptSize, CombineType ct)
            throws FitsException, IOException {
        validateCubeAtHDU(hdus,hduNum);
        ImageHDU hdu= getImageHDU(hdus,hduNum);
        Header header= hdu.getHeader();
        int zLen= FitsReadUtil.getNaxis3(header);
        List<Number> retList= new ArrayList<>(zLen);
        for(int i=0;i<zLen; i++) {
            retList.add(valueFromFitsFile(hdu,(int)pt.getX(), (int)pt.getY(),i,ptSize, ptSize, ct));
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

    public interface Extractor { List<Number> extractAry(BasicHDU<?>[] hdus, int hduNum, CombineType ct) throws FitsException, IOException; }

    public record ExtractionResults(int hduNum, String extName, List<Number> aryData, boolean refHDU, Header header, CombineType ct) { }
}
