/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
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
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU<?>[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        var tableHduList = findWaveTabHDU(HDUs);
        if (tableHduList.isEmpty()) return null;


        var relatedDataList = new ArrayList<RelatedData>();
        for (TableHdu tableHdu : tableHduList) {
            Map<String, String> params = new HashMap<>(10);
            params.put("id", "IpacTableFromSource");
            params.put("startIdx", "0");
            params.put("pageSize", "30000");
            params.put("tbl_index", tableHdu.hduIdx + "");
            params.put("source", ServerContext.replaceWithPrefix(f));
            relatedDataList.add(
                    RelatedData.makeWavelengthTabularRelatedData(
                            frAry[fitsReadIndex].getHduNumber(),
                            params, "WAVE-TAB", "Table Wavelength information",
                            tableHdu.hduName, tableHdu.hduVersion, tableHdu.hduLevel, tableHdu.hduIdx));
        }
        return relatedDataList;
    }


    private static List<TableHdu> findWaveTabHDU(BasicHDU<?>[] HDUs) {
        List<TableHdu> tableHduList = new ArrayList<>();
        for (int i = 0; (i < HDUs.length); i++) {
            var altProjList = FitsReadUtil.getAtlProjectionIDs(HDUs[i].getHeader());
            var tabHdu = findWaveTabHDU(HDUs, i, "");
            if (tabHdu != null) addTableHdu(tableHduList,tabHdu);
            for (String alt : altProjList) {
                tabHdu = findWaveTabHDU(HDUs, i, alt);
                if (tabHdu != null) addTableHdu(tableHduList,tabHdu);
            }
        }
        return tableHduList;
    }

    private static void addTableHdu(List<TableHdu> tableHduList, TableHdu tabHdu) {
        if (tableHduList.stream().noneMatch( (h) -> h.hduIdx==tabHdu.hduIdx)) {
            tableHduList.add(tabHdu);
        }
    }

    private record TableHdu(int hduIdx, String hduName, int hduVersion, int hduLevel) {}

    private record ExtId(String extName, int extVer, int extLevel) {}

    /**
     * If a FITS file contains multiple XTENSION HDUs (header data
     * units) with the specified EXTNAME, EXTVER, and EXTLEVEL,
     * then the result of the WCS table lookup is undefined.
     * If the specified FITS BINTABLE contains no column, or multiple
     * columns, with the specified TTYPEn, then the result of the
     * WCS table lookup is undefined. The specified FITS BINTABLE
     * must contain only one row.
     *
     * @param HDUs FITS HDUs
     * @param tabExtId Extension ID of the table lookup extension
     * @return matching tab extension; null if it can not be found or there are more than one matching extension
     */
    private static TableHdu findTabExtension(BasicHDU<?>[] HDUs, ExtId tabExtId) {

        TableHdu tabExtHdu = null;
        for (int i = 0; i < HDUs.length; i++) {
            Header header = HDUs[i].getHeader();
            String xtension = header.getStringValue("XTENSION");
            if ("BINTABLE".equals(xtension)) {
                ExtId extId = new ExtId(header.getStringValue("EXTNAME"),
                        header.getIntValue("EXTVER", 1),
                        header.getIntValue("EXTLEVEL", 1));
                if (extId.equals(tabExtId)) {
                    if (tabExtHdu == null) {
                        tabExtHdu = new TableHdu(i, extId.extName, extId.extVer, extId.extLevel);
                    } else {
                        // duplicate extension ids - the result of table lookup is undefined
                        return null;
                    }
                }
            }
        }

        return tabExtHdu;
    }

    /**
     * For each image HDU, look for its corresponding BinaryTableHDU.  This only applies for WAVE-TAB case.
     * The -TAB implementation is more complicated than most other WCS conventions because the coordinate system
     * is not completely defined by keywords in a single FITS header.
     * The necessary WCS parameters that are in general distributed over two FITS HDUs and in the body
     * of the WCS extension table.
     *
     * @param HDUs   the array of all HDUs in the FITS file
     * @param hduIdx image HDU
     * @return info lookup table
     */
    private static TableHdu findWaveTabHDU(BasicHDU<?>[] HDUs, int hduIdx, String alt) {

        if (HDUs.length < 2) return null;

        BasicHDU<?> hdu = HDUs[hduIdx];
        Header header = hdu.getHeader();

        // no support for -TAB usage in tables
        String xtensionPrim = header.getStringValue("XTENSION");
        if (("BINTABLE").equals(xtensionPrim)) return null;

        // check if -TAB coordinate is present in the hdu
        List<Integer> ctypeList = getCtypeWithWave(header, alt);
        if (ctypeList.isEmpty()) return null;

        for (int idx : ctypeList) {
            TableHdu tableHdu = findWaveTabHDUForCtypeIdx(HDUs, header, alt, idx);
            if (tableHdu != null) return tableHdu;
        }
        return null;
    }


    private static TableHdu findWaveTabHDUForCtypeIdx(BasicHDU<?>[] HDUs, Header header, String alt, int idx) {

        // parameter keywords used for the -TAB algorithm to find the extension with lookup table
        // PSi_0a table extension name (EXTNAME)
        // PVi_1a table version number (EXTVER)
        // PVi_2a table level number (EXTLEVEL)

        String tabExtName = header.getStringValue("PS" + idx + "_0" + alt);

        // for images PSi_0a has no default
        if (tabExtName != null) {
            int tabExtVer = header.getIntValue("PV" + idx + "_1" + alt, 1);
            int tabExtLevel = header.getIntValue("PV" + idx + "_2" + alt, 1);
            ExtId tabExtId = new ExtId(tabExtName, tabExtVer, tabExtLevel);
            return findTabExtension(HDUs, tabExtId);
        }

        return null;
    }


    private static List<Integer> getCtypeWithWave(Header h, String alt) {
        var retList = new ArrayList<Integer>();
        for (int i = 1; (i <= 4); i++) {
            if ("WAVE-TAB".equals(h.getStringValue("CTYPE" + i + alt))) retList.add(i);
        }
        return retList;
    }
}
