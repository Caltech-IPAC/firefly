/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;
/**
 * User: roby
 * Date: 7/13/18
 * Time: 10:39 AM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
class MaskEval implements FitsEvaluation.Eval {
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
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        if (HDUs.length<2) return null;
        String extType = frAry[fitsReadIndex].getExtType();
        if (extType.equalsIgnoreCase("IMAGE")) {
            FitsRead baseFr= frAry[fitsReadIndex];
            List<RelatedData> relatedList = new ArrayList<>();
            for (int i = 0; (i < frAry.length); i++) {
                if (i != fitsReadIndex) {
                    if (isMask(frAry[i], baseFr.getNaxis1(), baseFr.getNaxis2())) {
                        RelatedData d = RelatedData.makeMaskRelatedData(
                                baseFr.getHduNumber(),
                                f.getAbsolutePath(),
                                getMaskHeaders(frAry[i].getHeader()), i, "mask");
                        relatedList.add(d);
                    }
                    if (isVariance(frAry[i])) {
                        RelatedData d = RelatedData.makeImageOverlayRelatedData(f.getAbsolutePath(),
                                "variance", "Variance", i);
                        relatedList.add(d);
                    }
                }
            }
            return relatedList.size() > 0 ? relatedList : null;
        } else {
            return null;
        }
    }

    private static Map<String, String> getMaskHeaders(Header header) {
        Map<String, String> maskHeaders = new HashMap<>(23);
        HeaderCard hc;
        Cursor extraIter = header.iterator();
        for (; extraIter.hasNext(); ) {
            hc = (HeaderCard) extraIter.next();
            if (hc.getKey().startsWith("MP") || hc.getKey().startsWith("HIERARCH.MP")) {
                maskHeaders.put(hc.getKey(), hc.getValue());
            }
        }
        return maskHeaders;
    }

    public boolean isMask(FitsRead fr, int naxis1, int naxis2) {
        if (fr.isCube() && fr.getPlaneNumber()>0) return false;
        return (fr.getNaxis1()==naxis1 && fr.getNaxis2()==naxis2 && fr.getExtType().equalsIgnoreCase("MASK"));
    }

    public boolean isVariance(FitsRead fr) {
        return fr.getExtType().equalsIgnoreCase("VARIANCE");
    }

}
