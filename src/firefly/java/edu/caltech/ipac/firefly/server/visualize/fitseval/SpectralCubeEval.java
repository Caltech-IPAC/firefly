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
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
class SpectralCubeEval implements FitsEvaluation.Eval {
    @Override
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        int idx= findWaveTabHDU(HDUs);
        if (idx==-1)  return null;
        Map<String,String> params= new HashMap<>(10);
        params.put("id", "IpacTableFromSource");
        params.put("startIdx", "0");
        params.put("pageSize", "30000");
        params.put("tbl_index", idx+"");
        params.put("source", ServerContext.replaceWithPrefix(f));
        return Collections.singletonList(
                RelatedData.makeWavelengthTabularRelatedData(params, "WAVE-TAB", "Table Wavelength information"));
    }


    public static int findWaveTabHDU(BasicHDU[] HDUs) {
        int tabHduIdx= -1;
        for(int i=0; (i<HDUs.length); i++) {
            tabHduIdx= findWaveTabHDU(HDUs,i);
            if (tabHduIdx>-1) break;
        }
        return tabHduIdx;
    }

    /**
     * NOTE:  HDUs here is the array of all HDUs in the FITs file.
     * For each image HDU, look for its coressonding BinaryTableHDU.  This only applies for WAVE-TAB case
     * @param HDUs
     * @param hduIdx
     * @return
     */
    public static int findWaveTabHDU(BasicHDU[] HDUs, int hduIdx) {

        if (HDUs.length<2) return -1;
        BasicHDU hdu= HDUs[hduIdx];
        Header header= hdu.getHeader();
        String xtensionPrim= header.getStringValue("XTENSION");
        if (xtensionPrim!=null && xtensionPrim.equals("BINTABLE")) return -1;
        String ctype= header.getStringValue("CTYPE3");
        if (!"WAVE-TAB".equals(ctype)) return -1;
        String waveHDUName= header.getStringValue("PS3_0");
        if  (waveHDUName==null) return -1;
        //The hdu= HDUs[hduIdx] is the an image HDU, it has a corresponding BinaryTableHDU
        //that contains the WAVE-TAB information. The loop below, is to find the index number of the BinaryTableHDU.
        for(int i=0; i<HDUs.length; i++) {
            if (i!=hduIdx) {
                Header hIHeader= HDUs[i].getHeader();
                String xtension= hIHeader.getStringValue("XTENSION");
                String extName= hIHeader.getStringValue("EXTNAME");
                if ("BINTABLE".equals(xtension) && waveHDUName.equals(extName)) {
                    return i;
                }

            }
        }
        return -1;
    }
}
