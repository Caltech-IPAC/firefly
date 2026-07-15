/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;

import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.core.Util.Try;

/**
 * @author Trey Roby
 */
class MaskEval implements FitsEvaluation.Eval {

    private final List<String> imageNames= Arrays.asList("IMAGE", "FLUX", "SCI");
    private final List<String> maskNames= Arrays.asList("MASK", "FLAGS");
    /**
     * This method attempts to find how data might be related in a multi-extension fits file. I expect it will grow
     * and become more advanced over time.
     * Currently it looks of the extension type marked 'IMAGE' and makes that the based. Any extensions marked 'MASK' or
     * 'VARIANCE' are related data.
     *
     * @param f     the fits file name
     * @param frAry the array of FitsRead objects that came from the file.
     * @return the data relations
     */
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU<?>[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        if (HDUs.length<2) return null;
        Header h = frAry[fitsReadIndex].getHeader();
        String extType = FitsReadUtil.getExtTypeOrName(h);
        extType= extType==null ? "" : extType.toUpperCase();

        if (imageNames.contains(extType)) {
            FitsRead baseFr= frAry[fitsReadIndex];
            List<RelatedData> relatedList = new ArrayList<>();
            for (int i = 0; (i < frAry.length); i++) {
                if (i != fitsReadIndex) {
                    if (isMask(frAry[i], baseFr)) {
                        RelatedData d = RelatedData.makeMaskRelatedData(
                                baseFr.getHduNumber(), f.getAbsolutePath(),
                                getMaskHeaders(frAry[i].getHeader()),
                                frAry[i].isCube(), frAry[i].getHduNumber(), "mask");
                        relatedList.add(d);
                    }
                    if (isVariance(frAry[i])) {
                        RelatedData d = RelatedData.makeImageOverlayRelatedData(f.getAbsolutePath(),
                                "variance", "Variance", i);
                        relatedList.add(d);
                    }
                }
            }
            return !relatedList.isEmpty() ? relatedList : null;
        } else {
            return null;
        }
    }

    private static List<RelatedData.MaskEntry> getMaskHeaders(Header header) {
        Cursor<String, HeaderCard> extraIter = header.iterator();

        boolean foundV1Style= false;
        while (extraIter.hasNext() && !foundV1Style) {
            var key = extraIter.next().getKey();
            foundV1Style= (key.startsWith("MP") || key.startsWith("HIERARCH.MP")) ;
        }
        return foundV1Style ? getMaskHeadersV1(header) : getMaskHeadersV2(header);
    }
    
    private static List<RelatedData.MaskEntry> getMaskHeadersV1(Header header) {
        var maskEntries= new ArrayList<RelatedData.MaskEntry>();
        Cursor<String, HeaderCard> extraIter = header.iterator();
        while (extraIter.hasNext()) {
            var hc = extraIter.next();
            var inKey= hc.getKey();
            if (inKey.startsWith("MP") || inKey.startsWith("HIERARCH.MP")) {
                var v= Try.it(() -> Integer.parseInt(hc.getValue())).getOrElse(-1);
                if (v>-1) {
                    String key= inKey.startsWith("HIERARCH.MP") ? inKey.substring(12) : inKey.substring(3);
                    maskEntries.add(new RelatedData.MaskEntry(key, v, ""));
                }
            }
        }
        return maskEntries;
    }

    private static List<RelatedData.MaskEntry> getMaskHeadersV2(Header header) {
        var maskEntries= new ArrayList<RelatedData.MaskEntry>();
        Cursor<String, HeaderCard> extraIter = header.iterator();
        while (extraIter.hasNext()) {
            var cnt= getMaskNameCnt(extraIter.next().getKey());
            if (cnt!=null)  {
                RelatedData.MaskEntry me= makeMaskEntry(header,cnt);
                if (me!=null) maskEntries.add(me);
            }
        }
        return maskEntries;
    }

    public static String getMaskNameCnt(String name) {
        if (name==null) return null;
        if (!name.startsWith("MSKN")) return null;
        var valPartStr=  name.substring("MSKN".length());
        var v= Try.it(() -> Integer.parseInt(valPartStr)).getOrElse(-1);
        return v>-1 ? valPartStr : null;
    }

    public static RelatedData.MaskEntry makeMaskEntry(Header header, String cntStr) {
        var name= header.getStringValue("MSKN"+cntStr);
        int v= header.getIntValue("MSKM"+cntStr,-1);
        var desc= header.getStringValue("MSKD"+cntStr,"");
        if (v==-1 || name==null) return null;
        return new RelatedData.MaskEntry(name,Integer.numberOfTrailingZeros(v),desc);
    }


    public boolean isMask(FitsRead maskFr, FitsRead baseFr) {
        boolean isCube= baseFr.isCube();
        int cubePlaneNumber=  baseFr.getPlaneNumber();
        if (!isCube && maskFr.isCube() && maskFr.getPlaneNumber()>0) return false;
        if (maskFr.getNaxis1()!=baseFr.getNaxis1() || maskFr.getNaxis2()!=baseFr.getNaxis2()) return false;
        var extType= FitsReadUtil.getExtTypeOrName(maskFr.getHeader());
        extType= extType==null ? "" : extType.toUpperCase();
        if (!maskNames.contains(extType)) return false;
        if (!maskFr.isCube()) return true;
        return (maskFr.getNaxisLength(3)==baseFr.getNaxisLength(3) && cubePlaneNumber==maskFr.getPlaneNumber());
    }

    public boolean isVariance(FitsRead fr) {
        return fr.getExtType().equalsIgnoreCase("VARIANCE");
    }

}
