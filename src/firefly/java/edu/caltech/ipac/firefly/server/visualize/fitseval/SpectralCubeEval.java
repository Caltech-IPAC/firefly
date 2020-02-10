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
import java.util.*;
import java.util.stream.Collectors;

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
     * If a FITS file contains multiple XTENSION HDUs (headerdata
     * units) with the specified EXTNAME, EXTLEVEL, and
     * EXTVER, then the result of the WCS table lookup is undefined.
     * If the specified FITS BINTABLE contains no column, or multiple
     * columns, with the specified TTYPEn, then the result of the
     * WCS table lookup is undefined. The specified FITS BINTABLE
     * must contain only one row.
     * No units conversions are to
     * @return
     */
    private static boolean isValidWCSForTab(BasicHDU[] HDUs){

        List<String> extNames = new ArrayList<String>();
        List<Integer> extLevels= new ArrayList<Integer>();
        List<Integer> extVers  = new ArrayList<Integer>();


        for (int i=0; i<HDUs.length; i++){
            Header header= HDUs[i].getHeader();
            String xtension= header.getStringValue("XTENSION");
            if ("BINTABLE".equals(xtension)) {
                extNames.add(header.getStringValue("EXTNAME"));
                extLevels.add( header.getIntValue("EXTLEVEL"));
                extVers.add( header.getIntValue("EXTVER"));

            }
        }


        for (int i=0; i<extNames.size(); i++){
            for (int j=i+1; j<extNames.size();j++){
                if (extNames.get(i) == extNames.get(j) &&
                   extLevels.get(i) == extLevels.get(j) &&
                   extVers.get(i)   == extVers.get(j) ){
                    return false;
                }
            }
        }


        return true;
    }
    /**
     * NOTE:  HDUs here is the array of all HDUs in the FITs file.
     *        For each image HDU, look for its corresponding BinaryTableHDU.  This only applies for WAVE-TAB case.
     *        The -TAB implementation is more complicated than most other WCS conventions because the coordinate system
     *        is not completely defined by keywords in a single FITS header.
     *        The necessary WCS parameters that are in general distributed over two FITS HDUs and in the body
     *        of the WCS extension table.
     * @param HDUs
     * @param hduIdx
     * @return
     */
    public static int findWaveTabHDU(BasicHDU[] HDUs, int hduIdx) {

        if (HDUs.length<2) return -1;


        BasicHDU hdu= HDUs[hduIdx];
        Header header= hdu.getHeader();
        String xtensionPrim= header.getStringValue("XTENSION");
        if (xtensionPrim!=null && "BINTABLE".equals(xtensionPrim) ) return -1;
        String ctype= header.getStringValue("CTYPE3");
        if (!"WAVE-TAB".equals(ctype)) return -1;

        //If it is WAVE-TAB, validate it first
        if (!isValidWCSForTab(HDUs)) return -1;
        String waveHDUName= header.getStringValue("PS3_0");
        int waveExtVer= header.containsKey("PV3_1") ? header.getIntValue("PV3_1"): 1;
        int waveExtLevel=header.containsKey("PV3_2") ? header.getIntValue("PV3_2"):1;

        ////When PS3_0 is undefined, use the binary table whose name starts with "WCS-".
        if (waveHDUName==null){
            for(int i=0; i<HDUs.length; i++) {
                if (i!=hduIdx) {
                    Header hIHeader= HDUs[i].getHeader();
                    String xtension= hIHeader.getStringValue("XTENSION");
                    String extName= hIHeader.getStringValue("EXTNAME");
                    if ("BINTABLE".equals(xtension) && extName.startsWith("WCS-")) {
                        return i;
                    }
                }
            }

        }

        //The hdu= HDUs[hduIdx] is the an image HDU, it has a corresponding BinaryTableHDU
        //that contains the WAVE-TAB information. The loop below, is to find the index number of the BinaryTableHDU.
        for(int i=0; i<HDUs.length; i++) {
            if (i!=hduIdx) {
                Header hIHeader= HDUs[i].getHeader();
                String xtension= hIHeader.getStringValue("XTENSION");
                String extName= hIHeader.getStringValue("EXTNAME");
                int extVer = hIHeader.containsKey("EXTVER")?hIHeader.getIntValue("EXTVER"):1;
                int extLevel = hIHeader.containsKey("EXTLEVEL")?hIHeader.getIntValue("EXTLEVEL"):1;
                if ("BINTABLE".equals(xtension) && waveHDUName.equals(extName) &&
                    extVer == waveExtVer && extLevel == waveExtLevel) {
                    return i;
                }

            }
        }


        return -1;
    }
}
