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
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
class SpectralCubeEval implements FitsEvaluation.Eval {
    @Override
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        var tableHduList = findWaveTabHDU(HDUs);
        if (tableHduList.size()==0)  return null;


        var relatedDataList= new ArrayList<RelatedData>();
        for(TableHdu tableHdu : tableHduList) {
            Map<String,String> params= new HashMap<>(10);
            params.put("id", "IpacTableFromSource");
            params.put("startIdx", "0");
            params.put("pageSize", "30000");
            params.put("tbl_index", tableHdu.hduIdx+"");
            params.put("source", ServerContext.replaceWithPrefix(f));
            relatedDataList.add(
                    RelatedData.makeWavelengthTabularRelatedData(params, "WAVE-TAB", "Table Wavelength information", tableHdu.hduName, tableHdu.hduIdx));

        }
        return relatedDataList;
    }


    public static List<TableHdu> findWaveTabHDU(BasicHDU<?>[] HDUs) {
        List<TableHdu> tableHduList= new ArrayList<>();
        for(int i=0; (i<HDUs.length); i++) {
            var altProjList= FitsReadUtil.getAtlProjectionIDs(HDUs[i].getHeader());
            var tabHdu= findWaveTabHDU(HDUs,i,"");
            if (tabHdu!=null) tableHduList.add(tabHdu);
            for(String alt : altProjList) {
                tabHdu= findWaveTabHDU(HDUs,i,alt);
                if (tabHdu!=null) tableHduList.add(tabHdu);
            }
        }
        return tableHduList;
    }

    private record TableHdu(int hduIdx, String hduName) {};


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
    private static boolean isValidWCSForTab(BasicHDU<?>[] HDUs){

        List<String> extNames = new ArrayList<>();
        List<Integer> extLevels= new ArrayList<>();
        List<Integer> extVers  = new ArrayList<>();


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
                if (ComparisonUtil.equals(extNames.get(i), extNames.get(j)) &&
                    ComparisonUtil.equals(extLevels.get(i),extLevels.get(j)) &&
                    ComparisonUtil.equals(extVers.get(i),extVers.get(j)) ){
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
    public static TableHdu findWaveTabHDU(BasicHDU<?>[] HDUs, int hduIdx, String alt) {

        if (HDUs.length<2) return null;


        BasicHDU<?> hdu= HDUs[hduIdx];
        Header header= hdu.getHeader();
        String xtensionPrim= header.getStringValue("XTENSION");
        if (("BINTABLE").equals(xtensionPrim)) return null;
        List<Integer> ctypeList= getCtypeWithWave(header,alt);
        if (ctypeList.size()==0) return null;

        //If it is WAVE-TAB, validate it first
        if (!isValidWCSForTab(HDUs)) return null;
        for(int idx : ctypeList) {
            TableHdu tableHdu= findWaveTabHDUForCtypeIdx(HDUs,header,hduIdx,alt,idx);
            if (tableHdu!=null) return tableHdu;
        }
        return null;
    }


    public static TableHdu findWaveTabHDUForCtypeIdx(BasicHDU<?>[] HDUs, Header header, int hduIdx, String alt, int idx) {

        String waveHDUName= header.getStringValue("PS"+idx+"_0"+alt);

        ////When PSi_0["",A-Z] is undefined, use the binary table whose name starts with "WCS-".
        if (waveHDUName==null){
            for(int i=0; i<HDUs.length; i++) {
                if (i!=hduIdx) {
                    Header hIHeader= HDUs[i].getHeader();
                    String xtension= hIHeader.getStringValue("XTENSION");
                    String extName= hIHeader.getStringValue("EXTNAME");
                    if ("BINTABLE".equals(xtension) && extName.startsWith("WCS-")) {
                        return new TableHdu(i,extName);
                    }
                }
            }

        }

        //The hdu= HDUs[hduIdx] is an image HDU, it has a corresponding BinaryTableHDU
        //that contains the WAVE-TAB information. The loop below, is to find the index number of the BinaryTableHDU.
        for(int i=0; i<HDUs.length; i++) {
            if (i!=hduIdx) {
                Header hIHeader= HDUs[i].getHeader();
                String xtension= hIHeader.getStringValue("XTENSION");
                String extName= hIHeader.getStringValue("EXTNAME");
                if ("BINTABLE".equals(xtension) && waveHDUName.equals(extName)) {
                    return new TableHdu(i,extName);
                }

            }
        }
        return null;
    }





    private static List<Integer> getCtypeWithWave(Header h, String alt) {
        var retList= new ArrayList<Integer>();
        for(int i= 1; (i<=4); i++) {
            if ("WAVE-TAB".equals(h.getStringValue("CTYPE"+i+alt))) retList.add(i);
        }
        return retList;



    }
}
